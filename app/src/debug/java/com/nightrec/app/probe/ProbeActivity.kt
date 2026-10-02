package com.nightrec.app.probe
import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.*
class ProbeActivity:Activity(){
 private var player:android.media.MediaPlayer?=null
 override fun onResume(){super.onResume();ProbeService.visibility(true)}
 override fun onPause(){ProbeService.visibility(false);super.onPause()}
 override fun onCreate(b:Bundle?){super.onCreate(b); val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,80,32,32)}
 box.addView(TextView(this).apply{text="NightRec • 录音/识曲可行性测试\n独立可清除测试，不是正式 Night Session";textSize=20f})
 box.addView(Button(this).apply{text="开始后台测试";setOnClickListener{if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),10) else begin()}})
 box.addView(Button(this).apply{text="停止并保存测试";setOnClickListener{startService(Intent(this@ProbeActivity,ProbeService::class.java).setAction("stop"))}})
 box.addView(Button(this).apply{text="循环播放官方识曲测试音源";setOnClickListener{player?.release();player=android.media.MediaPlayer().apply{setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).setAllowedCapturePolicy(android.media.AudioAttributes.ALLOW_CAPTURE_BY_NONE).build());setPreferredDevice((getSystemService(AUDIO_SERVICE) as android.media.AudioManager).getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS).firstOrNull{it.type==android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER});assets.openFd("recognition-example.mp3").use{setDataSource(it.fileDescriptor,it.startOffset,it.length)};setVolume(.4f,.4f);isLooping=true;prepare();start()}}})
 box.addView(Button(this).apply{text="循环播放第二首测试音乐";setOnClickListener{player?.release();player=android.media.MediaPlayer().apply{setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).setAllowedCapturePolicy(android.media.AudioAttributes.ALLOW_CAPTURE_BY_NONE).build());setPreferredDevice((getSystemService(AUDIO_SERVICE) as android.media.AudioManager).getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS).firstOrNull{it.type==android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER});assets.openFd("recognition-second.mp3").use{setDataSource(it.fileDescriptor,it.startOffset,it.length)};setVolume(.4f,.4f);isLooping=true;prepare();start()}}})
 box.addView(Button(this).apply{text="停止测试音乐";setOnClickListener{player?.release();player=null}})
 setContentView(box)
 }
 private fun begin(){startForegroundService(Intent(this,ProbeService::class.java))}
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==10&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)begin()}
}
