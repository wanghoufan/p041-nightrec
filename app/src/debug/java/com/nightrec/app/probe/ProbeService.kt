package com.nightrec.app.probe
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.*
import android.media.*
import com.nightrec.app.BuildConfig
import com.nightrec.app.R
import java.io.*
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** Throwaway Spike host: no production Session metadata, no UI shortcuts in release. */
class ProbeService:Service(){
 companion object { @Volatile private var activeProbe:ProbeService?=null;fun visibility(foreground:Boolean){activeProbe?.event("VISIBILITY",JSONObject().put("foreground",foreground))} }
 private val running=AtomicBoolean(false);private var thread:Thread?=null;private var wake:PowerManager.WakeLock?=null
 private lateinit var dir:File;private val windows=ArrayBlockingQueue<Pair<ByteArray,Long>>(1)
 private var frames=0L;private var rate=48000;private var channels=2;private var count=0
 override fun onBind(i:Intent?)=null
 override fun onStartCommand(i:Intent?,f:Int,id:Int):Int{
  if(i?.action=="stop"){running.set(false);return START_NOT_STICKY}
  if(running.getAndSet(true))return START_NOT_STICKY
  dir=File(filesDir,"probe/"+System.currentTimeMillis()).apply{mkdirs()}
  activeProbe=this
  val nm=getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel("probe","NightRec 测试录音",NotificationManager.IMPORTANCE_LOW))
  startForeground(91,Notification.Builder(this,"probe").setSmallIcon(R.drawable.ic_wave_mono).setContentTitle("NightRec • 后台录音验证").setContentText("测试音频仅存设备本地；AudD 同意后的短片段用于识曲验证").setOngoing(true).build())
  wake=(getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"NightRec:probe").apply{acquire(4*60*60*1000L)}
  Thread({recognize()},"probe-recognition").start();thread=Thread({capture()},"probe-audio").apply{start()};return START_NOT_STICKY
 }
 @Synchronized private fun event(kind:String,extra:JSONObject=JSONObject()){extra.put("event",kind).put("wall",System.currentTimeMillis()).put("frames",frames);File(dir,"events.jsonl").appendText(extra.toString()+"\n")}
 // RECORD_AUDIO is requested by ProbeActivity before the probe service is ever started.
 @SuppressLint("MissingPermission")
 private fun capture(){
  Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
  var record:AudioRecord?=null;var encoder:ProbeEncoder?=null
  try{
   for(c in listOf(2,1)){val mask=if(c==2)AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO;val size=AudioRecord.getMinBufferSize(rate,mask,AudioFormat.ENCODING_PCM_16BIT);if(size<=0)continue
    val candidate=AudioRecord(MediaRecorder.AudioSource.UNPROCESSED,rate,mask,AudioFormat.ENCODING_PCM_16BIT,size*4);if(candidate.state==AudioRecord.STATE_INITIALIZED){record=candidate;channels=c;break}else candidate.release()}
   val ar=record?:error("No recording format");ar.startRecording();event("START",JSONObject().put("sampleRate",rate).put("channels",channels))
   val pcm=ByteArray(4096);val window=ByteArrayOutputStream();var next=rate*30L;var windowStart=0L;var segmentStart=0L
   encoder=ProbeEncoder(File(dir,"000.m4a"),rate,channels)
   while(running.get()){
    val n=ar.read(pcm,0,pcm.size,AudioRecord.READ_BLOCKING);if(n<=0)error("Audio read error $n")
    encoder!!.write(pcm,n);val readFrames=n/(2*channels)
    if(frames>=next-rate*12L){if(window.size()==0)windowStart=frames*1000/rate;for(k in 0 until n step 2*channels){var sum=0;for(c in 0 until channels)sum+=((pcm[k+c*2].toInt()and 255)or(pcm[k+c*2+1].toInt() shl 8)).toShort().toInt();val mono=sum/channels;window.write(mono and 255);window.write((mono shr 8)and 255)}}
    frames+=readFrames
    if(frames>=next){windows.offer(window.toByteArray() to windowStart);window.reset();next+=rate*30L}
    if(frames-segmentStart>=rate*300L){encoder!!.close();event("SEALED",JSONObject().put("segment",count));count++;segmentStart=frames;encoder=ProbeEncoder(File(dir,"%03d.m4a".format(count)),rate,channels)}
    if(frames%(rate*10)<readFrames)event("HEARTBEAT")
   }
   ar.stop();encoder?.close();encoder=null;event("STOP",JSONObject().put("segments",count+1))
  }catch(e:Exception){event("FAIL",JSONObject().put("type",e.javaClass.simpleName).put("message",e.message))}finally{running.set(false);record?.release();try{encoder?.close()}catch(_:Exception){};wake?.let{if(it.isHeld)it.release()};stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
 }
 private fun recognize(){
  val prefs=getSharedPreferences("audd-budget",MODE_PRIVATE);val client=OkHttpClient.Builder().callTimeout(java.time.Duration.ofSeconds(20)).build()
  while(running.get()||windows.isNotEmpty()){
   val w=windows.poll(1,java.util.concurrent.TimeUnit.SECONDS)?:continue
   // Probe is activated by explicit developer test consent; only short windows leave device.
   val used=prefs.getInt("attempted",1);if(used>=BuildConfig.AUDD_LIMIT){event("BUDGET_EXHAUSTED");continue};prefs.edit().putInt("attempted",used+1).commit()
   try{
    val wav=wav(w.first,rate)
    val body=MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("api_token",BuildConfig.AUDD_TOKEN).addFormDataPart("file","window.wav",wav.toRequestBody("audio/wav".toMediaType())).build()
    client.newCall(Request.Builder().url("https://api.audd.io/").post(body).build()).execute().use{response->val j=JSONObject(response.body?.string()?:"{}");val song=j.optJSONObject("result");event("RECOGNITION",JSONObject().put("http",response.code).put("logicalMs",w.second).put("status",j.optString("status")).put("title",song?.optString("title")).put("artist",song?.optString("artist")))}
   }catch(e:Exception){event("RECOGNITION_FAILURE",JSONObject().put("type",e.javaClass.simpleName))}
  }
 }
 override fun onDestroy(){activeProbe=null;running.set(false);super.onDestroy()}
}
private fun wav(pcm:ByteArray,rate:Int):ByteArray{val b=ByteBuffer.allocate(44+pcm.size).order(java.nio.ByteOrder.LITTLE_ENDIAN);b.put("RIFF".toByteArray()).putInt(36+pcm.size).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate*2).putShort(2).putShort(16).put("data".toByteArray()).putInt(pcm.size).put(pcm);return b.array()}
private class ProbeEncoder(private val file:File,rate:Int,private val channels:Int){
 private val temp=File(file.path+".open");private val codec=MediaCodec.createEncoderByType("audio/mp4a-latm");private val mux=MediaMuxer(temp.path,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);private var track=-1;private var started=false;private var frames=0L;private val hz=rate
 init{val format=MediaFormat.createAudioFormat("audio/mp4a-latm",rate,channels).apply{setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC);setInteger(MediaFormat.KEY_BIT_RATE,192000);setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,16384)};codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);codec.start()}
 fun write(bytes:ByteArray,n:Int){var off=0;while(off<n){val i=codec.dequeueInputBuffer(10000);if(i>=0){val buf=codec.getInputBuffer(i)!!;buf.clear();val size=minOf(buf.remaining(),n-off);buf.put(bytes,off,size);codec.queueInputBuffer(i,0,size,frames*1000000/hz,0);frames+=size/(2*channels);off+=size};drain(false)}}
 private fun drain(eos:Boolean){val info=MediaCodec.BufferInfo();var idle=0;while(true){val o=codec.dequeueOutputBuffer(info,if(eos)10000 else 0);if(o==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){track=mux.addTrack(codec.outputFormat);mux.start();started=true}else if(o>=0){val b=codec.getOutputBuffer(o)!!;if(info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG!=0)info.size=0;if(info.size>0&&started){b.position(info.offset);b.limit(info.offset+info.size);mux.writeSampleData(track,b,info)};val done=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0;codec.releaseOutputBuffer(o,false);if(done)return}else{if(!eos)return;if(++idle>200)error("Encoder EOS timeout")}}}
 fun close(){val i=codec.dequeueInputBuffer(100000);check(i>=0);codec.queueInputBuffer(i,0,0,frames*1000000/hz,MediaCodec.BUFFER_FLAG_END_OF_STREAM);drain(true);codec.stop();codec.release();if(started)mux.stop();mux.release();RandomAccessFile(temp,"rw").use{it.fd.sync()};check(temp.renameTo(file));File(file.path+".sha256").writeText(java.security.MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString(""){"%02x".format(it)})}
}
