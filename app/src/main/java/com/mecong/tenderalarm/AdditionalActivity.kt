package com.mecong.tenderalarm

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
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
    }

    private fun rateMe() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + this.packageName)))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + this.packageName)))
        }
    }
}