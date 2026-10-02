package com.nightrec.app
import org.junit.Test
import org.junit.Assert.*
class DomainTest {
 @Test fun stateRejectsUnconfirmedStop() { val m=SessionStateMachine(); m.move(SessionState.STARTING);m.move(SessionState.RECORDING);assertThrows(IllegalStateException::class.java){m.move(SessionState.READY)};assertEquals(SessionState.RECORDING,m.state) }
 @Test fun awayResumeKeepsValidState() {val m=SessionStateMachine();m.move(SessionState.STARTING);m.move(SessionState.RECORDING);m.move(SessionState.AWAY);m.move(SessionState.RECORDING);assertEquals(SessionState.RECORDING,m.state)}
 @Test fun gapDoesNotEnterLogicalTime() {val s=listOf(TimedSegment("a",0,1000,10000),TimedSegment("b",1000,1000,15000));val m=TimelineMapper(s);assertEquals(SegmentPosition(1,500),m.seek(1500));assertEquals(15500,m.wallTime(1500));assertEquals(2000,m.duration)}
 @Test fun endSeekClampsToLastFrame() { val m=TimelineMapper(listOf(TimedSegment("a",0,1000,0)));assertEquals(SegmentPosition(0,999),m.seek(9999)) }
 @Test fun jitterDoesNotSwitchSong() {val s=TrackStabilizer();assertNull(s.observe("A",0));assertEquals("A",s.observe("A",5000)?.song);assertNull(s.observe("B",6000));assertNull(s.observe("A",7000));assertEquals("A",s.current?.song)}
 @Test fun lateResultsCannotRewindCurrentSong() {val s=TrackStabilizer();s.observe("A",0);s.observe("A",5000);s.observe("B",10000);s.observe("B",15000);s.observe("A",2000);assertEquals("B",s.current?.song)}
 @Test fun repeatedSongMakesNewOccurrence() {val s=TrackStabilizer();s.observe("A",0);s.observe("A",5000);s.observe("B",10000);s.observe("B",15000);s.observe("A",20000);assertEquals(20000L,s.observe("A",25000)?.start)}
 @Test fun loggerRedactsSecretsAndAudio() {assertEquals("[redacted]",SafeLogger.sanitize("Authorization: Bearer abc"));assertEquals("[redacted]",SafeLogger.sanitize("api_token=abc"));assertEquals("[audio omitted]",SafeLogger.sanitize(byteArrayOf(1,2)))}
 @Test fun cleanupMustKeepOriginalWhenConfidenceLow() {assertFalse(CleanupPolicy.shouldProcess(.5,.9));assertFalse(CleanupPolicy.shouldProcess(.99,.9));assertTrue(CleanupPolicy.shouldProcess(.99,.05))}
}
