package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.content.Context
import android.database.Cursor
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.util.Strings
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.MediaEntity
import com.mecong.tenderalarm.sleep_assistant.media_selection.MediaItemViewAdapter.MediaItemViewHolder

// parent activity will implement this method to respond to click events
interface FileItemClickListener {
    fun onFileItemClick(url: String?, position: Int)
    fun onFileItemDeleteClick(itemId: Int)
}

class MediaItemViewAdapter
internal constructor(
        private val context: Context,
        private var list: List<MediaEntity>,
        private val mClickListenerFile: FileItemClickListener?,
        private val showUrl: Boolean) : RecyclerView.Adapter<MediaItemViewHolder?>() {

    var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_media_row, parent, false)
        return MediaItemViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: MediaItemViewHolder, position: Int) {
        val myListItem = list[position]

        viewHolder.headerText.tag = myListItem.uri
        viewHolder.headerText.text = if (Strings.isEmptyOrWhitespace(myListItem.header)) myListItem.uri else myListItem.header
        if (showUrl) {
            viewHolder.urlText.text = myListItem.uri
            viewHolder.urlText.visibility = View.VISIBLE
        } else {
            viewHolder.urlText.visibility = View.GONE
        }
        viewHolder.itemView.isSelected = selectedPosition == position
    }

    fun updateDataSet(cursor: Cursor) {
        cursor.use {
            list = generateSequence { if (cursor.moveToNext()) cursor else null }
                    .map { MediaEntity.fromCursor(it) }
                    .toList()
        }

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = list.size

    // stores and recycles views as they are scrolled off screen
    inner class MediaItemViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var headerText: TextView = itemView.findViewById(R.id.headerText)
        var urlText: TextView = itemView.findViewById(R.id.urlText)

        override fun onClick(view: View) {
            if (adapterPosition == RecyclerView.NO_POSITION) return
            notifyItemChanged(selectedPosition)
            selectedPosition = adapterPosition

            mClickListenerFile?.onFileItemClick(headerText.tag.toString(), selectedPosition)
            notifyItemChanged(selectedPosition)
        }

        init {
            val btnDeleteItem = itemView.findViewById<ImageButton>(R.id.btnDeleteItem)
            btnDeleteItem.setOnClickListener(View.OnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) return@OnClickListener

                val wrapper = ContextThemeWrapper(context, R.style.MyPopupMenu)
                val popup = PopupMenu(wrapper, btnDeleteItem)
                popup.menuInflater.inflate(R.menu.menu_media_element, popup.menu)
                popup.setOnMenuItemClickListener {
                    mClickListenerFile!!.onFileItemDeleteClick(list[adapterPosition].id)
                    true
                }
                popup.show() //showing popup menu
            })
            itemView.setOnClickListener(this)
        }
    }

}