package com.gitinclo.vibepitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gitinclo.vibepitch.ui.navigation.VibePitchNavGraph
import com.gitinclo.vibepitch.ui.theme.VibePitchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibePitchTheme {
                VibePitchNavGraph()
            }
        }
    }
}
