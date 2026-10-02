package com.nightrec.app.recognition

import com.nightrec.app.BuildConfig

/**
 * Debug-only token provider（T052）。
 * token 只来自 BuildConfig（debug 注入自 local.properties，release 为空），
 * 绝不写入日志/仓库/文档。
 */
object DebugTokenProvider : TokenProvider {
    override fun token(): String = BuildConfig.AUDD_TOKEN
}