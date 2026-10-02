package com.nightrec.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nightrec.app.data.NightSessionEntity
import com.nightrec.app.data.NightRecDatabase
import com.nightrec.app.recognition.RecognitionCoordinator
import com.nightrec.app.recognition.RecognitionEngine
import com.nightrec.app.recognition.RecognitionPolicy
import com.nightrec.app.recognition.RecognitionRequest
import com.nightrec.app.recognition.RecognitionResult
import java.util.concurrent.atomic.AtomicInteger
import com.nightrec.app.recognition.SessionTimelineReader
import com.nightrec.app.recognition.TimelineEntry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** T057: 识别结果幂等持久化 + 稳定后 upsert 一条 TrackOccurrence；未命中落 unknown range。 */
@RunWith(AndroidJUnit4::class)
class RecognitionPersistenceTest {
    private lateinit var db: NightRecDatabase
    private lateinit var budgetFile: File
    private var sessionId = 0L

    private val clock = object : ClockProvider {
        override fun monotonicMs() = 0L
        override fun wallMs() = 1000L
    }

    private val engine = object : RecognitionEngine {
        override val provider = "audd"
        override suspend fun recognize(monoPcm16: ByteArray, sampleRate: Int, audioTimestampMs: Long) =
            RecognitionResult.unmatched(provider, audioTimestampMs)
    }

    @Before fun open() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, NightRecDatabase::class.java).build()
        budgetFile = File(context.cacheDir, "recognition-budget-${System.nanoTime()}.txt")
        sessionId = db.sessions().insert(
            NightSessionEntity(
                name = "Tonight", place = null, state = "RECORDING",
                wallStartMs = 0, wallEndMs = null, logicalDurationMs = 0,
                createdAt = 0, updatedAt = 0,
            ),
        )
    }

    @After fun close() {
        db.close()
        budgetFile.delete()
    }

    private fun coordinator() = RecognitionCoordinator(
        sessionId = sessionId,
        engine = engine,
        budget = RequestBudget(budgetFile, 300),
        db = db,
        clock = clock,
    )

    @Test fun duplicateObservationPersistsOnceAndSingleHitDoesNotStabilize() = runBlocking {
        val c = coordinator()
        val hit = RecognitionResult("audd", true, "42", "Warriors", "Imagine Dragons", null, null, 12_000)
        c.persist(hit)
        c.persist(hit)
        assertEquals(1, db.observations().bySession(sessionId).size)
        assertEquals(0, db.occurrences().countBySession(sessionId))
    }

    @Test fun secondHitStabilizesIntoOccurrence() = runBlocking {
        val c = coordinator()
        c.persist(RecognitionResult("audd", true, "42", "Warriors", "Imagine Dragons", null, null, 12_000))
        c.persist(RecognitionResult("audd", true, "42", "Warriors", "Imagine Dragons", null, null, 24_000))
        assertEquals(2, db.observations().bySession(sessionId).size)
        assertEquals(1, db.occurrences().countBySession(sessionId))
    }

    @Test fun unmatchedObservationCreatesUnrecognizedRange() = runBlocking {
        val c = coordinator()
        c.persist(RecognitionResult.unmatched("audd", 12_000))
        assertEquals(1, db.observations().bySession(sessionId).size)
        assertEquals(1, db.unrecognized().unresolved(sessionId).size)
        assertEquals(0, db.occurrences().countBySession(sessionId))
    }

    @Test fun hitCooldownSkipsRequestsUntilCooldownElapses() = runBlocking {
        val calls = AtomicInteger(0)
        val hitEngine = object : RecognitionEngine {
            override val provider = "audd"
            override suspend fun recognize(monoPcm16: ByteArray, sampleRate: Int, audioTimestampMs: Long): RecognitionResult {
                calls.incrementAndGet()
                return RecognitionResult("audd", true, "42", "Warriors", "Imagine Dragons", null, null, audioTimestampMs)
            }
        }
        val c = RecognitionCoordinator(
            sessionId = sessionId,
            engine = hitEngine,
            budget = RequestBudget(budgetFile, 300),
            db = db,
            clock = clock,
            policy = RecognitionPolicy(hitCooldownMs = 60_000),
        )
        c.recognize(RecognitionRequest(ByteArray(10), 12_000))
        c.recognize(RecognitionRequest(ByteArray(10), 24_000)) // 冷却期内 -> 不发请求
        c.recognize(RecognitionRequest(ByteArray(10), 72_000)) // 冷却结束 -> 再发
        assertEquals(2, calls.get())
    }

    @Test fun timelineReflectsConfirmedTrackAndUnknowns() = runBlocking {
        val c = coordinator()
        c.persist(RecognitionResult("audd", true, "42", "Warriors", "Imagine Dragons", null, null, 12_000))
        c.persist(RecognitionResult("audd", true, "42", "Warriors", "Imagine Dragons", null, null, 24_000))
        c.persist(RecognitionResult.unmatched("audd", 36_000))
        val entries = SessionTimelineReader(db).timeline(sessionId)
        assertEquals(2, entries.size)
        val track = entries.first { it.kind == TimelineEntry.Kind.TRACK }
        assertEquals("Warriors", track.title)
        assertEquals("Imagine Dragons", track.artist)
        val unknown = entries.first { it.kind == TimelineEntry.Kind.UNKNOWN }
        assertEquals(36_000L, unknown.logicalStartMs)
    }
}