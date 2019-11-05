package com.mecong.tenderalarm.sleep_assistant.media_selection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantViewModel;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;


@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class NoisesFragment extends Fragment implements NoisesItemViewAdapter.NoisesItemClickListener {
    private static final String SELECTED_POSITION = "selectedPosition";
    int selectedPosition;
    NoisesItemViewAdapter adapter;
    SleepAssistantViewModel model;

    private NoisesFragment(SleepAssistantViewModel model) {
        this.model = model;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param selectedPosition - selected item in the list
     * @return A new instance of fragment NoisesFragment.
     */
    static NoisesFragment newInstance(int selectedPosition, SleepAssistantViewModel model) {
        NoisesFragment fragment = new NoisesFragment(model);
        Bundle args = new Bundle();
        args.putInt(SELECTED_POSITION, selectedPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            selectedPosition = getArguments().getInt(SELECTED_POSITION);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_noises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        final RecyclerView recyclerView = view.findViewById(R.id.noisesList);

        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        adapter = new NoisesItemViewAdapter(view.getContext(),
                SleepNoise.Companion.retrieveNoises(), selectedPosition);
        adapter.setClickListener(this);

        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(View view, int position) {
        SleepAssistantViewModel.PlayList newPlayList = new SleepAssistantViewModel.PlayList(
                adapter.getItem(position).getUrl(), adapter.getItem(position).getName(), SleepMediaType.NOISE);
        model.setPlaylist(newPlayList);
    }
}

