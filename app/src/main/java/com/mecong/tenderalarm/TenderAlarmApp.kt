package com.mecong.tenderalarm

import android.app.Application
import timber.log.Timber
import timber.log.Timber.DebugTree

class TenderAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(TimberReleaseTree())
        }
    }
}