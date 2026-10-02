package com.nightrec.app.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 采集所需运行时权限（T030）。
 * RECORD_AUDIO 是硬性条件；POST_NOTIFICATIONS 在 Android 13+ 仅影响常驻通知可见性，
 * 缺失时不阻断采集（FGS 仍合法启动）。
 */
object PermissionCoordinator {
    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }

    fun missing(context: Context): List<String> =
        required().filter { !granted(context, it) }

    /** 采集的硬性条件；只有它为 true 才能启动 microphone FGS。 */
    fun canCapture(context: Context): Boolean =
        granted(context, Manifest.permission.RECORD_AUDIO)

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}