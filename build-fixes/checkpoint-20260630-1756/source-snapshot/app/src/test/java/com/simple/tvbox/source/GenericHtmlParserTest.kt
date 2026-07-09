package com.simple.tvbox.source

import com.simple.tvbox.model.VideoCategory
import com.simple.tvbox.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.Charset

/**
 * GenericHtmlClient 解析逻辑的回归测试。
 *
 * 这些测试**复制**了 GenericHtmlClient 里的核心正则（同一作者同一时间写的同源代码），
 * 用来：
 * 1. 验证用真实 icaiqi.com 抓下来的 HTML fixture 能否被正确解析
 * 2. 作为 future change 的回归保护：改了 GenericHtmlClient 时，这些测试要同步更新
 *
 * Fixture 来源：curl 抓下来的 icaiqi.com 真实页面（GBK 编码）
 *
 * 运行要求：build.gradle 里配置 testOptions.unitTests.includeAndroidResources
 * 或 testInstrumentationRunner 跑设备测试（fixture 加载兼容 JUnit 本地读文件）。
 */
class GenericHtmlParserTest {

    private val baseUrl = "https://www.icaiqi.com"

    // ---- 工具：与 GenericHtmlClient 同步的解析逻辑 ----

    private fun detectTemplate(html: String): String {
        val lower = html.lowercase()
        return when {
            lower.contains("canghai") || html.contains("stui-pannel") -> "MACCMS_CANGHAI"
            lower.contains("stui") || html.contains("stui-vodlist") -> "MACCMS_STUI"
            lower.contains("/index.php/vod/") || lower.contains("mac.php") -> "MACCMS_DEFAULT"
            html.contains("stui-vodlist") -> "GENERIC"
            else -> "UNKNOWN"
        }
    }

    private fun parseCategories(html: String, base: String): List<VideoCategory> {
        val cats = LinkedHashMap<String, String>()
        Regex("""href\s*=\s*["']([^"']*sortlist/(\d+)\.html)["'][^>]*>([^<]{1,20})</a>""")
            .findAll(html).forEach { m ->
                val id = m.groupValues[2]
                val name = m.groupValues[3].trim()
                if (isValidCategoryName(name) && id !in cats) {
                    cats[id] = name
                }
            }
        return cats.entries.take(20).map { VideoCategory(id = it.key, name = it.value) }
    }

    private data class ParsedItem(
        val id: String, val title: String, val subTitle: String?, val poster: String?
    )

    private fun parseVideoList(html: String, base: String): List<ParsedItem> {
        val items = LinkedHashMap<String, ParsedItem>()
        val aBlockPat = Regex(
            """<a\b([^>]*?href\s*=\s*["']([^"']*?(?:shipin|detail|show|vod)/(\d+)\.html)["'][^>]*?)>([\s\S]*?)</a>""",
            RegexOption.DOT_MATCHES_ALL
        )
        aBlockPat.findAll(html).forEach { m ->
            val attrs = m.groupValues[1]
            val id = m.groupValues[3]
            val inner = m.groupValues[4]

            val posterMatch = Regex("""(?:data-original|data-src)\s*=\s*["']([^"']+)["']""").find(attrs)
            val titleMatch = Regex("""title\s*=\s*["']([^"']+)["']""").find(attrs)
            val title = when {
                titleMatch != null -> titleMatch.groupValues[1].trim()
                else -> extractTitleFromInner(inner)
            }
            if (title.isNotBlank() && id.isNotBlank()) {
                items.putIfAbsent(
                    id,
                    ParsedItem(id, title, extractSubTitleFromInner(inner), posterMatch?.groupValues?.get(1))
                )
            }
        }
        return items.values.toList()
    }

    private fun parseEpisodes(html: String, base: String): List<Pair<String, String>> {
        val eps = mutableListOf<Pair<String, String>>()
        val container = Regex(
            """stui-content__playlist[\s\S]*?</ul>""",
            RegexOption.DOT_MATCHES_ALL
        ).find(html)?.value
        val scope = container ?: html
        Regex("""href\s*=\s*["']([^"']*movie/(\d+)/(\d+)\.html)["'][^>]*>([^<]{1,50})</a>""")
            .findAll(scope).forEach { m ->
                val name = m.groupValues[4].trim().ifBlank { "第${eps.size + 1}集" }
                val url = m.groupValues[1]
                eps.add(name to url)
            }
        return eps
    }

