package com.simple.tvbox

import android.app.Application
import com.simple.tvbox.data.SourceRepository
import com.simple.tvboxmobile.data.SourceAccess
import com.simple.tvboxmobile.data.WatchHistoryStore

class TvBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        SourceAccess.init(applicationContext)
        WatchHistoryStore.init(applicationContext)
    }
    companion object {
        @Volatile private var instance: TvBoxApp? = null
        fun get(): TvBoxApp = instance ?: error("TvBoxApp.onCreate() 还没执行")
    }
}
