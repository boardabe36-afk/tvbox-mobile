package com.simple.tvbox.source

import com.simple.tvbox.model.PlayInfo
import com.simple.tvbox.model.SpiderSite
import com.simple.tvbox.model.VideoCategory
import com.simple.tvbox.model.VideoItem
import com.simple.tvbox.util.HttpUtil
import java.io.IOException

/**
 * 通用 HTML 影视站适配器。
 *
 * 启发式识别多种 maccms 衍生模板（canghai_two / stui / 飞飞 / 海洋 等）。
 *
 * 兼容的站：icaiqi.com、555yy、tiantang、飞速、达达兔、8090 等国内 PHP 模板影视小偷站。
 * 不兼容：纯 SPA 站 / DRM 加密 / 视频号抖音类客户端内置视频。
 *
 * 历史踩坑：
 * - 详情页标题在 canghai 模板里是 <h3 class="title">，不是 <h1>
 * - 分页 URL 在 canghai 模板里是 /sortlist/{catid}/last-{N}.html，不是 /index_{N}.html
 * - 部分视频的 thisUrl = ""（视频源挂了），需要兜底策略
 * - 抓首页/列表页要带 Referer（防盗链）
 */
class GenericHtmlClient(rawRoot: String) : VideoClient {

    override val key: String = "html_" + extractHost(rawRoot)

    private val baseUrl: String = normalizeRoot(rawRoot)
    private val host: String = extractHost(rawRoot)

    /** 模板识别结果；UNKNOWN = 不支持 */
    private var template: Template = Template.UNKNOWN

    /** 探测状态：是否已经触发过首页探测 */
    private var probed = false

    /** 解析后的全站首页 HTML（避免重复请求） */
    private var homeHtml: String = ""

    private fun ensureProbed() {
        if (probed) return
        probed = true
        val html = try {
            HttpUtil.fetchText(baseUrl, referer = baseUrl)
        } catch (t: Throwable) {
            // Do not silently classify a site as unsupported when the device/network is temporarily unavailable.
            throw IOException("\u7ad9\u70b9\u63a2\u6d4b\u5931\u8d25\uff1a${t.message ?: t.javaClass.simpleName}", t)
        }
        homeHtml = html
        template = detectTemplate(html)
    }

    override fun isSupported(): Boolean {
        // html:// clients are intentionally created only for user supplied HTML sites.
        // Network/template errors should be reported by fetch* methods instead of hidden behind
        // ?unsupported site type?.
        return true
    }

    override fun fetchHomeCategories(): List<VideoCategory> {
        ensureProbed()
        if (template == Template.UNKNOWN) {
            throw IOException("\u6682\u672a\u8bc6\u522b\u8be5 HTML \u7ad9\u70b9\u6a21\u677f")
        }
        return parseCategories(homeHtml)
    }

    override fun fetchCategory(categoryId: String, page: Int): List<VideoItem> {
        ensureProbed()
        if (template == Template.UNKNOWN) return emptyList()
        val url = buildCategoryUrl(categoryId, page)
        val html = runCatching { HttpUtil.fetchText(url, referer = baseUrl) }
            .getOrNull() ?: return emptyList()
        return parseVideoList(html, baseUrl)
    }

    override fun search(keyword: String, page: Int): List<VideoItem> {
        ensureProbed()
        if (template == Template.UNKNOWN) return emptyList()
        val encoded = HttpUtil.let { encodeQuery(keyword) }
        // 多种搜索入口（不同模板支持不同）
        val candidates = listOf(
            // maccms 搜索入口
            "$baseUrl/index.php/vod/search.html?wd=$encoded",
            // 模板自带的搜索入口
            "$baseUrl/search/$encoded.html",
            "$baseUrl/search.php?wd=$encoded",
            "$baseUrl/index.php?m=vod-search&wd=$encoded",
        )
        for (url in candidates) {
            val html = runCatching { HttpUtil.fetchText(url, referer = baseUrl) }
                .getOrNull() ?: continue
            val items = parseVideoList(html, baseUrl)
            if (items.isNotEmpty()) return items
        }
        return emptyList()
    }

