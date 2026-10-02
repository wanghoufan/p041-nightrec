package com.nightrec.app
import org.junit.Test
import org.junit.Assert.*
import java.io.File
class ReliabilityTest {
 @Test fun separateBudgetInstancesSharePersistentReservation() {val d=kotlin.io.path.createTempDirectory("shared-budget").toFile();val f=File(d,"budget");val a=RequestBudget(f,7);val b=RequestBudget(f,7);val workers=(0..9).map{i->Thread{repeat(10){if(i%2==0)a.reserve()else b.reserve()}}};workers.forEach{it.start()};workers.forEach{it.join()};assertEquals(7,a.used);assertEquals(7,b.used);d.deleteRecursively()}
 @Test fun damagedBudgetFailsClosed() {val f=File.createTempFile("budget", ".txt").apply{writeText("broken")};assertFalse(RequestBudget(f,300).reserve());f.delete()}
 @Test fun stereoDownmixPreservesSignedSamplesAndWindowTimestamp() {val w=RecognitionWindow(10,2,1000,2000);val input=java.nio.ByteBuffer.allocate(80).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply{repeat(20){putShort((-2000).toShort());putShort(1000)}}.array();val result=w.append(input,5000)!!;assertEquals(6000L,result.second);val mono=java.nio.ByteBuffer.wrap(result.first).order(java.nio.ByteOrder.LITTLE_ENDIAN);repeat(10){assertEquals((-500).toShort(),mono.short)}}

 @Test fun budgetPersistsAndStopsAllConsumers() {val f=File.createTempFile("budget", ".txt");f.delete();val b=RequestBudget(f,2);assertTrue(b.reserve());assertTrue(b.reserve());assertFalse(b.reserve());assertFalse(RequestBudget(f,2).reserve());f.delete()}
 @Test fun concurrentReservationsCannotExceedLimit() {val f=File.createTempFile("budget", ".txt");f.delete();val b=RequestBudget(f,10);val threads=(0..19).map{Thread{repeat(10){b.reserve()}}};threads.forEach{it.start()};threads.forEach{it.join()};assertEquals(10,b.used);f.delete()}
 @Test fun gapResetsWindowAndKeepsLogicalStart() {val w=RecognitionWindow(10,1,1000,2000);assertNull(w.append(ByteArray(20),0));assertNotNull(w.append(ByteArray(20),1000));w.reset();assertNull(w.append(ByteArray(20),5000))}
 @Test fun sealedOriginalHasSameChecksumAfterDerivedWrite() {val d=kotlin.io.path.createTempDirectory("nightrec-test").toFile();val original=File(d,"original").apply{writeBytes(ByteArray(10000){(it%255).toByte()})};val before=sha256(original);val out=File(d,"clean");DerivedAudio.writeCopy(original,out);assertEquals(before,sha256(original));assertArrayEquals(original.readBytes(),out.readBytes());assertThrows(IllegalArgumentException::class.java){DerivedAudio.writeCopy(original,original)};d.deleteRecursively()}
 @Test fun wallClockJumpDoesNotChangeLogicalDuration() {val c=SessionClock(1000,10000);assertEquals(1000L,c.logicalAt(2000));assertEquals(11000L,c.wallAt(2000));c.pause(2000);c.resume(5000,50000);assertEquals(1500L,c.logicalAt(5500));assertEquals(50500L,c.wallAt(5500))}
}
