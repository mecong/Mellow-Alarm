package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.util.Strings
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.MediaEntity.Companion.fromCursor
import com.mecong.tenderalarm.sleep_assistant.media_selection.MediaItemViewAdapter.MediaItemViewHolder

class MediaItemViewAdapter internal constructor(private val context: Context, cursor: Cursor?, private val mClickListener: ItemClickListener?, private val showUrl: Boolean) : CursorRecyclerViewAdapter<MediaItemViewHolder?>(context, cursor) {
    private var selectedPosition = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_media_row, parent, false)
        return MediaItemViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: MediaItemViewHolder?, cursor: Cursor?, position: Int) {
        if (viewHolder == null) return
        val myListItem = fromCursor(cursor!!)

        viewHolder.headerText.tag = myListItem.uri
        val header = if (Strings.isEmptyOrWhitespace(myListItem.header)) myListItem.uri else myListItem.header
        viewHolder.headerText.text = header
        if (showUrl) {
            viewHolder.urlText.text = myListItem.uri
            viewHolder.urlText.visibility = View.VISIBLE
        } else {
            viewHolder.urlText.visibility = View.GONE
        }
        viewHolder.itemView.isSelected = selectedPosition == position
    }

    /////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
// parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClick(url: String?, position: Int)
        fun onItemDeleteClick(position: Int)
    }

    // stores and recycles views as they are scrolled off screen
    inner class MediaItemViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var headerText: TextView
        var urlText: TextView
        override fun onClick(view: View) {
            if (adapterPosition == RecyclerView.NO_POSITION) return
            notifyItemChanged(selectedPosition)
            selectedPosition = adapterPosition
            mClickListener?.onItemClick(headerText.tag.toString(), selectedPosition)
            notifyItemChanged(selectedPosition)
        }

        init {
            headerText = itemView.findViewById(R.id.headerText)
            urlText = itemView.findViewById(R.id.urlText)
            val btnDeleteItem = itemView.findViewById<ImageButton>(R.id.btnDeleteItem)
            btnDeleteItem.setOnClickListener(View.OnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) return@OnClickListener
                val popup = PopupMenu(context, btnDeleteItem)
                //Inflating the Popup using xml file
                popup.menuInflater.inflate(R.menu.menu_media_element, popup.menu)
                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener {
                    mClickListener!!.onItemDeleteClick(adapterPosition)
                    true
                }
                popup.show() //showing popup menu
            })
            itemView.setOnClickListener(this)
        }
    }

}