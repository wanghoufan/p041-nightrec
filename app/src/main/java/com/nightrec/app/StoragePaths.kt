package com.nightrec.app

import android.content.Context
import java.io.File

/**
 * app-private storage layout (T012).
 * Original is immutable and never written by Clean; Clean writes derivation only; temp is disposable.
 */
class StoragePaths(val filesDir: File, val cacheDir: File) {
    val original: File = File(filesDir, "original")
    val clean: File = File(filesDir, "clean/v1")
    val temp: File = File(cacheDir, "temp")

    fun sessionOriginal(sessionId: Long): File = File(original, sessionId.toString())
    fun sessionClean(sessionId: Long): File = File(clean, sessionId.toString())
    fun segmentFile(sessionId: Long, index: Int): File = File(sessionOriginal(sessionId), "%05d.m4a".format(index))
    /** Writer must only ever open this .open temp; seal renames it into [segmentFile] atomically. */
    fun segmentOpenTemp(sessionId: Long, index: Int): File = File(sessionOriginal(sessionId), "%05d.m4a.open".format(index))
    fun segmentChecksum(sessionId: Long, index: Int): File = File(sessionOriginal(sessionId), "%05d.m4a.sha256".format(index))

    fun ensure() {
        listOf(original, clean, temp).forEach { it.mkdirs() }
    }

    companion object {
        fun of(context: Context): StoragePaths = StoragePaths(context.filesDir, context.cacheDir)
    }
}