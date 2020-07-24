package com.mecong.tenderalarm

import android.app.Application
import timber.log.Timber

class MellowAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree(this.applicationContext))
        } else {
            Timber.plant(TimberReleaseTree())
        }
    }
}