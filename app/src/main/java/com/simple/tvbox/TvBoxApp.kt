package com.simple.tvbox

import android.app.Application
import com.simple.tvbox.data.SourceRepository

/**
 * 应用入口（精简版：只保留源仓库）。
 * 业务层仍用 com.simple.tvbox.* 包，方便后期和 TV 版共享代码。
 */
class TvBoxApp : Application() {

    lateinit var sourceRepository: SourceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        sourceRepository = SourceRepository(applicationContext)
        // 同步初始化 Composable 层用的统一访问入口
        com.simple.tvboxmobile.data.SourceAccess.init(applicationContext)
    }

    companion object {
        @Volatile
        private var instance: TvBoxApp? = null

        fun get(): TvBoxApp =
            instance ?: error("TvBoxApp.onCreate() 还没执行")
    }
}
