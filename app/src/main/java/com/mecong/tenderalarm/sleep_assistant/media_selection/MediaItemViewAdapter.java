package com.mecong.tenderalarm.sleep_assistant.media_selection;


import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.util.Strings;
import com.mecong.tenderalarm.R;
import com.mecong.tenderalarm.model.MediaEntity;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaItemViewAdapter extends CursorRecyclerViewAdapter<MediaItemViewAdapter.MediaItemViewHolder> {

    int selectedPosition = 0;
    ItemClickListener mClickListener;
    boolean showUrl;
    Context context;


    MediaItemViewAdapter(Context context, Cursor cursor, ItemClickListener clickListener, boolean showUrl) {
        super(context, cursor);
        this.context = context;
        this.mClickListener = clickListener;
        this.showUrl = showUrl;
    }

    @NonNull
    @Override
    public MediaItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_media_row, parent, false);
        return new MediaItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MediaItemViewHolder mediaItemViewHolder, Cursor cursor, int position) {
        MediaEntity myListItem = MediaEntity.fromCursor(cursor);
        mediaItemViewHolder.headerText.setTag(myListItem.getUri());

        String header = Strings.isEmptyOrWhitespace(myListItem.getHeader()) ?
                myListItem.getUri() : myListItem.getHeader();
        mediaItemViewHolder.headerText.setText(header);
        if (showUrl) {
            mediaItemViewHolder.urlText.setText(myListItem.getUri());
            mediaItemViewHolder.urlText.setVisibility(View.VISIBLE);
        } else {
            mediaItemViewHolder.urlText.setVisibility(View.GONE);
        }


        mediaItemViewHolder.itemView.setSelected(selectedPosition == position);
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
    public class MediaItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView headerText;
        TextView urlText;

        MediaItemViewHolder(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
            urlText = itemView.findViewById(R.id.urlText);
            final ImageButton btnDeleteItem = itemView.findViewById(R.id.btnDeleteItem);
            btnDeleteItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getAdapterPosition() == RecyclerView.NO_POSITION) return;

                    PopupMenu popup = new PopupMenu(context, btnDeleteItem);
                    //Inflating the Popup using xml file
                    popup.getMenuInflater().inflate(R.menu.menu_media_element, popup.getMenu());

                    //registering popup with OnMenuItemClickListener
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            mClickListener.onItemDeleteClick(getAdapterPosition());
                            return true;
                        }
                    });

                    popup.show();//showing popup menu
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