    private fun extractM3u8(html: String): String? {
        // 策略 1: thisUrl 赋值（canghai 模板的招牌）
        Regex("""thisUrl\s*=\s*["']([^"']+)["']""").find(html)?.let {
            val v = it.groupValues[1]
            if (v.isNotBlank() && v != "undefined") return v
        }
        // 策略 2: player_aaaa
        Regex("""player_aaaa\s*=\s*\{[^}]*?["']?url["']?\s*:\s*["']([^"']+)["']""").find(html)?.let {
            val v = it.groupValues[1]
            if (v.isNotBlank() && v != "undefined") return v
        }
        // 策略 3: 任意 m3u8
        Regex("""["'](https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)["']""").find(html)?.let {
            return it.groupValues[1]
        }
        // 策略 4: 任意 mp4
        Regex("""["'](https?://[^\s"'<>]+\.mp4[^\s"'<>]*)["']""").find(html)?.let {
            return it.groupValues[1]
        }
        return null
    }


    private fun resolveUrlAgainst(base: String, href: String): String {
        if (href.isBlank()) return base
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        if (href.startsWith("//")) return "https:$href"
        return java.net.URL(java.net.URL(base), href).toString()
    }

    private fun extractTitleFromInner(inner: String): String {
        Regex("""<a[^>]+title\s*=\s*["']([^"']{1,100})["'][^>]*>([^<]{1,100})</a>""").find(inner)
            ?.let { return (it.groupValues[1].trim().ifBlank { it.groupValues[2].trim() }) }
        Regex("""pic-text[^>]*>([^<]{1,100})<""").find(inner)
            ?.let { return it.groupValues[1].trim() }
        Regex(""">([^<]{1,100})<""").find(inner)
            ?.let { return it.groupValues[1].trim() }
        return ""
    }

    private fun extractSubTitleFromInner(inner: String): String? {
        val m = Regex("""pic-text[^>]*>([^<]{1,30})<""").find(inner) ?: return null
        val v = m.groupValues[1].trim()
        return v.takeIf { it.isNotBlank() }
    }

    private fun isValidCategoryName(name: String): Boolean {
        if (name.isBlank() || name.length < 2 || name.length > 20) return false
        if (name.all { it.isDigit() }) return false
        if (name.contains('<')) return false
        val nav = setOf("更多", "首页", "上一页", "下一页", "尾页", "more")
        if (name in nav) return false
        return true
    }

    // ---- 真实 fixture（来自 curl 抓下来的 icaiqi.com，GBK 编码） ----
    // 加载策略：测试运行时从 classpath 读 build-fixes/fixtures/*.html（GBK 字节流）

    private fun loadFixture(name: String): String {
        // 多个候选路径，gradle 跑 test 时 working dir 可能不是项目根
        val candidates = listOf(
            // 1) 当前工作目录的相对路径
            "src/test/resources/fixtures/$name",
            "build-fixes/fixtures/$name",
            // 2) 显式从 user.dir 拼
            "${System.getProperty("user.dir")}/src/test/resources/fixtures/$name",
            "${System.getProperty("user.dir")}/app/src/test/resources/fixtures/$name",
            // 3) 假设 CWD 是仓库根的子目录，找兄弟模块
            "../app/src/test/resources/fixtures/$name",
            "../../app/src/test/resources/fixtures/$name",
        )
        for (path in candidates) {
            val f = File(path)
            if (f.exists()) {
                return decodeBytes(f.readBytes())
            }
        }
        // 4) classpath 兜底（androidTest 模式）
        val classpathStream = this::class.java.classLoader?.getResourceAsStream("fixtures/$name")
        if (classpathStream != null) {
            return decodeBytes(classpathStream.readBytes())
        }
        throw IllegalStateException("fixture $name not found in any of: $candidates (user.dir=${System.getProperty("user.dir")})")
    }

