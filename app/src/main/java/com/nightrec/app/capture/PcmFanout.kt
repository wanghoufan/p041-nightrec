package com.nightrec.app.capture

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * 有界、可丢帧的 PCM fanout（T040）。
 *
 * 采集线程只做非阻塞 [offer]：识别消费者若跟不上，直接丢弃最旧/最新块并计数，
 * 绝不阻塞录音。Original 写入与识别消费由此解耦。
 */
class PcmFanout(private val capacity: Int = 32) {
    private val queue = ArrayBlockingQueue<Chunk>(capacity)
    private val dropped = AtomicLong(0)

    val droppedChunks: Long get() = dropped.get()

    fun offer(pcm: ByteArray, offset: Int, size: Int, logicalMs: Long) {
        if (!queue.offer(Chunk(pcm.copyOfRange(offset, offset + size), logicalMs))) {
            dropped.incrementAndGet()
        }
    }

    fun poll(timeoutMs: Long): Chunk? = queue.poll(timeoutMs, TimeUnit.MILLISECONDS)

    fun clear() = queue.clear()

    data class Chunk(val pcm: ByteArray, val logicalMs: Long)
}