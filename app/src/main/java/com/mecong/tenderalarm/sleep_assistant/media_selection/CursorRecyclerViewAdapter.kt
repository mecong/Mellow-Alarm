package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.content.Context
import android.database.Cursor
import android.database.DataSetObserver
import androidx.recyclerview.widget.RecyclerView

abstract class CursorRecyclerViewAdapter<VH : RecyclerView.ViewHolder?> internal constructor(context: Context?, private var mCursor: Cursor?) : RecyclerView.Adapter<VH>() {
    private var mDataValid: Boolean
    private var mRowIdColumn: Int
    private val mDataSetObserver: DataSetObserver?
    override fun getItemCount(): Int {
        return if (mDataValid && !mCursor!!.isClosed) {
            mCursor!!.count
        } else 0
    }

    override fun getItemId(position: Int): Long {
        return if (mDataValid && !mCursor!!.isClosed && mCursor!!.moveToPosition(position)) {
            mCursor!!.getLong(mRowIdColumn)
        } else 0
    }

    override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(true)
    }

    abstract fun onBindViewHolder(viewHolder: VH, cursor: Cursor?, position: Int)
    override fun onBindViewHolder(viewHolder: VH, position: Int) {
        check(mDataValid) { "this should only be called when the cursor is valid" }
        check(mCursor!!.moveToPosition(position)) { "couldn't move cursor to position $position" }
        onBindViewHolder(viewHolder, mCursor, position)
    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     */
    fun changeCursor(cursor: Cursor) {
        val old = swapCursor(cursor)
        old?.close()
    }

    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * [.changeCursor], the returned old Cursor is *not*
     * closed.
     */
    private fun swapCursor(newCursor: Cursor): Cursor? {
        if (newCursor === mCursor) {
            return null
        }
        val oldCursor = mCursor
        if (oldCursor != null && mDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataSetObserver)
        }
        mCursor = newCursor
        if (mCursor != null) {
            if (mDataSetObserver != null) {
                mCursor!!.registerDataSetObserver(mDataSetObserver)
            }
            mRowIdColumn = newCursor.getColumnIndexOrThrow("_id")
            mDataValid = true
            notifyDataSetChanged()
        } else {
            mRowIdColumn = -1
            mDataValid = false
            notifyDataSetChanged()
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
        return oldCursor
    }

    private inner class NotifyingDataSetObserver : DataSetObserver() {
        override fun onChanged() {
            super.onChanged()
            mDataValid = true
            notifyDataSetChanged()
        }

        override fun onInvalidated() {
            super.onInvalidated()
            mDataValid = false
            notifyDataSetChanged()
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
    }

    init {
        mDataValid = mCursor != null && !mCursor!!.isClosed
        mRowIdColumn = if (mDataValid) mCursor!!.getColumnIndex("_id") else -1
        mDataSetObserver = NotifyingDataSetObserver()
        if (mCursor != null) {
            mCursor!!.registerDataSetObserver(mDataSetObserver)
        }
    }
}