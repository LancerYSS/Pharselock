package com.denie.phraselock

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createChannel(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
