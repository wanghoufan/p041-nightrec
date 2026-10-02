package com.nightrec.app

import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class SpikeDeviceTest {
 private val instrumentation=InstrumentationRegistry.getInstrumentation()
 private val context=instrumentation.targetContext
 @Test fun yamnetRunsOnDeviceAndProducesFiniteScores(){
  val model=context.assets.open("yamnet.tflite").use{it.readBytes()}
  val memory=ByteBuffer.allocateDirect(model.size).order(ByteOrder.nativeOrder()).apply{put(model);rewind()}
  Interpreter(memory,Interpreter.Options().setNumThreads(2)).use { interpreter ->
   assertArrayEquals(intArrayOf(15600),interpreter.getInputTensor(0).shape())
   assertArrayEquals(intArrayOf(1,521),interpreter.getOutputTensor(0).shape())
   val input=ByteBuffer.allocateDirect(15600*4).order(ByteOrder.nativeOrder())
   val output=Array(1){FloatArray(521)}
   val start=SystemClock.elapsedRealtime();interpreter.run(input,output)
   val latency=SystemClock.elapsedRealtime()-start
   assertTrue(output[0].all{it.isFinite()&&it>=0&&it<=1})
   File(context.filesDir,"yamnet-spike.json").writeText("{\"latencyMs\":$latency,\"inputSamples\":15600,\"classes\":521}")
  }
 }
 @Test fun classifyTwentyControlledVocalSpeechMixtures(){
  val model=context.assets.open("yamnet.tflite").use{it.readBytes()}
  val memory=ByteBuffer.allocateDirect(model.size).order(ByteOrder.nativeOrder()).apply{put(model);rewind()}
  val rows=mutableListOf<org.json.JSONObject>();var changedTotal=0
  Interpreter(memory,Interpreter.Options().setNumThreads(2)).use { interpreter ->
   repeat(20){sample ->
    val data=instrumentation.context.assets.open("cleanup-ab/%02d.wav".format(sample)).use{it.readBytes()}
    val pcm=ByteBuffer.wrap(data,44,data.size-44).order(ByteOrder.LITTLE_ENDIAN)
    val left=FloatArray((data.size-44)/4);val right=FloatArray(left.size)
    for(i in left.indices){left[i]=pcm.short/32768f;right[i]=pcm.short/32768f}
    var maxSpeech=0f;var maxSinging=0f;var eligible=0;val detections=mutableListOf<SpeechWindow>()
    for(offset in 0..left.size-15600 step 15600){
     val input=ByteBuffer.allocateDirect(15600*4).order(ByteOrder.nativeOrder())
     var leftPower=0.0;var rightPower=0.0
     repeat(15600){i->val l=left[offset+i];val r=right[offset+i];input.putFloat((l+r)/2);leftPower+=l*l;rightPower+=r*r};input.rewind()
     val output=Array(1){FloatArray(521)};interpreter.run(input,output)
     val speech=output[0][0];val singing=(24..32).maxOf{output[0][it]}
     val imbalance=maxOf(leftPower,rightPower)/maxOf(0.000001,minOf(leftPower,rightPower))
     maxSpeech=maxOf(maxSpeech,speech);maxSinging=maxOf(maxSinging,singing)
     if(speech>=0.98f&&singing<=0.05f&&imbalance>=4)eligible++
     detections.add(SpeechWindow(offset*1000L/16000,(offset+15600)*1000L/16000,speech,singing,output[0][132],imbalance,if(leftPower>=rightPower)0 else 1))
     assertTrue(output[0].all{it.isFinite()})
    }
    val original=FloatArray(left.size*2){i->if(i%2==0)left[i/2]else right[i/2]};val unchanged=original.copyOf()
    val clean=ConservativeCleanup.apply(original,16000,detections)
    assertArrayEquals(unchanged,original,0f)
    for(i in original.indices step 2){assertEquals(original[i+1],clean[i+1],0f);assertTrue(kotlin.math.abs(clean[i])>=kotlin.math.abs(original[i])*.849f)}
    val changed=original.indices.count{original[it]!=clean[it]};changedTotal+=changed
    val target=File(context.cacheDir,"cleanup-ab/%02d-clean.wav".format(sample)).apply{parentFile!!.mkdirs()}
    val output=data.copyOf();val encoded=ByteBuffer.wrap(output,44,output.size-44).order(ByteOrder.LITTLE_ENDIAN)
    clean.forEach{encoded.putShort((it*32768).toInt().coerceIn(-32768,32767).toShort())};target.writeBytes(output)
    rows.add(org.json.JSONObject().put("changedSamples",changed).put("originalUnchanged",true).put("untouchedChannelExact",true).put("sample",sample).put("maxSpeech",maxSpeech.toDouble()).put("maxSinging",maxSinging.toDouble()).put("eligibleWindows",eligible).put("inputChecksum",sha256Copy(data)))
   }
  }
  File(context.filesDir,"cleanup-ab-scores.json").writeText(org.json.JSONArray(rows).toString(2));assertTrue("High dominance speech controls must produce a real derived output",changedTotal>0)
 }
 private fun sha256Copy(data:ByteArray):String=java.security.MessageDigest.getInstance("SHA-256").digest(data).joinToString(""){"%02x".format(it)}
 @Test fun actualSealedSegmentsPlayAcrossBoundaryAndSeekTwentyTimes(){
  val root=File(context.filesDir,"probe").listFiles()!!.filter{it.isDirectory&&it.listFiles()!!.count{f->f.extension=="m4a"}>=2}.maxBy{it.name}
  val segments=root.listFiles()!!.filter{it.extension=="m4a"}.sortedBy{it.name}
  assertTrue("At least two real sealed capture segments required",segments.size>=2)
  val hashes=segments.associateWith{sha256(it)}
  lateinit var player:ExoPlayer
  val prepared=CountDownLatch(1)
  instrumentation.runOnMainSync {
   player=ExoPlayer.Builder(context).build()
   player.addListener(object:Player.Listener{override fun onPlaybackStateChanged(state:Int){if(state==Player.STATE_READY)prepared.countDown()}})
   player.setMediaItems(segments.map{MediaItem.fromUri(Uri.fromFile(it))});player.prepare()
  }
  assertTrue("Prepare actual capture",prepared.await(10,TimeUnit.SECONDS))
  val durations=segments.map { f -> android.media.MediaMetadataRetriever().use{r->r.setDataSource(f.path);r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()} }
  val rows=mutableListOf<String>()
  try {
   repeat(20){
    val index=Random(1100+it).nextInt(segments.size);val offset=Random(1200+it).nextLong(0,durations[index]-1000)
    val ready=CountDownLatch(1);val start=SystemClock.elapsedRealtime()
    val listener=object:Player.Listener{override fun onEvents(p:Player,e:Player.Events){if(p.currentMediaItemIndex==index&&p.playbackState==Player.STATE_READY&&kotlin.math.abs(p.currentPosition-offset)<=250)ready.countDown()}}
    instrumentation.runOnMainSync{player.addListener(listener);player.seekTo(index,offset)}
    val completed=ready.await(250,TimeUnit.MILLISECONDS);val elapsed=SystemClock.elapsedRealtime()-start
    instrumentation.runOnMainSync{player.removeListener(listener)}
    rows.add("{\"index\":$index,\"offsetMs\":$offset,\"readyMs\":$elapsed,\"passed\":$completed}")
    assertTrue("Seek $it ready within 250ms ($elapsed ms)",completed)
   }
   val transitioned=CountDownLatch(1)
   instrumentation.runOnMainSync {
    player.addListener(object:Player.Listener{override fun onMediaItemTransition(item:MediaItem?,reason:Int){if(player.currentMediaItemIndex==1&&reason==Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)transitioned.countDown()}})
    player.seekTo(0,durations[0]-1500);player.play()
   }
   assertTrue("Automatic contiguous next segment",transitioned.await(5,TimeUnit.SECONDS))
   hashes.forEach{(f,h)->assertEquals(h,sha256(f))}
  } finally {
   File(context.filesDir,"media3-spike.json").writeText("["+rows.joinToString(",")+"]")
   instrumentation.runOnMainSync{player.release()}
  }
 }
}
