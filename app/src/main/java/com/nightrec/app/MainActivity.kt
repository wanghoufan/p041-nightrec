package com.nightrec.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nightrec.app.ui.NightRecApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Official androidx SplashScreen (compat). No separate SplashActivity.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // NightRecApp applies NightRecTheme internally so the persisted theme mode wins.
            NightRecApp()
        }
    }
}