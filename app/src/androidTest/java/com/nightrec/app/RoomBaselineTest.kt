package com.nightrec.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nightrec.app.data.NightRecDatabase
import com.nightrec.app.data.RecognitionObservationEntity
import com.nightrec.app.data.SessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** T013 baseline: schema v1 opens, single-active-session invariant, idempotent observations. */
@RunWith(AndroidJUnit4::class)
class RoomBaselineTest {
    private lateinit var db: NightRecDatabase

    @Before fun open() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, NightRecDatabase::class.java).build()
    }

    @After fun close() = db.close()

    @Test fun onlyOneActiveSessionAllowed() = runBlocking<Unit> {
        val repo = SessionRepository(db, object : ClockProvider {
            override fun monotonicMs() = 0L
            override fun wallMs() = 1000L
        })
        repo.createActiveSession("Tonight", null)
        assertEquals(1, db.sessions().activeCount())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.createActiveSession("Later", null) }
        }
    }

    @Test fun duplicateObservationIsIgnoredByIdempotencyKey() = runBlocking {
        val dao = db.observations()
        val row = RecognitionObservationEntity(
            idempotencyKey = "s1:18000:warriors",
            sessionId = 1,
            audioTimestampMs = 18_000,
            provider = "audd",
            rawSongId = "warriors",
            confidenceLikeState = "HIT",
            receivedAt = 100,
        )
        assertEquals(1L, dao.insert(row))
        assertEquals(-1L, dao.insert(row))
        assertEquals(1, dao.bySession(1).size)
    }
}