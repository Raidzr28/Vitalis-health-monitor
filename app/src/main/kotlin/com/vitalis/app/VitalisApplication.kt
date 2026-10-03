package com.vitalis.app

import android.app.Application
import com.vitalis.data.StepRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VitalisApplication : Application() {
    @Inject lateinit var steps: StepRepository

    override fun onCreate() {
        super.onCreate()
        // Keeps the 15-minute step sampling alive across app updates; a no-op without the permission.
        steps.scheduleBackgroundSync()
    }
}
