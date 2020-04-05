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
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.PlaylistEntity.Companion.fromCursor

class PlaylistViewAdapter
internal constructor(
        private val context: Context,
        cursor: Cursor?,
        private val mClickListenerPlaylist: PlaylistItemClickListener?)
    : CursorRecyclerViewAdapter<PlaylistViewAdapter.PlaylistViewHolder?>(context, cursor) {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_media_row, parent, false)
        return PlaylistViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: PlaylistViewHolder?, cursor: Cursor?, position: Int) {
        if (viewHolder == null) return
        val myListItem = fromCursor(cursor!!)

        viewHolder.title.text = "[${myListItem.title}]"
        viewHolder.title.tag = myListItem.title.toString()
        viewHolder.itemView.isSelected = selectedPosition == position
        viewHolder.urlText.visibility = View.GONE
        viewHolder.id = myListItem.id
    }

    /////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
// parent activity will implement this method to respond to click events
    interface PlaylistItemClickListener {
        fun onPlaylistItemClick(title: String?, id: Long, position: Int)
        fun onPlaylistDeleteClick(id: Long, position: Int)
    }

    // stores and recycles views as they are scrolled off screen
    inner class PlaylistViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var title: TextView = itemView.findViewById(R.id.headerText)
        var urlText: TextView = itemView.findViewById(R.id.urlText)
        var id: Long = -1L

        override fun onClick(view: View) {
            if (adapterPosition == RecyclerView.NO_POSITION) return
            notifyItemChanged(selectedPosition)
            selectedPosition = adapterPosition
            mClickListenerPlaylist?.onPlaylistItemClick(title.tag.toString(), id, selectedPosition)
            notifyItemChanged(selectedPosition)
        }

        init {
            val btnDeleteItem = itemView.findViewById<ImageButton>(R.id.btnDeleteItem)
            btnDeleteItem.setOnClickListener(View.OnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) return@OnClickListener
                val popup = PopupMenu(context, btnDeleteItem)
                //Inflating the Popup using xml file
                popup.menuInflater.inflate(R.menu.menu_media_element, popup.menu)
                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener {
                    mClickListenerPlaylist!!.onPlaylistDeleteClick(id, adapterPosition)
                    true
                }
                popup.show() //showing popup menu
            })
            itemView.setOnClickListener(this)
        }
    }

}