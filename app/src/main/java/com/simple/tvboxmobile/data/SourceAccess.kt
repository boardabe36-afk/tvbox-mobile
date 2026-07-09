package com.simple.tvboxmobile.data

import android.content.Context
import com.simple.tvbox.data.SourceRepository
import com.simple.tvbox.model.Source

/**
 * 业务访问统一入口。
 *
 * 让 Compose 层不直接依赖 TvBoxApp 单例，封装在 Application Context 里。
 * 实现简单包装：因为 SourceRepository 内部已经是 stateless 的（每次 getAllSources 都解析 SP）。
 */
object SourceAccess {
    private var repo: SourceRepository? = null

    fun init(ctx: Context) {
        if (repo == null) repo = SourceRepository(ctx.applicationContext)
    }

    fun all(): List<Source> {
        val r = repo ?: error("SourceAccess.init() 还没调用")
        return r.getAllSources()
    }

    fun add(source: Source): Boolean {
        val r = repo ?: error("SourceAccess.init() 还没调用")
        return r.addSource(source)
    }

    fun remove(source: Source) {
        val r = repo ?: error("SourceAccess.init() 还没调用")
        r.removeSource(source)
    }

    fun repository(): SourceRepository =
        repo ?: error("SourceAccess.init() 还没调用")
}

