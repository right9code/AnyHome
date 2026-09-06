package com.right9code.anyhome.engine

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

data class Page(val apps: List<ResolveInfo>)

object PagedAppViewManager {

    fun chunkIntoPages(installed: List<ResolveInfo>, rows: Int, cols: Int): List<Page> {
        if (rows <= 0 || cols <= 0) return emptyList()
        val pageSize = rows * cols
        return installed.chunked(pageSize).map { Page(it) }
    }

    fun filterApps(query: String, apps: List<ResolveInfo>, pm: PackageManager): List<ResolveInfo> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return apps
        return apps.filter { app ->
            app.loadLabel(pm).toString().lowercase().contains(q)
        }
    }
}
