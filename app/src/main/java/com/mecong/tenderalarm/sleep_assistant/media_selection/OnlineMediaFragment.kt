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
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantFragment
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListModel.SleepAssistantPlayList
import java.net.MalformedURLException
import java.net.URL

class OnlineMediaFragment internal constructor() : Fragment(), MediaItemViewAdapter.FileItemClickListener {
    private var mediaItemViewAdapter: MediaItemViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? { // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_online_media, container, false)
    }

    private fun addUrl(url: String) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        sqLiteDBHelper.addMediaUrl(url)
        mediaItemViewAdapter!!.changeCursor(sqLiteDBHelper.allOnlineMedia)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mediaListView: RecyclerView = view.findViewById(R.id.mediaListView)
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)
        mediaListView.layoutManager = LinearLayoutManager(view.context)
        mediaItemViewAdapter = MediaItemViewAdapter(view.context,
                sqLiteDBHelper!!.allOnlineMedia, this, true)
        mediaListView.adapter = mediaItemViewAdapter
        val buttonAdd = view.findViewById<Button>(R.id.buttonAdd)
        buttonAdd.setOnClickListener {
            val dialog = Dialog(context!!, R.style.UrlDialogCustom)
            dialog.setContentView(R.layout.url_input_dialog)
            val textUrl = dialog.findViewById<EditText>(R.id.textUrl)
            val buttonOk = dialog.findViewById<Button>(R.id.buttonOk)
            buttonOk.setOnClickListener {
                try {
                    URL(textUrl.text.toString())
                    addUrl(textUrl.text.toString())
                    dialog.dismiss()
                } catch (mue: MalformedURLException) {
                    Toast.makeText(context, "Url is not valid", Toast.LENGTH_SHORT).show()
                }
            }
            val buttonCancel = dialog.findViewById<Button>(R.id.buttonCancel)
            buttonCancel.setOnClickListener { dialog.dismiss() }
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            dialog.show()
            dialog.window!!.attributes = lp
        }
    }

    override fun onFileItemClick(url: String?, position: Int) {
        SleepAssistantFragment.playListModel.playlist.value = SleepAssistantPlayList(url, url, SleepMediaType.ONLINE)
    }

    override fun onFileItemDeleteClick(position: Int) {
        val itemId = mediaItemViewAdapter!!.getItemId(position)
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        sqLiteDBHelper.deleteOnlineMedia(itemId.toString())
        mediaItemViewAdapter!!.changeCursor(sqLiteDBHelper.allOnlineMedia)
    }

}