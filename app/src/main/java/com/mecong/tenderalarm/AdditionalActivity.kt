package com.mecong.tenderalarm

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AdditionalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_additional)
        val closeBtn = findViewById<Button>(R.id.closeBtn)
        closeBtn.setOnClickListener { finish() }
    }
}