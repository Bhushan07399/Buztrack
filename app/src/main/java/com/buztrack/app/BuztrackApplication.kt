package com.buztrack.app

import android.app.Application
import com.buztrack.app.data.repository.LocalBuztrackRepository

class BuztrackApplication : Application() {
    val repository: LocalBuztrackRepository by lazy {
        LocalBuztrackRepository.getInstance(this)
    }
}
