package com.nightrec.app

enum class SessionState { IDLE,STARTING,RECORDING,AWAY,INTERRUPTED,RECOVERING,STOPPING,PROCESSING,READY }
class SessionStateMachine(initial:SessionState=SessionState.IDLE) {
 var state=initial; private set
 fun move(next:SessionState){
  val allowed=when(state){
   SessionState.IDLE->setOf(SessionState.STARTING)
   SessionState.STARTING->setOf(SessionState.RECORDING,SessionState.INTERRUPTED)
   SessionState.RECORDING->setOf(SessionState.AWAY,SessionState.INTERRUPTED,SessionState.STOPPING)
   SessionState.AWAY->setOf(SessionState.RECORDING,SessionState.STOPPING)
   SessionState.INTERRUPTED->setOf(SessionState.RECOVERING,SessionState.STOPPING)
   SessionState.RECOVERING->setOf(SessionState.RECORDING,SessionState.INTERRUPTED,SessionState.STOPPING)
   SessionState.STOPPING->setOf(SessionState.PROCESSING)
   SessionState.PROCESSING->setOf(SessionState.READY)
   SessionState.READY->emptySet()
  };check(next in allowed){"Illegal transition $state -> $next"};state=next
 }
}
data class TimedSegment(val path:String,val logicalStart:Long,val duration:Long,val wallStart:Long)
data class SegmentPosition(val index:Int,val offset:Long)
class TimelineMapper(val segments:List<TimedSegment>) {
 init{require(segments.all{it.duration>0});require(segments.zipWithNext().all{(a,b)->a.logicalStart+a.duration==b.logicalStart})}
 val duration:Long=segments.lastOrNull()?.let{it.logicalStart+it.duration}?:0
 fun seek(ms:Long):SegmentPosition{check(segments.isNotEmpty());val p=ms.coerceIn(0,duration-1);val i=segments.indexOfLast{it.logicalStart<=p}.coerceAtLeast(0);return SegmentPosition(i,p-segments[i].logicalStart)}
 fun wallTime(ms:Long):Long{val p=seek(ms);return segments[p.index].wallStart+p.offset}
}
data class StableTrack(val song:String,val start:Long,val confirmed:Long)
class TrackStabilizer {
 var current:StableTrack?=null;private set
 private var candidate:String?=null;private var first=0L;private var hits=0;private var newest=-1L
 fun observe(song:String,time:Long):StableTrack?{
  if(time<newest)return null;newest=time
  if(song==current?.song){candidate=null;hits=0;return null}
  if(song!=candidate){candidate=song;first=time;hits=1;return null}
  hits++;if(hits>=2||time-first>=10000){val t=StableTrack(song,first,time);current=t;candidate=null;hits=0;return t};return null
 }
}
object SafeLogger {
 fun sanitize(value:Any):String{if(value is ByteArray)return "[audio omitted]";val s=value.toString();return if(Regex("(?i)(token|authorization|bearer|secret|private.?key)").containsMatchIn(s))"[redacted]" else s.take(200)}
}
object CleanupPolicy {fun shouldProcess(speech:Double,vocal:Double):Boolean=speech>=.95&&vocal<=.1}
interface ClockProvider {fun monotonicMs():Long;fun wallMs():Long}
class SystemClockProvider:ClockProvider {override fun monotonicMs()=android.os.SystemClock.elapsedRealtime();override fun wallMs()=System.currentTimeMillis()}
