package com.nightrec.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nightrec.app.cleanup.CleanupRequest
import com.nightrec.app.cleanup.ConservativeSpeechCleanupEngine
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T083 OriginalImmutabilityTest：Clean 成功/失败前后 Original 的 SHA-256 必须 100% 相同。
 *
 * 覆盖 20 个真实 AB 样本（`cleanup-ab/`）的文件级不可变性：无论 Clean 是否产出派生 asset，
 * Original 文件字节逐字节不变；同时验证引擎拒绝把 Original 当作输出（防写回）。
 */
@RunWith(AndroidJUnit4::class)
class OriginalImmutabilityTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    @Test fun cleanNeverMutatesOriginalAcrossAllAbSamples() = runBlocking {
        val engine = ConservativeSpeechCleanupEngine()
        val root = File(context.cacheDir, "orig-immut").apply { mkdirs() }
        var produced = 0
        repeat(20) { index ->
            val bytes = instrumentation.context.assets
                .open("cleanup-ab/%02d.wav".format(index)).use { it.readBytes() }
            val original = File(root, "%02d.wav".format(index)).apply { writeBytes(bytes) }
            val output = File(root, "%02d-clean.wav".format(index))
            val before = sha256(original)
            val beforeLen = original.length()

            val outcome = engine.process(CleanupRequest(original, output, 0L))

            assertEquals("Original checksum must not change (sample $index)", before, sha256(original))
            assertEquals("Original length must not change (sample $index)", beforeLen, original.length())
            if (outcome.produced) {
                produced++
                assertTrue("Derived asset must differ from Original (sample $index)", sha256(output) != before)
                assertEquals("Derived asset must not be the Original path", false, output.canonicalPath == original.canonicalPath)
            }
        }
        File(context.filesDir, "original-immutability.json").writeText(
            "{\"samples\":20,\"produced\":$produced,\"originalUnchanged\":true}",
        )
    }

    @Test fun engineRefusesToWriteBackToOriginal() {
        val engine = ConservativeSpeechCleanupEngine()
        val file = File(context.cacheDir, "guard-original.wav")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { engine.process(CleanupRequest(file, file, 0L)) }
        }
    }
}