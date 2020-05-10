package com.mecong.tenderalarm

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View.GONE
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.mecong.tenderalarm.alarm.AutoStartUtils
import kotlinx.android.synthetic.main.activity_additional.*

class AdditionalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_additional)
        val closeBtn = findViewById<Button>(R.id.closeBtn)
        closeBtn.setOnClickListener { finish() }

        appLinkButton.setOnClickListener {
            rateMe()
        }

        appShareButton.setOnClickListener {
            shareApp()
        }

        val autostartIntent = AutoStartUtils.findAutoStartIntent(this)
        if (autostartIntent != null) {
            autostartOpenButton.setOnClickListener {
                AutoStartUtils.runAutostart(this, autostartIntent)
            }
        } else {
            autostartOpenButton.visibility = GONE
            autostartPrompt.visibility = GONE
        }
    }

    private fun rateMe() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${this.packageName}")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=${this.packageName}")))
        }
    }

    private fun shareApp() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        val shareBody = this.getString(R.string.app_share_text, this.packageName)
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "App link")
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, "Share App Link Via :"))
    }
}