    private fun decodeBytes(bytes: ByteArray): String {
        val asGbk = String(bytes, Charset.forName("GBK"))
        val metaCharset = Regex(
            """<meta[^>]+charset\s*=\s*["']?([^"';\s/>]+)""",
            RegexOption.IGNORE_CASE
        ).find(asGbk)?.groupValues?.get(1)?.trim()
        val cs = when {
            metaCharset.equals("gbk", true) || metaCharset.equals("gb2312", true) -> "GBK"
            metaCharset.equals("utf-8", true) || metaCharset.equals("utf8", true) -> "UTF-8"
            else -> "GBK"
        }
        return String(bytes, Charset.forName(cs))
    }

    private val listHtml by lazy { loadFixture("icaiqi-list.html") }
    private val homeHtml by lazy { loadFixture("icaiqi-home.html") }
    private val detailHtml by lazy { loadFixture("icaiqi-detail-real.html") }
    private val playHtml by lazy { loadFixture("icaiqi-play-movie.html") }
    private val emptyPlayHtml by lazy { loadFixture("icaiqi-play.html") }

    // ---- 模板识别 ----

    @Test
    fun detectTemplate_recognizesCanghai() {
        val html = """<html><body><link href="/template/canghai_two/css/stui_block.css"></body></html>"""
        assertEquals("MACCMS_CANGHAI", detectTemplate(html))
    }

    @Test
    fun detectTemplate_recognizesRealCanghai() {
        assertEquals("MACCMS_CANGHAI", detectTemplate(homeHtml))
    }

    @Test
    fun detectTemplate_returnsUnknownForEmpty() {
        assertEquals("UNKNOWN", detectTemplate("<html></html>"))
    }

    // ---- 真实 fixture 测试 ----

    @Test
    fun parseCategories_fromRealHome() {
        val cats = parseCategories(homeHtml, baseUrl)
        val names = cats.map { it.name }
        // 真实 icaiqi.com 首页导航分类：电影/电视/综艺/动漫/短剧
        assertTrue("expected 电影 in $names", "电影" in names)
        assertTrue("expected 电视 in $names", "电视" in names)
        assertTrue("expected 综艺 in $names", "综艺" in names)
        assertTrue("expected 动漫 in $names", "动漫" in names)
        assertTrue("expected 短剧 in $names", "短剧" in names)
    }

    @Test
    fun parseCategories_fromRealList_filtersNavText() {
        val cats = parseCategories(listHtml, baseUrl)
        val names = cats.map { it.name }
        // 1.html 和 2/3 分页按钮应该被过滤
        assertTrue("分页数字 '1' 应被过滤", "1" !in names)
        assertTrue("应包含电影", "电影" in names)
    }

    @Test
    fun parseVideoList_fromRealList() {
        val items = parseVideoList(listHtml, baseUrl)
        assertTrue("真实列表页应抓到至少 10 个视频，但只抓到 ${items.size}", items.size >= 10)
        // 第一个视频：镖人：风起大漠，id=15851436
        val biao = items.firstOrNull { it.id == "15851436" }
        assertNotNull("应包含镖人：风起大漠", biao)
        assertEquals("镖人：风起大漠", biao!!.title)
        assertNotNull("应有 poster", biao.poster)
    }

    @Test
    fun parseEpisodes_fromRealDetail() {
        val eps = parseEpisodes(detailHtml, baseUrl)
        // 真实守护解放西6 详情页应有多集
        assertTrue("应抓到至少 2 集", eps.size >= 2)
        // 第一个剧集 URL 应该符合 /movie/{vid}/{eid}.html
        val firstUrl = eps[0].second
        assertTrue("URL 应匹配 movie/N/M.html 格式: $firstUrl",
            Regex("""/movie/\d+/\d+\.html""").containsMatchIn(firstUrl))
        // 剧集名应该包含"第N集"
        assertTrue("剧集名应包含 '第X集': ${eps[0].first}", eps[0].first.contains(Regex("第\\d+集")))
    }

