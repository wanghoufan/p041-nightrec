package com.nightrec.app

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** All callers reserve durably before networking. Malformed state fails closed. */
class RequestBudget(private val file: File, private val limit: Int, private val initialUsed: Int = 0) {
    init { require(limit >= 0 && initialUsed >= 0) }
    private val monitor = monitors.computeIfAbsent(file.canonicalPath) { Any() }
    val used: Int get() = synchronized(monitor) { readCount() }
    private fun readCount(): Int = if (!file.exists()) initialUsed else
        file.readText().trim().toIntOrNull()?.takeIf { it >= 0 } ?: limit
    fun reserve(): Boolean = synchronized(monitor) {
        file.parentFile?.mkdirs()
        RandomAccessFile(File(file.path + ".lock"), "rw").use { lockFile ->
            lockFile.channel.lock().use {
                val count = readCount()
                if (count >= limit) return@synchronized false
                val pending = File(file.path + ".pending")
                FileOutputStream(pending).use { out -> out.write((count + 1).toString().toByteArray()); out.fd.sync() }
                check(pending.renameTo(file)) { "Cannot durably reserve recognition request" }
                true
            }
        }
    }
    companion object { private val monitors = ConcurrentHashMap<String, Any>() }
}

/**
 * A bounded trailing mono PCM16 window; reset always discards pre-gap audio.
 *
 * [silenceRmsThreshold] > 0 时，整段窗口 RMS 低于该阈值会被判定为静音并跳过
 * （不产出识别请求，避免在空场/间隙空跑 AudD 额度）。默认 0 = 不启用（保持旧行为）。
 */
class RecognitionWindow(
    private val rate: Int,
    private val channels: Int,
    windowMs: Long,
    intervalMs: Long,
    private val silenceRmsThreshold: Int = 0,
) {
    private val windowFrames = (rate * windowMs / 1000).toInt()
    private val intervalFrames = rate * intervalMs / 1000
    private val ring = ByteArray(windowFrames * 2)
    private var write = 0
    private var filled = 0
    private var elapsed = 0L
    init { require(rate > 0 && channels in 1..2 && windowMs > 0 && intervalMs >= windowMs && windowFrames > 0 && silenceRmsThreshold >= 0) }
    fun append(bytes: ByteArray, logicalMs: Long): Pair<ByteArray, Long>? {
        require(bytes.size % (channels * 2) == 0 && logicalMs >= 0)
        val frames = bytes.size / (channels * 2)
        for (frame in 0 until frames) {
            var sum = 0
            for (channel in 0 until channels) {
                val offset = (frame * channels + channel) * 2
                sum += ((bytes[offset].toInt() and 255) or (bytes[offset + 1].toInt() shl 8)).toShort().toInt()
            }
            val mono = sum / channels
            ring[write] = mono.toByte(); ring[write + 1] = (mono shr 8).toByte()
            write = (write + 2) % ring.size; filled = minOf(ring.size, filled + 2)
        }
        elapsed += frames
        if (filled < ring.size || elapsed < intervalFrames) return null
        elapsed %= intervalFrames
        val output = ByteArray(ring.size)
        ring.copyInto(output, 0, write, ring.size); ring.copyInto(output, ring.size - write, 0, write)
        if (silenceRmsThreshold > 0 && rms(output) < silenceRmsThreshold) return null
        return output to (logicalMs + frames * 1000L / rate - windowFrames * 1000L / rate).coerceAtLeast(0)
    }
    fun reset() { write = 0; filled = 0; elapsed = 0; ring.fill(0) }

    /** mono PCM16 little-endian 的均方根幅度（0..32767）。 */
    companion object {
        fun rms(pcm: ByteArray): Int {
            if (pcm.size < 2) return 0
            var sumSquares = 0.0
            var i = 0
            while (i + 1 < pcm.size) {
                val sample = ((pcm[i].toInt() and 255) or (pcm[i + 1].toInt() shl 8)).toShort().toInt()
                sumSquares += (sample.toDouble() * sample)
                i += 2
            }
            return kotlin.math.sqrt(sumSquares / (pcm.size / 2)).toInt()
        }
    }
}

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

object DerivedAudio {
    fun writeCopy(original: File, output: File) {
        require(original.canonicalFile != output.canonicalFile) { "Derived asset cannot replace Original" }
        require(!output.exists()) { "Derived asset already exists" }
        output.parentFile?.mkdirs()
        val pending = File(output.path + ".pending")
        require(pending.canonicalFile != original.canonicalFile)
        try {
            original.inputStream().use { source -> FileOutputStream(pending).use { target -> source.copyTo(target); target.fd.sync() } }
            check(pending.renameTo(output))
        } finally { pending.delete() }
    }
}

class SessionClock(monotonic: Long, wall: Long) {
    private var anchor = monotonic
    private var wallAnchor = wall
    private var accrued = 0L
    private var active = true
    fun logicalAt(now: Long): Long = accrued + if (active) (now - anchor).coerceAtLeast(0) else 0
    fun wallAt(now: Long): Long = wallAnchor + (now - anchor).coerceAtLeast(0)
    fun pause(now: Long) { if (active) { accrued = logicalAt(now); active = false } }
    fun resume(now: Long, wall: Long) { check(!active); anchor = now; wallAnchor = wall; active = true }
}
