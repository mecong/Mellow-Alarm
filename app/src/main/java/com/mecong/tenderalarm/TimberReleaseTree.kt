package com.mecong.tenderalarm

import android.util.Log
import timber.log.Timber

class TimberReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }
        // log your crash to your favourite
// Sending crash report to Firebase CrashAnalytics
// FirebaseCrash.report(message);
// FirebaseCrash.report(new Exception(message));
    }
}