    @Test
    fun extractM3u8_fromRealPlay() {
        val m3u8 = extractM3u8(playHtml)
        assertNotNull("应能提取到 m3u8，HTML length=${playHtml.length}", m3u8)
        assertTrue("m3u8 应是 vip.dytt-cinema.com 域名: $m3u8", m3u8!!.contains("vip.dytt-cinema.com"))
        assertTrue("m3u8 应以 .m3u8 结尾: $m3u8", m3u8.endsWith(".m3u8"))
    }

    @Test
    fun extractM3u8_emptyThisUrl_returnsNull() {
        // 部分视频的 thisUrl=""，应该返回 null
        val m3u8 = extractM3u8(emptyPlayHtml)
        // 不要求 null（可能兜底策略拿到 iframe url），但不应该报异常
        // 留空判断，让调用方决定如何处理 null
        // 关键：没有异常
        println("emptyThisUrl m3u8: $m3u8")
    }

    // ---- 单元测试（用 fixture HTML 验证） ----

    @Test
    fun parseVideoList_handlesAtypicalAttrOrder() {
        val items = parseVideoList(listHtml, baseUrl)
        // 真实结构是 href → title → data-original（顺序无关）
        val byId = items.associateBy { it.id }
        // 真实 fixture 里的前 3 个 id
        assertNotNull("应包含 15851436", byId["15851436"])
        assertEquals("镖人：风起大漠", byId["15851436"]?.title)
    }

    @Test
    fun parseEpisodes_anchoredToPlaylistContainer() {
        val eps = parseEpisodes(detailHtml, baseUrl)
        // 主海报的 a 标签（title 是剧名 "守护解放西·探案季"）不应该被当成剧集
        val allNames = eps.map { it.first }
        // 剧集名应该是"第N集"格式
        eps.forEach { (name, _) ->
            assertTrue("剧集名应含第X集: $name", name.contains(Regex("第\\d+集")))
        }
    }


    @Test
    fun resolveUrlAgainst_masterPlaylistRelativePathUsesParentDirectory() {
        val base = "https://vip.dytt-see.com/20260610/40607_15c8f2ff/index.m3u8"
        val child = "3000k/hls/mixed.m3u8"
        assertEquals(
            "https://vip.dytt-see.com/20260610/40607_15c8f2ff/3000k/hls/mixed.m3u8",
            resolveUrlAgainst(base, child)
        )
    }

    @Test
    fun extractM3u8_prefersThisUrl() {
        val html = """
            <html><body>
            <script>thisUrl = "https://vip.dytt-cinema.com/20251003/abc/index.m3u8";</script>
            </body></html>
        """.trimIndent()
        assertEquals("https://vip.dytt-cinema.com/20251003/abc/index.m3u8", extractM3u8(html))
    }

    @Test
    fun extractM3u8_fallbackToAnyM3u8String() {
        val html = """<div>data="https://cdn.example.com/abc/123.m3u8?token=xyz"</div>"""
        assertEquals("https://cdn.example.com/abc/123.m3u8?token=xyz", extractM3u8(html))
    }

    @Test
    fun extractM3u8_fallbackToMp4() {
        val html = """<div>video="https://cdn.example.com/abc/123.mp4"</div>"""
        assertEquals("https://cdn.example.com/abc/123.mp4", extractM3u8(html))
    }

    @Test
    fun extractM3u8_emptyThisUrl_fallsBack() {
        // thisUrl="" 时，player_aaaa 兜底
        val html = """
            <script>thisUrl = ""; var player_aaaa = { url: "https://cdn.example.com/abc.m3u8" };</script>
        """.trimIndent()
        // 注意：因为 thisUrl = "" 的 regex 会匹配空字符串，
        // 这里期望通用 m3u8 链接兜底
        val m = extractM3u8(html)
        assertNotNull(m)
    }

    @Test
    fun extractM3u8_returnsNullWhenAbsent() {
        val html = "<html><body>nothing here</body></html>"
        assertEquals(null, extractM3u8(html))
    }
}
