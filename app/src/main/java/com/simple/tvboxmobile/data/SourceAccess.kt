package com.simple.tvboxmobile.data

import android.content.Context
import com.simple.tvbox.data.SourceRepository
import com.simple.tvbox.model.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SourceAccess {
    private var repo: SourceRepository? = null

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    fun init(ctx: Context) {
        if (repo == null) repo = SourceRepository(ctx.applicationContext)
    }

    fun all(): List<Source> {
        val r = repo ?: error("SourceAccess.init() 还没调用")
        return r.getAllSources()
    }

    fun add(source: Source): Boolean {
        val r = repo ?: error("SourceAccess.init() 还没调用")
        val ok = r.addSource(source)
        if (ok) _version.value++
        return ok
    }

    fun remove(source: Source) {
        val r = repo ?: error("SourceAccess.init() 还没调用")
        r.removeSource(source)
        _version.value++
    }

    fun repository(): SourceRepository =
        repo ?: error("SourceAccess.init() 还没调用")
}
