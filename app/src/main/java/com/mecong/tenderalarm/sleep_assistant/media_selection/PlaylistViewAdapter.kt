package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.app.Dialog
import android.content.Context
import android.database.Cursor
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.PlaylistEntity
import com.mecong.tenderalarm.model.PlaylistEntity.Companion.fromCursor

// parent activity will implement this method to respond to click events
interface PlaylistItemClickListener {
    fun onPlaylistItemClick(title: String?, id: Long, position: Int)
    fun onPlaylistItemEditClick(newTitle: String, id: Long, position: Int)
    fun onPlaylistDeleteClick(id: Long, position: Int)
}

class PlaylistViewAdapter constructor(
        private val context: Context,
        private var list: List<PlaylistEntity>,
        private val mClickListenerPlaylist: PlaylistItemClickListener)
    : RecyclerView.Adapter<PlaylistViewAdapter.PlaylistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_playlist_row, parent, false)
        return PlaylistViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: PlaylistViewHolder, position: Int) {
        val myListItem = list[position]

        viewHolder.title.text = context.getString(R.string.playlist_title, myListItem.title)
        viewHolder.title.tag = myListItem.title
        viewHolder.id = myListItem.id
    }

    fun updateDataSet(cursor: Cursor) {
        cursor.use {
            list = generateSequence { if (cursor.moveToNext()) cursor else null }
                    .map { fromCursor(it) }
                    .toList()
        }

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = list.size


    // stores and recycles views as they are scrolled off screen
    inner class PlaylistViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var title: TextView = itemView.findViewById(R.id.headerText)
        var id: Long = -1L

        override fun onClick(view: View) {
            if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return
            notifyItemChanged(absoluteAdapterPosition)
            mClickListenerPlaylist.onPlaylistItemClick(title.tag.toString(), id, absoluteAdapterPosition)
            notifyItemChanged(absoluteAdapterPosition)
        }

        init {
            val btnDeleteItem = itemView.findViewById<ImageButton>(R.id.btnDeleteItem)
            val btnEditItem = itemView.findViewById<ImageButton>(R.id.btnEditItem)

            btnDeleteItem.setOnClickListener(View.OnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@OnClickListener

                val wrapper = ContextThemeWrapper(context, R.style.MyPopupMenu)
                val popup = PopupMenu(wrapper, btnDeleteItem)
                popup.menuInflater.inflate(R.menu.menu_media_element, popup.menu)
                popup.setOnMenuItemClickListener {
                    mClickListenerPlaylist.onPlaylistDeleteClick(id, absoluteAdapterPosition)
                    true
                }
                popup.show() //showing popup menu
            })

            btnEditItem.setOnClickListener {
                if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val dialog = Dialog(context, R.style.UrlDialogCustom)
                dialog.setContentView(R.layout.url_input_dialog)
                val textUrl = dialog.findViewById<EditText>(R.id.textUrl)
                textUrl.setText(title.tag.toString())
                textUrl.hint = "Playlist name"

                val textTitle = dialog.findViewById<EditText>(R.id.textTitle)
                textTitle.visibility = View.GONE

                val buttonOk = dialog.findViewById<Button>(R.id.buttonOk)
                val buttonOkTop = dialog.findViewById<Button>(R.id.buttonOkTop)
                val buttonCancel = dialog.findViewById<Button>(R.id.buttonCancel)
                val buttonCancelTop = dialog.findViewById<Button>(R.id.buttonCancelTop)

                val okOnclickListener: (v: View) -> Unit = {
                    val title = textUrl.text.toString()
                    if (title.isNotBlank()) {
                        mClickListenerPlaylist.onPlaylistItemEditClick(title, id, absoluteAdapterPosition)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(context, context.getString(R.string.empty_playlist_name_warning), Toast.LENGTH_SHORT).show()
                    }
                }
                buttonOk.setOnClickListener(okOnclickListener)
                buttonOkTop.setOnClickListener(okOnclickListener)

                buttonCancel.setOnClickListener { dialog.dismiss() }
                buttonCancelTop.setOnClickListener { dialog.dismiss() }

                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.width = WindowManager.LayoutParams.MATCH_PARENT
                lp.horizontalMargin = 1000.0f
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                dialog.show()
                dialog.window!!.attributes = lp
            }

            itemView.setOnClickListener(this)
        }
    }


}