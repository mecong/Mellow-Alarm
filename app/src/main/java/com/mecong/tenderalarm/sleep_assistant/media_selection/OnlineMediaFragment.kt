package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.MediaEntity
import com.mecong.tenderalarm.model.PropertyName
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.Media
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListActive
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListIdle
import org.greenrobot.eventbus.EventBus
import java.net.MalformedURLException
import java.net.URL

class OnlineMediaFragment : Fragment(), FileItemClickListener {
    private var mediaItemViewAdapter: MediaItemViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? { // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_online_media, container, false)
    }

    private fun addUrl(title: String, url: String) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        sqLiteDBHelper.addMediaUrl(title, url)
        mediaItemViewAdapter!!.updateDataSet(sqLiteDBHelper.allOnlineMedia)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mediaListView: RecyclerView = view.findViewById(R.id.mediaListView)
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        mediaListView.layoutManager = LinearLayoutManager(view.context)

        val list = sqLiteDBHelper.allOnlineMedia.use {
            generateSequence { if (it.moveToNext()) it else null }
                    .map { MediaEntity.fromCursor(it) }
                    .toList()
        }

        val savedActiveTab = sqLiteDBHelper.getPropertyInt(PropertyName.ACTIVE_TAB)

        var selectedPosition = -1
        if (savedActiveTab == 1) {
            selectedPosition = sqLiteDBHelper.getPropertyInt(PropertyName.TRACK_NUMBER) ?: 0
            initPlaylist(selectedPosition, false)
        }

        mediaItemViewAdapter = MediaItemViewAdapter(view.context, selectedPosition,
                list, this, true)

        mediaListView.scrollToPosition(selectedPosition)

        mediaListView.adapter = mediaItemViewAdapter

        val buttonAdd = view.findViewById<Button>(R.id.buttonAdd)
        buttonAdd.setOnClickListener {
            val dialog = Dialog(context!!, R.style.UrlDialogCustom)
            dialog.setContentView(R.layout.url_input_dialog)
            val textUrl = dialog.findViewById<EditText>(R.id.textUrl)
            val titleUrl = dialog.findViewById<EditText>(R.id.textTitle)
            val buttonOk = dialog.findViewById<Button>(R.id.buttonOk)
            val buttonOkTop = dialog.findViewById<Button>(R.id.buttonOkTop)
            val addUrlListener: (v: View) -> Unit = {
                try {
                    addUrl(titleUrl.text.toString(), URL(textUrl.text.toString()).toString())
                    dialog.dismiss()
                } catch (mue: MalformedURLException) {
                    Toast.makeText(context, context!!.getString(R.string.url_invalid_warning), Toast.LENGTH_SHORT).show()
                }
            }
            buttonOk.setOnClickListener(addUrlListener)
            buttonOkTop.setOnClickListener(addUrlListener)
            val buttonCancel = dialog.findViewById<Button>(R.id.buttonCancel)
            val buttonCancelTop = dialog.findViewById<Button>(R.id.buttonCancelTop)
            buttonCancel.setOnClickListener { dialog.dismiss() }
            buttonCancelTop.setOnClickListener { dialog.dismiss() }
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            dialog.show()
            dialog.window!!.attributes = lp
        }
    }

    override fun onFileItemClick(url: String?, position: Int) {
        initPlaylist(position, true)
    }

    private fun initPlaylist(position: Int, active: Boolean) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!

        val list = sqLiteDBHelper.allOnlineMedia.use {
            generateSequence { if (it.moveToNext()) it else null }
                    .map { MediaEntity.fromCursor(it) }
                    .toList()
        }

        val media = list.map { Media(it.uri, it.header) }.toList()

        val newPlayList = if (active)
            SleepAssistantPlayListActive(position, media, SleepMediaType.ONLINE, -1)
        else
            SleepAssistantPlayListIdle(position, media, SleepMediaType.ONLINE, -1)

        EventBus.getDefault().postSticky(newPlayList)

    }

    override fun onFileItemDeleteClick(itemId: Int) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        sqLiteDBHelper.deleteOnlineMedia(itemId.toString())
        mediaItemViewAdapter!!.updateDataSet(sqLiteDBHelper.allOnlineMedia)
    }

}