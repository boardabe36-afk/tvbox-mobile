package com.simple.tvboxmobile.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.simple.tvbox.util.HttpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

object OtaService {

    private const val TAG = "OtaService"
    const val GITHUB_REPO = "boardabe36-afk/tvbox-mobile"
    const val GITHUB_LATEST_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    data class UpdateInfo(
        val versionName: String,
        val versionCode: Int,
        val apkUrl: String,
        val apkSize: Long,
        val changelog: List<String>,
        val releaseDate: String?
    ) {
        fun isNewerThan(currentVersionCode: Int): Boolean = versionCode > currentVersionCode
    }

    suspend fun fetchUpdateInfo(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "fetching latest release from GitHub: $GITHUB_LATEST_URL")
            val text = HttpUtil.fetchText(GITHUB_LATEST_URL)
            Log.i(TAG, "got ${text.length} chars")
            parseGithubRelease(text)
        } catch (t: Throwable) {
            Log.e(TAG, "fetchUpdateInfo FAILED", t)
            throw t
        }
    }

    private fun parseGithubRelease(text: String): UpdateInfo {
        val obj = JSONObject(text)
        val tagName = obj.optString("tag_name", "")
        if (tagName.isBlank()) throw IOException("GitHub latest release has no tag_name")
        val body = obj.optString("body", "")
        val versionName = tagName.removePrefix("v").trim()
        val versionCode = parseVersionCodeFromBody(body, tagName)

        val assetsArr = obj.optJSONArray("assets")
        var apkUrl = ""
        var apkSize = 0L
        if (assetsArr != null) {
            for (i in 0 until assetsArr.length()) {
                val a = assetsArr.optJSONObject(i) ?: continue
                val name = a.optString("name", "")
                if (name.endsWith(".apk")) {
                    apkUrl = a.optString("browser_download_url", "")
                    apkSize = a.optLong("size", 0)
                    break
                }
            }
        }

        val changelog = body.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .take(15)

        return UpdateInfo(
            versionName = versionName,
            versionCode = versionCode,
            apkUrl = apkUrl,
            apkSize = apkSize,
            changelog = changelog,
            releaseDate = obj.optString("published_at", "")
        )
    }

    private fun parseVersionCodeFromBody(body: String, tag: String): Int {
        for (line in body.lines()) {
            val ms = Regex("(?i)versionCode\\s*[:=]\\s*(\\d+)").findAll(line).toList()
            if (ms.isNotEmpty()) {
                return ms.last().groupValues[1].toIntOrNull() ?: 0
            }
        }
        Log.w(TAG, "Could not parse versionCode from release body for $tag")
        return -1
    }

    suspend fun downloadApk(
        ctx: Context,
        info: UpdateInfo,
        onProgress: ((percent: Int, downloaded: Long, total: Long) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        if (info.apkUrl.isBlank()) throw IllegalStateException("apkUrl is empty")

        val targetFile = File(File(ctx.cacheDir, "ota").also { it.mkdirs() },
            "tvbox-mobile-v${info.versionName}.apk")

        val request = okhttp3.Request.Builder()
            .url(info.apkUrl)
            .header("User-Agent", HttpUtil.USER_AGENT)
            .header("Accept", "*/*")
            .build()
        HttpUtil.client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} download failed")
            val body = resp.body ?: throw IOException("empty response")
            val total = body.contentLength().takeIf { it > 0 } ?: info.apkSize
            val tmpFile = File(targetFile.parentFile, targetFile.name + ".tmp")
            FileOutputStream(tmpFile).use { out ->
                val buf = ByteArray(8 * 1024)
                var downloaded = 0L
                var lastReport = 0
                val source = body.byteStream()
                while (true) {
                    val n = source.read(buf)
                    if (n == -1) break
                    out.write(buf, 0, n)
                    downloaded += n
                    if (onProgress != null && total > 0) {
                        val percent = (downloaded * 100 / total).toInt().coerceIn(0, 100)
                        if (percent - lastReport >= 1) {
                            onProgress(percent, downloaded, total)
                            lastReport = percent
                        }
                    }
                }
                out.flush()
            }
            if (targetFile.exists()) targetFile.delete()
            if (!tmpFile.renameTo(targetFile)) {
                tmpFile.copyTo(targetFile, overwrite = true)
                tmpFile.delete()
            }
            targetFile
        }
    }

    fun installApk(ctx: Context, apkFile: File) {
        val authority = ctx.packageName + ".fileprovider"
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(ctx, authority, apkFile)
        } else {
            Uri.fromFile(apkFile)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        Log.i(TAG, "Starting installer: uri=$uri file=$apkFile")
        ctx.startActivity(intent)
    }

    fun canInstallUnknownApks(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return ctx.packageManager.canRequestPackageInstalls()
    }
}
