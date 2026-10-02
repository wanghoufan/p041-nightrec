package com.nightrec.app

import com.nightrec.app.recognition.AuddRecognitionEngine
import com.nightrec.app.recognition.PcmWav
import com.nightrec.app.recognition.RecognitionPolicy
import com.nightrec.app.recognition.RecognitionResult
import com.nightrec.app.recognition.TimelineEntry
import com.nightrec.app.recognition.TimelineTrack
import com.nightrec.app.recognition.TimelineUnknown
import com.nightrec.app.recognition.TokenProvider
import com.nightrec.app.recognition.TrackTimeline
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionTest {

    @Test
    fun wavHeaderEncodesMono16Pcm() {
        val pcm = ByteArray(100)
        val wav = PcmWav.encode(pcm, 48_000)
        assertEquals(44 + 100, wav.size)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals("WAVE", String(wav, 8, 4))
        val rate = (wav[24].toInt() and 255) or ((wav[25].toInt() and 255) shl 8) or
            ((wav[26].toInt() and 255) shl 16) or ((wav[27].toInt() and 255) shl 24)
        assertEquals(48_000, rate)
        val dataSize = (wav[40].toInt() and 255) or ((wav[41].toInt() and 255) shl 8)
        assertEquals(100, dataSize)
    }

    @Test
    fun identityPrefersProviderSongId() {
        val withId = RecognitionResult("audd", true, "42", "T", "A", null, null, 0)
        assertEquals("audd:42", withId.identity)
        val noId = RecognitionResult("audd", true, null, "T", "A", null, null, 0)
        assertEquals("audd:T|A", noId.identity)
    }

    @Test
    fun blankTokenNeverHitsNetwork() = runBlocking {
        val engine = AuddRecognitionEngine(tokens = object : TokenProvider {
            override fun token() = ""
        })
        assertNull(engine.recognize(ByteArray(10), 48_000, 0))
    }

    @Test
    fun unmatchedResultIsNotAFailure() {
        val result = RecognitionResult.unmatched("audd", 12_000)
        assertEquals(false, result.matched)
        assertEquals(12_000, result.audioTimestampMs)
    }

    @Test
    fun silentWindowIsSkippedButLoudWindowPasses() {
        val policy = RecognitionPolicy(silenceRmsThreshold = 300)

        val silent = RecognitionWindow(10, 1, 1_000, 1_000, policy.silenceRmsThreshold)
        assertNull(silent.append(ByteArray(20), 0))

        val loud = java.nio.ByteBuffer.allocate(20).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .apply { repeat(10) { putShort(5_000) } }.array()
        assertTrue(RecognitionWindow.rms(loud) >= 4_900)
        val gated = RecognitionWindow(10, 1, 1_000, 1_000, policy.silenceRmsThreshold)
        assertNotNull(gated.append(loud, 0))
    }

    @Test
    fun policyRejectsInvalidWindow() {
        assertThrows(IllegalArgumentException::class.java) {
            RecognitionPolicy(windowMs = 12_000, intervalMs = 5_000)
        }
    }

    @Test
    fun timelineMergesTracksAndUnknownsSortedByTime() {
        val entries = TrackTimeline.build(
            tracks = listOf(
                TimelineTrack(1000, 5000, "A", "X"),
                TimelineTrack(5000, 9000, "B", "Y"),
            ),
            unknowns = listOf(TimelineUnknown(0, 1000)),
            sessionDurationMs = 9000,
        )
        assertEquals(3, entries.size)
        assertEquals(TimelineEntry.Kind.UNKNOWN, entries[0].kind)
        assertEquals(TimelineEntry.Kind.TRACK, entries[1].kind)
        assertEquals("A", entries[1].title)
        assertEquals("B", entries[2].title)
        assertEquals(0L, entries[0].logicalStartMs)
        assertEquals(9000L, entries[2].logicalEndMs)
    }

    @Test
    fun timelineClampsSegmentsToSessionDuration() {
        val entries = TrackTimeline.build(
            tracks = listOf(TimelineTrack(0, 99_000, "A", "X")),
            unknowns = emptyList(),
            sessionDurationMs = 60_000,
        )
        assertEquals(60_000L, entries.single().logicalEndMs)
    }

    @Test
    fun trackEndsFollowNextStart() {
        assertEquals(listOf(5000L, 9000L), TrackTimeline.trackEnds(listOf(1000L, 5000L), 9000L))
    }
}