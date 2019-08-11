package com.mecong.myalarm.sleep_assistant.media_selection;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mecong.myalarm.R;
import com.mecong.myalarm.model.SQLiteDBHelper;
import com.mecong.myalarm.sleep_assistant.SleepAssistantViewModel;

import java.net.MalformedURLException;
import java.net.URL;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor //required
public class OnlineMediaFragment extends Fragment implements MediaItemViewAdapter.ItemClickListener {

    MediaItemViewAdapter adapter;
    SleepAssistantViewModel model;


    OnlineMediaFragment(SleepAssistantViewModel model) {
        this.model = model;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_online_media, container, false);
    }

    private void addUrl(String url) {
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(this.getContext());
        sqLiteDBHelper.addMediaUrl(url);
        adapter.changeCursor(sqLiteDBHelper.getAllOnlineMedia());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final RecyclerView recyclerView = view.findViewById(R.id.mediaListView);
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(this.getContext());

        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        adapter = new MediaItemViewAdapter(view.getContext(),
                sqLiteDBHelper.getAllOnlineMedia(), this, true);

        recyclerView.setAdapter(adapter);

        final Button buttonAdd = view.findViewById(R.id.buttonAdd);
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.url_input_dialog);

                final EditText textUrl = dialog.findViewById(R.id.textUrl);
                Button buttonOk = dialog.findViewById(R.id.buttonOk);
                buttonOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            new URL(textUrl.getText().toString());

                            addUrl(textUrl.getText().toString());
                            dialog.dismiss();
                        } catch (MalformedURLException mue) {
                            Toast.makeText(getContext(), "Url is not valid", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                final Button buttonCancel = dialog.findViewById(R.id.buttonCancel);
                buttonCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                dialog.show();
                dialog.getWindow().setAttributes(lp);
            }
        });
    }


    @Override
    public void onItemClick(String url, int position) {
        SleepAssistantViewModel.PlayList newPlayList =
                new SleepAssistantViewModel.PlayList(url, url, SleepMediaType.ONLINE);
        model.setPlaylist(newPlayList);
    }

    @Override
    public void onItemDeleteClick(int position) {
        long itemId = adapter.getItemId(position);
        SQLiteDBHelper sqLiteDBHelper = SQLiteDBHelper.getInstance(this.getContext());
        sqLiteDBHelper.deleteOnlineMedia(String.valueOf(itemId));
        adapter.changeCursor(sqLiteDBHelper.getAllOnlineMedia());
    }
}

