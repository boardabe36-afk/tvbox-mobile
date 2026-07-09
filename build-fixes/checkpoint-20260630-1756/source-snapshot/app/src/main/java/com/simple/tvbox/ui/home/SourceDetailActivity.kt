package com.simple.tvbox.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.simple.tvbox.R
import com.simple.tvbox.TvBoxApp
import com.simple.tvbox.model.SpiderSite
import com.simple.tvbox.model.VideoCategory
import com.simple.tvbox.source.VideoClientFactory
import com.simple.tvbox.ui.detail.DetailActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 单个源的全部分类页。
 *
 * 进入路径：首页 → 点击"已配置源"行里的源卡片
 * 退出路径：返回首页
 */
class SourceDetailActivity : FragmentActivity() {

    private lateinit var container: LinearLayout
    private var siteKey: String = ""
    private var sourceName: String = ""
    private var sourceUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_source_detail)

        siteKey = intent.getStringExtra(EXTRA_SITE_KEY) ?: ""
        sourceName = intent.getStringExtra(EXTRA_SOURCE_NAME) ?: "源"
        sourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL) ?: ""
        if (siteKey.isBlank()) {
            finish()
            return
        }

        title = sourceName
        container = findViewById(R.id.source_detail_container)

        findViewById<TextView>(R.id.source_detail_subtitle)?.text = sourceUrl

        loadCategories()
    }

    private fun loadCategories() {
        // ?? SpiderSite
        val isHtml = sourceUrl.isNotBlank()
        val api = if (isHtml) "html://$sourceUrl" else sourceUrl
        val site = SpiderSite(
            key = siteKey,
            name = sourceName,
            type = 1,
            api = api
        )
        val client = VideoClientFactory.create(site)
        lifecycleScope.launch {
            try {
                val cats: List<VideoCategory> = withContext(Dispatchers.IO) {
                    if (!client.isSupported()) {
                        throw IllegalStateException("\u8be5\u7ad9\u70b9\u7c7b\u578b\u6682\u4e0d\u652f\u6301")
                    }
                    client.fetchHomeCategories()
                }
                renderCategories(cats)
            } catch (t: Throwable) {
                renderError("\u52a0\u8f7d\u5931\u8d25\uff1a${t.message ?: t.javaClass.simpleName}")
            }
        }
    }

    private fun renderError(message: String) {
        container.removeAllViews()
        val tv = TextView(this).apply {
            text = message
            setTextColor(Color.LTGRAY)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }
        container.addView(tv)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun renderCategories(cats: List<VideoCategory>) {
        container.removeAllViews()
        if (cats.isEmpty()) {
            val tv = TextView(this).apply {
                text = "该源没有可用的分类"
                setTextColor(Color.LTGRAY)
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(40, 40, 40, 40)
            }
            container.addView(tv)
            return
        }
        cats.forEach { cat ->
            val tv = TextView(this).apply {
                text = cat.name
                textSize = 22f
                gravity = Gravity.CENTER
                isFocusable = true
                isFocusableInTouchMode = true
                setBackgroundResource(R.drawable.bg_card)
                setTextColor(Color.WHITE)
                setPadding(40, 32, 40, 32)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
                setOnClickListener { onCategoryClick(cat) }
            }
            container.addView(tv)
        }
    }

    private fun onCategoryClick(cat: VideoCategory) {
        startActivity(
            CategoryActivity.intent(
                this,
                siteKey = siteKey,
                categoryId = cat.id,
                categoryName = cat.name
            )
        )
    }

    companion object {
        private const val EXTRA_SITE_KEY = "site_key"
        private const val EXTRA_SOURCE_NAME = "source_name"
        private const val EXTRA_SOURCE_URL = "source_url"

        fun intent(ctx: Context, siteKey: String, sourceName: String, sourceUrl: String) =
            Intent(ctx, SourceDetailActivity::class.java).apply {
                putExtra(EXTRA_SITE_KEY, siteKey)
                putExtra(EXTRA_SOURCE_NAME, sourceName)
                putExtra(EXTRA_SOURCE_URL, sourceUrl)
            }

        fun intent(ctx: Context, sourceKey: String, sourceName: String) =
            Intent(ctx, SourceDetailActivity::class.java).apply {
                putExtra(EXTRA_SITE_KEY, sourceKey)
                putExtra(EXTRA_SOURCE_NAME, sourceName)
            }
    }
}
