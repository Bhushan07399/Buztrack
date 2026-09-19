package com.buztrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.buztrack.app.ui.navigation.BuztrackMainApp
import com.buztrack.app.ui.theme.BuztrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as BuztrackApplication).repository

        setContent {
            BuztrackTheme {
                BuztrackMainApp(repository = repository)
            }
        }
    }
}