    override fun fetchEpisodes(videoId: String): List<Pair<String, String>> {
        ensureProbed()
        if (template == Template.UNKNOWN) return emptyList()
        val detailUrl = buildDetailUrl(videoId)
        val html = runCatching { HttpUtil.fetchText(detailUrl, referer = baseUrl) }
            .getOrNull() ?: return emptyList()
        return parseEpisodes(html, baseUrl)
    }

    override fun fetchDetailInfo(videoId: String): JSONObject? {
        ensureProbed()
        if (template == Template.UNKNOWN) return null
        val detailUrl = buildDetailUrl(videoId)
        val html = runCatching { HttpUtil.fetchText(detailUrl, referer = baseUrl) }
            .getOrNull() ?: return null
        val title = extractDetailTitle(html, videoId)
        val desc = extractDetailDesc(html)
        val poster = extractDetailPoster(html)
        return JSONObject().apply {
            put("vod_name", title)
            put("vod_pic", poster)
            put("vod_content", desc)
        }
    }

    override fun resolvePlayUrl(episodeUrl: String): PlayInfo {
        val absolute = absolutize(episodeUrl, baseUrl)
        if (isDirectMediaUrl(absolute)) {
            return PlayInfo(quality = "\u9ed8\u8ba4", url = absolute)
        }
        val html = runCatching { HttpUtil.fetchText(absolute, referer = baseUrl) }
            .getOrElse { throw IOException("\u64ad\u653e\u9875\u8bf7\u6c42\u5931\u8d25\uff1a${it.message ?: it.javaClass.simpleName}", it) }
        // 1. Prefer canghai thisUrl / maccms player_aaaa / inline media URLs
        val direct = extractM3u8FromPlayPage(html)
        if (!direct.isNullOrBlank()) {
            val followed = HttpUtil.followRedirects(direct, referer = baseUrl)
            val finalUrl = resolveMasterPlaylist(followed) ?: followed
            return PlayInfo(quality = "\u9ed8\u8ba4", url = finalUrl)
        }
        // 2. If thisUrl is empty, try artplayer iframe url= parameter
        val iframeUrl = extractUrlFromIframe(html)
        if (!iframeUrl.isNullOrBlank()) {
            val followed = HttpUtil.followRedirects(iframeUrl, referer = baseUrl)
            val finalUrl = resolveMasterPlaylist(followed) ?: followed
            return PlayInfo(quality = "\u9ed8\u8ba4", url = finalUrl)
        }
        throw IOException("\u672a\u627e\u5230\u771f\u5b9e\u64ad\u653e\u5730\u5740\uff08m3u8/mp4\uff09\uff0c\u8be5\u89c6\u9891\u6e90\u53ef\u80fd\u5df2\u5931\u6548")
    }

