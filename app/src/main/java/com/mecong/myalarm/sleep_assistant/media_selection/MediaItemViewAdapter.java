package com.mecong.myalarm.sleep_assistant.media_selection;


import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.util.Strings;
import com.mecong.myalarm.R;
import com.mecong.myalarm.model.MediaEntity;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
class MediaItemViewAdapter extends CursorRecyclerViewAdapter<MediaItemViewAdapter.ViewHolder> {

    int selectedPosition = 0;
    ItemClickListener mClickListener;


    MediaItemViewAdapter(Context context, Cursor cursor, ItemClickListener clickListener) {
        super(context, cursor);
        this.mClickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_media_row, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor, int position) {
        MediaEntity myListItem = MediaEntity.fromCursor(cursor);
        viewHolder.headerText.setTag(myListItem.getUri());
        if (Strings.isEmptyOrWhitespace(myListItem.getHeader())) {
            viewHolder.headerText.setText(myListItem.getUri());
            viewHolder.urlText.setVisibility(View.INVISIBLE);
        } else {
            viewHolder.headerText.setText(myListItem.getHeader());
            viewHolder.urlText.setText(myListItem.getUri());
            viewHolder.urlText.setVisibility(View.VISIBLE);
        }

        viewHolder.itemView.setSelected(selectedPosition == position);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(String url, int position);

        void onItemDeleteClick(int position);
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView headerText;
        TextView urlText;

        ViewHolder(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
            urlText = itemView.findViewById(R.id.urlText);
            ImageButton btnDeleteItem = itemView.findViewById(R.id.btnDeleteItem);
            btnDeleteItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getAdapterPosition() == RecyclerView.NO_POSITION) return;
                    mClickListener.onItemDeleteClick(getAdapterPosition());
                }
            });

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (getAdapterPosition() == RecyclerView.NO_POSITION) return;
            notifyItemChanged(selectedPosition);

            selectedPosition = getAdapterPosition();
            if (mClickListener != null)
                mClickListener.onItemClick(headerText.getTag().toString(), selectedPosition);
            notifyItemChanged(selectedPosition);
        }
    }
}