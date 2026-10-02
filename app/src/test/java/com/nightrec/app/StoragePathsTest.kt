package com.nightrec.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class StoragePathsTest {
    private val root = Files.createTempDirectory("nightrec-storage").toFile()
    private val paths = StoragePaths(File(root, "files"), File(root, "cache"))

    @Test
    fun originalCleanAndTempAreSeparated() {
        paths.ensure()
        assertTrue(paths.original.isDirectory)
        assertTrue(paths.clean.isDirectory)
        assertTrue(paths.temp.isDirectory)
        assertFalse(paths.original.absolutePath == paths.clean.absolutePath)
        assertTrue(paths.temp.absolutePath.startsWith(paths.cacheDir.absolutePath))
    }

    @Test
    fun segmentPathsStayUnderOriginalsPerSession() {
        val seg = paths.segmentFile(42, 3)
        val open = paths.segmentOpenTemp(42, 3)
        val sum = paths.segmentChecksum(42, 3)
        assertEquals(paths.sessionOriginal(42), seg.parentFile)
        assertEquals(seg.parentFile, open.parentFile)
        assertEquals("00003.m4a", seg.name)
        assertEquals("00003.m4a.open", open.name)
        assertEquals("00003.m4a.sha256", sum.name)
    }

    @Test
    fun cleanDerivationNeverSharesDirectoryWithOriginal() {
        assertEquals(paths.sessionClean(7), File(paths.clean, "7"))
        assertFalse(paths.sessionClean(7).absolutePath.startsWith(paths.sessionOriginal(7).absolutePath))
    }
}