    private fun isDirectMediaUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") || lower.contains(".mp4")
    }

    // ---- Template detection ----

    private fun detectTemplate(html: String): Template {
        val lower = html.lowercase()
        return when {
            // canghai 模板特征：/template/canghai/ 路径 + /shipin/ + /movie/ + stui-pannel
            lower.contains("canghai") || html.contains("stui-pannel") -> Template.MACCMS_CANGHAI
            // stui 模板特征：/template/stui_ 或 stui-vodlist 类
            lower.contains("stui") || html.contains("stui-vodlist") -> Template.MACCMS_STUI
            // 通用 maccms：/index.php/vod/
            lower.contains("/index.php/vod/") || lower.contains("mac.php") -> Template.MACCMS_DEFAULT
            // 退路：能找到列表链接就当通用 PHP 模板处理
            html.contains(".html") -> Template.GENERIC
            else -> Template.UNKNOWN
        }
    }

    // ---- 列表页解析 ----

    /**
     * 分类解析。
     *
     * 真实结构（canghai 模板）：
     *   <a href="/sortlist/1414.html">电影</a>
     *   <a href="/sortlist/1415.html">电视</a>
     *
     * 主页头部还有"全部分类"的 popup（`/sortlist/{catid}.html` 形式），
     * 但因为 popup 默认隐藏，HTML 里也有，启发式过滤掉太短/纯数字的 name。
     */
    private fun parseCategories(html: String): List<VideoCategory> {
        val cats = LinkedHashMap<String, String>()
        // 1. 导航分类（顶层 /sortlist/N.html）
        Regex("""href\s*=\s*["']([^"']*sortlist/(\d+)\.html)["'][^>]*>([^<]{1,20})</a>""")
            .findAll(html).forEach { m ->
                val id = m.groupValues[2]
                val name = m.groupValues[3].trim()
                if (isValidCategoryName(name) && id !in cats) {
                    cats[id] = name
                }
            }
        // 2. 其他分类入口
        if (cats.isEmpty()) {
            Regex("""href\s*=\s*["']([^"']*(?:list|type|show|vodtype)/(\d+)\.html)["'][^>]*>([^<]{1,20})</a>""")
                .findAll(html).forEach { m ->
                    val id = m.groupValues[2]
                    val name = m.groupValues[3].trim()
                    if (isValidCategoryName(name) && id !in cats) {
                        cats[id] = name
                    }
                }
        }
        return cats.entries.take(20).map { VideoCategory(id = it.key, name = it.value) }
    }

    /**
     * 视频列表解析。
     *
     * 真实结构（canghai 模板）：
     *   <a class="...lazyload" href="https://www.icaiqi.com/shipin/15851436.html"
     *      title="镖人：风起大漠" data-original="https://pic...poster.jpg">
     *     <span class="pic-text text-right">HD国语|英语</span>
     *   </a>
     *   <h4 class="title text-overflow">
     *     <a href="..." title="镖人：风起大漠">镖人：风起大漠</a>
     *   </h4>
     *
     * 策略（顺序无关，兼容 canghai / stui / 模板混淆类名）：
     * 1. 主抓 <a> 块：attrs 里 data-original + title；title 为空时退路到 .pic-text 文本
     * 2. 主抓 <a> 块内文本退路：text-overflow 的 h4 > a 标题
     * 3. 退路：通用 maccms /index.php/vod/detail/id/{id}.html
     */
    private fun parseVideoList(html: String, base: String): List<VideoItem> {
        val items = LinkedHashMap<String, VideoItem>()

        // 策略 1: canghai / 通用 a 块解析
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
                items.putIfAbsent(id, VideoItem(
                    id = id,
                    title = title,
                    subTitle = extractSubTitleFromInner(inner),
                    poster = posterMatch?.groupValues?.get(1)?.let { absolutize(it, base) },
                    sourceKey = key
                ))
            }
        }

        // 策略 2: maccms_default 模板 /index.php/vod/detail/id/{id}.html（如果上面没抓到）
        if (items.isEmpty()) {
            val defaultPat = Regex(
                """<a\b([^>]*?href\s*=\s*["']([^"']*\/vod\/detail\/id\/(\d+)\.html)["'][^>]*?)>([\s\S]*?)</a>""",
                RegexOption.DOT_MATCHES_ALL
            )
            defaultPat.findAll(html).forEach { m ->
                val attrs = m.groupValues[1]
                val id = m.groupValues[3]
                val inner = m.groupValues[4]
                val posterMatch = Regex("""(?:data-original|data-src)\s*=\s*["']([^"']+)["']""").find(attrs)
                val titleMatch = Regex("""title\s*=\s*["']([^"']+)["']""").find(attrs)
                val title = titleMatch?.groupValues?.get(1)?.trim()
                    ?: extractTitleFromInner(inner)
                if (title.isNotBlank() && id.isNotBlank()) {
                    items.putIfAbsent(id, VideoItem(
                        id = id,
                        title = title,
                        subTitle = null,
                        poster = posterMatch?.groupValues?.get(1)?.let { absolutize(it, base) },
                        sourceKey = key
                    ))
                }
            }
        }
        return items.values.toList()
    }

    /**
     * 剧集列表解析。
     *
     * 真实结构（canghai 模板）：
     *   <ul class="...stui-content__playlist clearfix">
     *     <li><a href="/movie/15765062/91933381.html" title="第01集">第01集</a></li>
     *     ...
     *   </ul>
     * 或者单视频：
     *   /movie/15851436/0.html
     */
    private fun parseEpisodes(html: String, base: String): List<Pair<String, String>> {
        val eps = mutableListOf<Pair<String, String>>()
        val container = run {
            val patterns = listOf(
                """stui-content__playlist[\s\S]*?</ul>""",
                """playlist[\s\S]{0,5000}?</ul>"""
            )
            patterns.asSequence()
                .mapNotNull { Regex(it, RegexOption.DOT_MATCHES_ALL).find(html)?.value }
                .firstOrNull()
        }
        val scope = container ?: html

        // 策略 1: canghai 模板 — /movie/{vid}/{eid}.html
        Regex("""href\s*=\s*["']([^"']*movie/(\d+)/(\d+)\.html)["'][^>]*>([^<]{1,50})</a>""")
            .findAll(scope).forEach { m ->
                val url = absolutize(m.groupValues[1], base)
                val name = m.groupValues[4].trim().ifBlank { "第${eps.size + 1}集" }
                eps.add(name to url)
            }
        // 策略 2: /play/{vid}-{eid}.html
        if (eps.isEmpty()) {
            Regex("""href\s*=\s*["']([^"']*(?:play|videoplay)/(\d+)(?:[-/](\d+))?\.html)["'][^>]*>([^<]{1,50})</a>""")
                .findAll(scope).forEach { m ->
                    val url = absolutize(m.groupValues[1], base)
                    val name = m.groupValues[4].trim().ifBlank { "第${eps.size + 1}集" }
                    eps.add(name to url)
                }
        }
        // 策略 3: 通用 maccms /index.php/vod/play/id/{id}/n/{n}.html
        if (eps.isEmpty()) {
            Regex("""href\s*=\s*["']([^"']*\/vod\/play\/id/\d+(?:\/n\/\d+)?\.html)["'][^>]*>([^<]{1,50})</a>""")
                .findAll(scope).forEach { m ->
                    val url = absolutize(m.groupValues[1], base)
                    val name = m.groupValues[2].trim().ifBlank { "第${eps.size + 1}集" }
                    eps.add(name to url)
                }
        }
        // 策略 4: anchor 文本含"第X集"
        if (eps.isEmpty()) {
            Regex("""href\s*=\s*["']([^"']+\.html)["'][^>]*>([^<]{0,30}第\d+集[^<]{0,30})</a>""")
                .findAll(scope).forEach { m ->
                    val url = absolutize(m.groupValues[1], base)
                    val name = m.groupValues[2].trim()
                    eps.add(name to url)
                }
        }
        return eps
    }

    // ---- 播放页解析（拿 m3u8） ----

    /**
     * 从播放页 HTML 抓 m3u8/mp4 直链。
     *
     * 真实结构（canghai 模板）：
     *   <script> thisUrl = "https://vip.dytt-cinema.com/.../index.m3u8"; ... </script>
     *
     * 多策略（命中即返回）：
     *  1. `thisUrl = "https://...m3u8"` 直接赋值
     *  2. `player_aaaa = { url: "..." }`
     *  3. `<iframe src="...?url=...m3u8...">`
     *  4. 页面里直接出现的 .m3u8 / .mp4 链接
     *  5. MacPlayer JSON 配置
     */
    private fun extractM3u8FromPlayPage(html: String): String? {
        // 策略 1: thisUrl 赋值（canghai 模板的招牌）
        Regex("""thisUrl\s*=\s*["']([^"']+)["']""").find(html)?.let {
            val v = unescapeJsString(it.groupValues[1])
            if (v.isNotBlank() && v != "undefined") return v
        }
        // 策略 2: player_aaaa 变量（maccms 默认模板）
        Regex("""player_aaaa\s*=\s*\{[^}]*?["']?url["']?\s*:\s*["']([^"']+)["']""").find(html)?.let {
            val v = it.groupValues[1]
            if (v.isNotBlank() && v != "undefined") return v
        }
        // 策略 3: 任意 m3u8 链接
        Regex("""["'](https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)["']""").find(html)?.let {
            return it.groupValues[1]
        }
        Regex("""["'](https?://[^\s"'<>]+\.mp4[^\s"'<>]*)["']""").find(html)?.let {
            return it.groupValues[1]
        }
        // 策略 4: MacPlayer JSON 配置
        Regex("""MacPlayer\([^)]*\)\s*\.setup\s*\(\s*\{[^}]*url\s*:\s*["']([^"']+)["']""").find(html)?.let {
            return it.groupValues[1]
        }
        return null
    }

    /**
     * thisUrl 为空时，从 artplayer iframe 的 ?url= 参数里拿 m3u8。
     * 真实结构：thisUrl = "" + iframe src="/artplayer/index.php?title=...&url=..."
     */
    private fun extractUrlFromIframe(html: String): String? {
        // 找 src="/artplayer/..." 或包含 url= 的 iframe
        Regex("""<iframe[^>]+src\s*=\s*["']([^"']*(?:url|v|url)=([^&"']+))["']""").find(html)?.let {
            val raw = java.net.URLDecoder.decode(it.groupValues[2], "UTF-8")
            if (raw.startsWith("http") && (raw.contains(".m3u8") || raw.contains(".mp4"))) {
                return raw
            }
        }
        return null
    }

    // ---- 详情页元信息 ----

    /**
     * 标题提取（按优先级）：
     * 1. <h1>...</h1>
     * 2. <h3 class="title">...</h3>（canghai 模板真实结构）
     * 3. <title> 标签（去掉站点后缀）
     * 4. fallback: videoId
     */
    private fun extractDetailTitle(html: String, fallback: String): String {
        Regex("""<h1[^>]*>([^<]{1,100})</h1>""").find(html)?.let {
            val t = it.groupValues[1].trim()
            if (t.isNotBlank()) return t
        }
        Regex("""<h3[^>]*class\s*=\s*["'][^"']*\btitle\b[^"']*["'][^>]*>([^<]{1,100})</h3>""").find(html)?.let {
            val t = it.groupValues[1].trim()
            if (t.isNotBlank()) return t
        }
        Regex("""<title>([^<]{1,100})</title>""").find(html)?.let {
            return it.groupValues[1]
                .substringBefore('-')
                .substringBefore('_')
                .substringBefore('｜')
                .substringBefore('|')
                .trim()
                .ifBlank { fallback }
        }
        return fallback
    }

    /**
     * 简介提取（按优先级）：
     * 1. <div class="detail-content ...">...</div>
     * 2. <div class="stui-content__desc ...">...</div>（canghai）
     * 3. <meta name="description" content="...">
     */
    private fun extractDetailDesc(html: String): String {
        Regex("""<div[^>]+class\s*=\s*["'][^"']*detail-content[^"']*["'][^>]*>([\s\S]{20,2000}?)</div>""")
            .find(html)?.let { return stripHtml(it.groupValues[1]).trim() }
        Regex("""<div[^>]+class\s*=\s*["'][^"']*stui-content__desc[^"']*["'][^>]*>([\s\S]{20,2000}?)</div>""")
            .find(html)?.let { return stripHtml(it.groupValues[1]).trim() }
        Regex("""<meta\s+name\s*=\s*["']description["']\s+content\s*=\s*["']([^"']{10,500})["']""")
            .find(html)?.let { return it.groupValues[1].trim() }
        return ""
    }

    /**
     * 海报提取（按优先级）：
     * 1. <a class="stui-vodlist__thumb" ...><img data-original="..."></a>（canghai）
     * 2. <div class="stui-vodlist__thumb"><a><img data-original="..."></a></div>
     * 3. <div class="detail-pic"><img src="..."></div>
     */
    private fun extractDetailPoster(html: String): String? {
        // canghai 模板：stui-vodlist__thumb 里的 data-original（detail 页复用列表的样式）
        Regex("""class\s*=\s*["']stui-vodlist__thumb["'][\s\S]{0,2000}?data-original\s*=\s*["']([^"']+)["']""")
            .find(html)?.let { return it.groupValues[1] }
        Regex("""class\s*=\s*["']detail-pic["'][\s\S]{0,500}?src\s*=\s*["']([^"']+)["']""")
            .find(html)?.let { return it.groupValues[1] }
        // 兜底：详情页主图 src
        Regex("""<img[^>]+class\s*=\s*["']pic["'][^>]+src\s*=\s*["']([^"']+)["']""")
            .find(html)?.let { return it.groupValues[1] }
        return null
    }

    // ---- URL 拼接工具 ----

    /**
     * 分类页 URL 拼接。
     *
     * 真实格式（canghai 模板）：
     *   第 1 页: https://www.icaiqi.com/sortlist/1414.html
     *   第 N 页: https://www.icaiqi.com/sortlist/1414/last-{N}.html
     */
    private fun buildCategoryUrl(categoryId: String, page: Int): String {
        return if (page <= 1) {
            "$baseUrl/sortlist/$categoryId.html"
        } else {
            "$baseUrl/sortlist/$categoryId/last-$page.html"
        }
    }

    /**
     * 详情页 URL 拼接。
     *
     * 真实格式（canghai 模板）：
     *   https://www.icaiqi.com/shipin/{id}.html
     */
    private fun buildDetailUrl(videoId: String): String {
        return "$baseUrl/shipin/$videoId.html"
    }

    /**
     * master playlist 解析：如果 m3u8 是 master playlist（嵌套），下载并提取第一个子 m3u8。
     */
    private fun resolveMasterPlaylist(m3u8Url: String): String? {
        return runCatching {
            val content = HttpUtil.fetchText(m3u8Url, referer = baseUrl)
            if (!content.contains("#EXT-X-STREAM-INF")) return@runCatching null
            val sub = content.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                ?: return@runCatching null
            resolveUrlAgainst(m3u8Url, sub)
        }.getOrNull()
    }

    // ---- 工具 ----

    private fun normalizeRoot(raw: String): String {
        var url = raw.trim().trimEnd('/')
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url
    }

    private fun extractHost(raw: String): String {
        return runCatching {
            java.net.URL(if (raw.startsWith("http")) raw else "https://$raw").host
        }.getOrDefault(raw)
            .replace(".", "_")
    }

    private fun absolutize(href: String, base: String): String {
        if (href.isBlank()) return base
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        if (href.startsWith("//")) return "https:$href"
        if (href.startsWith("/")) return base + href
        return base + "/" + href
    }

    private fun resolveUrlAgainst(base: String, href: String): String {
        if (href.isBlank()) return base
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        if (href.startsWith("//")) return "https:$href"
        return runCatching { java.net.URL(java.net.URL(base), href).toString() }
            .getOrElse { absolutize(href, baseUrl) }
    }

    private fun isValidCategoryName(name: String): Boolean {
        if (name.isBlank() || name.length < 2 || name.length > 20) return false
        // 过滤：纯数字、HTML 标签残留、"更多"等导航文字
        if (name.all { it.isDigit() }) return false
        if (name.contains('<')) return false
        // 常见导航文字过滤
        val nav = setOf("更多", "首页", "上一页", "下一页", "尾页", "more", "首页")
        if (name in nav) return false
        return true
    }

    private fun extractTitleFromInner(inner: String): String {
        // 退路 1: <h4 class="title text-overflow"><a title="...">...</a></h4>
        Regex("""<a[^>]+title\s*=\s*["']([^"']{1,100})["'][^>]*>([^<]{1,100})</a>""").find(inner)
            ?.let { return (it.groupValues[1].trim().ifBlank { it.groupValues[2].trim() }) }
        // 退路 2: 第一个 <span class="pic-text"> 的文本
        Regex("""pic-text[^>]*>([^<]{1,100})<""").find(inner)
            ?.let { return it.groupValues[1].trim() }
        // 退路 3: 第一个非空文本
        Regex(""">([^<]{1,100})<""").find(inner)
            ?.let { return it.groupValues[1].trim() }
        return ""
    }

    private fun extractSubTitleFromInner(inner: String): String? {
        // 抓"已完结" / "更新至X集" / "HD" / "高清" 等 pic-text 副标题
        val m = Regex("""pic-text[^>]*>([^<]{1,30})<""").find(inner) ?: return null
        val v = m.groupValues[1].trim()
        return v.takeIf { it.isNotBlank() }
    }

    private fun encodeQuery(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun unescapeJsString(s: String): String =
        s.replace("\\\"", "\"").replace("\\'", "'").replace("\\\\", "\\")

    private fun stripHtml(s: String): String =
        s.replace(Regex("""<[^>]+>"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    /** 模板识别结果 */
    private enum class Template { MACCMS_CANGHAI, MACCMS_STUI, MACCMS_DEFAULT, GENERIC, UNKNOWN }
}

// Import 放到文件底部避免与 JSONObject 冲突
private typealias JSONObject = org.json.JSONObject
