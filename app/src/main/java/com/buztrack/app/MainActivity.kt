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

        val app = application as BuztrackApplication
        val repository = app.repository
        val authRepository = app.authRepository
        val sessionManager = app.sessionManager

        setContent {
            BuztrackTheme {
                BuztrackMainApp(
                    repository = repository,
                    authRepository = authRepository,
                    sessionManager = sessionManager
                )
            }
        }
    }
}
