package com.mecong.tenderalarm.logs;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hypertrack.hyperlog.HyperLog;
import com.mecong.tenderalarm.R;

public class LogsActivity extends AppCompatActivity implements MyRecyclerViewAdapter.ItemClickListener {
    MyRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);


        final RecyclerView recyclerView = findViewById(R.id.logsList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRecyclerViewAdapter(this, HyperLog.getDeviceLogsAsStringList(false));
        adapter.setClickListener(this);

        recyclerView.setAdapter(adapter);

        Button btnClearLogs = findViewById(R.id.btnClearLogs);
        btnClearLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HyperLog.deleteLogs();
                adapter = new MyRecyclerViewAdapter(LogsActivity.this,
                        HyperLog.getDeviceLogsAsStringList(false));
                recyclerView.setAdapter(adapter);
            }
        });

        recyclerView.scrollToPosition(recyclerView.getChildCount());
    }

    @Override
    public void onItemClick(View view, int position) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Log row", adapter.getItem(position));
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this,
                "Copied to buffer: " + adapter.getItem(position), Toast.LENGTH_SHORT).show();
    }
}


