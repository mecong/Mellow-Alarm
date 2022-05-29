package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.sleep_assistant.Media

interface NoisesItemClickListener {
  fun onItemClick(view: View?, position: Int)
}

class NoisesItemViewAdapter constructor(context: Context?, private val mData: List<Media>, var selectedPosition: Int) :
  RecyclerView.Adapter<NoisesItemViewAdapter.ViewHolder>() {

  private val mInflater: LayoutInflater = LayoutInflater.from(context)
  private var mClickListener: NoisesItemClickListener? = null

  // inflates the row layout from xml when needed
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = mInflater.inflate(R.layout.fragment_noises_row, parent, false)
    return ViewHolder(view)
  }

  // binds the data to the TextView in each row
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val item = mData[position]
    holder.headerText.text = item.title
    holder.itemView.isSelected = selectedPosition == position
  }

  // total number of rows
  override fun getItemCount(): Int {
    return mData.size
  }

  // convenience method for getting data at click position
  fun getItem(id: Int): Media {
    return mData[id]
  }

  // allows clicks events to be caught
  fun setClickListener(noisesItemClickListener: NoisesItemClickListener?) {
    mClickListener = noisesItemClickListener
  }

  /////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////
// parent activity will implement this method to respond to click events


  // stores and recycles views as they are scrolled off screen
  inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
    View.OnClickListener {

    var headerText: TextView = itemView.findViewById(R.id.headerText)

    override fun onClick(view: View) {
      if (absoluteAdapterPosition == RecyclerView.NO_POSITION) return
      notifyItemChanged(selectedPosition)
      selectedPosition = absoluteAdapterPosition
      mClickListener?.onItemClick(view, absoluteAdapterPosition)
      notifyItemChanged(selectedPosition)
    }

    init {
      itemView.setOnClickListener(this)
    }
  }

  // data is passed into the constructor
  init {
    if (selectedPosition > mData.size) selectedPosition = 0
  }
}