package com.nightrec.app
import org.junit.Assert.*
import org.junit.Test
class CleanupTest {
 private fun source()=FloatArray(4000){if(it%2==0)0.8f else 0.1f}
 @Test fun uncertainDetectionRetainsExactInput(){val input=source();assertArrayEquals(input,ConservativeCleanup.apply(input,1000,listOf(SpeechWindow(0,2000,.5f,0f,0f,64.0,0))),0f)}
 @Test fun musicAndVocalVetoEvenVeryConfidentSpeech(){val input=source();for(v in listOf(SpeechWindow(0,2000,.999f,.8f,0f,64.0,0),SpeechWindow(0,2000,.999f,0f,.9f,64.0,0))){assertArrayEquals(input,ConservativeCleanup.apply(input,1000,listOf(v)),0f)}}
 @Test fun oneHitCannotModifyAudio(){val input=source();assertArrayEquals(input,ConservativeCleanup.apply(input,1000,listOf(SpeechWindow(0,975,.999f,0f,0f,64.0,0))),0f)}
 @Test fun highSpeechOnlyModifiesDominantChannelAndCrossfades(){val input=source();val copy=input.copyOf();val out=ConservativeCleanup.apply(input,1000,listOf(SpeechWindow(0,975,.999f,0f,0f,64.0,0),SpeechWindow(975,1950,.999f,0f,0f,64.0,0)));assertArrayEquals(copy,input,0f);assertEquals(input[0],out[0],0f);assertEquals(.68f,out[1000],.0001f);for(i in input.indices step 2)assertEquals(input[i+1],out[i+1],0f);assertEquals(input[3998],out[3998],0f)}
 @Test fun sideMustStayStableAcrossAdjacentWindows(){val input=source();assertArrayEquals(input,ConservativeCleanup.apply(input,1000,listOf(SpeechWindow(0,975,.999f,0f,0f,64.0,0),SpeechWindow(975,1950,.999f,0f,0f,64.0,1))),0f)}
 @Test fun noImbalanceKeepsOriginal(){val input=source();assertArrayEquals(input,ConservativeCleanup.apply(input,1000,listOf(SpeechWindow(0,975,.999f,0f,0f,1.0,0),SpeechWindow(975,1950,.999f,0f,0f,1.0,0))),0f)}
}
