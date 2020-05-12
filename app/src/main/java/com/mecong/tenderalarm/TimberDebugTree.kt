package com.mecong.tenderalarm

import android.content.Context
import android.util.Log
import com.hypertrack.hyperlog.HyperLog
import timber.log.Timber

class TimberDebugTree(context: Context) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            Log.VERBOSE -> {
                HyperLog.v(tag, message)
            }

            Log.DEBUG -> {
                HyperLog.d(tag, message)
            }

            Log.ERROR -> {
                HyperLog.e(tag, message)
            }

            Log.WARN -> {
                HyperLog.w(tag, message)
            }

            Log.ASSERT -> {
                HyperLog.a(message)
            }
        }

        if (t != null) {
            HyperLog.exception(tag, t as Exception)
        }
    }

    init {
        HyperLog.initialize(context)
        HyperLog.setLogLevel(Log.INFO)
    }
}