package com.mecong.tenderalarm.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.R

class LogsActivity : AppCompatActivity(), MyRecyclerViewAdapter.ItemClickListener {
    var adapter: MyRecyclerViewAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)
        val recyclerView = findViewById<RecyclerView>(R.id.logsList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyRecyclerViewAdapter(this, HyperLog.getDeviceLogsAsStringList(false))
        adapter!!.setClickListener(this)
        recyclerView.adapter = adapter
        val btnClearLogs = findViewById<Button>(R.id.btnClearLogs)
        btnClearLogs.setOnClickListener {
            HyperLog.deleteLogs()
            adapter = MyRecyclerViewAdapter(this@LogsActivity,
                    HyperLog.getDeviceLogsAsStringList(false))
            recyclerView.adapter = adapter
        }
        recyclerView.scrollToPosition(recyclerView.childCount)
    }


    override fun onItemClick(view: View, position: Int) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Log row", adapter!!.getItem(position))
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this,
                "Copied to buffer: " + adapter!!.getItem(position), Toast.LENGTH_SHORT).show()
    }
}