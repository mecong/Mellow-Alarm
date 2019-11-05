package com.mecong.tenderalarm.sleep_assistant.media_selection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mecong.tenderalarm.R;

import java.util.List;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoisesItemViewAdapter extends RecyclerView.Adapter<NoisesItemViewAdapter.ViewHolder> {

    int selectedPosition;
    List<SleepNoise> mData;
    LayoutInflater mInflater;
    NoisesItemClickListener mClickListener;

    // data is passed into the constructor
    NoisesItemViewAdapter(Context context, List<SleepNoise> data, int selectedPosition) {
        this.selectedPosition = selectedPosition;
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        if (this.selectedPosition > this.mData.size()) this.selectedPosition = 0;
    }

    // inflates the row layout from xml when needed
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.fragment_noises_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SleepNoise item = mData.get(position);
        holder.headerText.setText(item.getName());
//        holder.urlText.setText(item.getUrl());

        holder.itemView.setSelected(selectedPosition == position);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // convenience method for getting data at click position
    SleepNoise getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(NoisesItemClickListener noisesItemClickListener) {
        this.mClickListener = noisesItemClickListener;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    // parent activity will implement this method to respond to click events
    public interface NoisesItemClickListener {
        void onItemClick(View view, int position);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView headerText;
//        TextView urlText;

        ViewHolder(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
//            urlText = itemView.findViewById(R.id.urlText);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (getAdapterPosition() == RecyclerView.NO_POSITION) return;
            notifyItemChanged(selectedPosition);

            selectedPosition = getAdapterPosition();
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
            notifyItemChanged(selectedPosition);
        }
    }
}