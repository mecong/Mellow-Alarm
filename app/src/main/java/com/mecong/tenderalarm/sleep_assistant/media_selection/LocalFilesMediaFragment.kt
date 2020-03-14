package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.Media
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListModel
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListModel.PlayList
import java.util.*

class LocalFilesMediaFragment internal constructor(private val model: SleepAssistantPlayListModel) : Fragment(), MediaItemViewAdapter.ItemClickListener {
    private var adapter: MediaItemViewAdapter? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? { // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_local_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView: RecyclerView = view.findViewById(R.id.mediaListView)
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        adapter = MediaItemViewAdapter(view.context,
                sqLiteDBHelper!!.allLocalMedia, this, false)
        recyclerView.adapter = adapter
        val buttonAdd = view.findViewById<Button>(R.id.buttonAdd)
        buttonAdd.setOnClickListener { performFileSearch() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
// READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
// response to some other intent, and the code below shouldn't run at all.
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) { // The document selected by the user won't be returned in the intent.
// Instead, a URI to that document will be contained in the return intent
// provided to this method as a parameter.
// Pull that URI using resultData.getData().
            val uri: Uri?
            if (resultData != null) {
                val clipData = resultData.clipData
                if (clipData == null) {
                    uri = resultData.data
                    HyperLog.i(AlarmUtils.TAG, "Uri: " + uri.toString())
                    addLocalFileMediaRecord(uri.toString(), dumpFileMetaData(uri))
                    context!!.contentResolver.takePersistableUriPermission(uri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    for (i in 0 until clipData.itemCount) {
                        val path = clipData.getItemAt(i)
                        addLocalFileMediaRecord(path.uri.toString(), dumpFileMetaData(path.uri))
                        context!!.contentResolver.takePersistableUriPermission(path.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        HyperLog.i("Path:", path.toString())
                    }
                }
            }
        }
    }

    private fun addLocalFileMediaRecord(url: String, title: String?) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)
        sqLiteDBHelper!!.addLocalMediaUrl(url, title)
        adapter!!.changeCursor(sqLiteDBHelper.allLocalMedia)
    }

    private fun dumpFileMetaData(uri: Uri?): String? { // The query, since it only applies to a single document, will only return
// one row. There's no need to filter, sort, or select fields, since we want
// all fields for one document.
        activity!!.contentResolver
                .query(uri!!, null, null, null, null, null).use { cursor ->
                    // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
// "if there's anything to look at, look at it" conditionals.
                    if (cursor != null && cursor.moveToFirst()) { // Note it's called "Display Name".  This is
// provider-specific, and might not necessarily be the file name.
                        val displayName = cursor.getString(
                                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        HyperLog.i(AlarmUtils.TAG, "Display Name: $displayName")
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        // If the size is unknown, the value stored is null.  But since an
// int can't be null in Java, the behavior is implementation-specific,
// which is just a fancy term for "unpredictable".  So as
// a rule, check if it's null before assigning to an int.  This will
// happen often:  The storage API allows for remote files, whose
// size might not be locally known.
                        val size: String
                        size = if (!cursor.isNull(sizeIndex)) { // Technically the column stores an int, but cursor.getString()
// will do the conversion automatically.
                            cursor.getString(sizeIndex)
                        } else {
                            "Unknown"
                        }
                        HyperLog.i(AlarmUtils.TAG, "Size: $size")
                        return displayName
                    }
                }
        return null
    }

    private fun performFileSearch() { // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
// browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        // Filter to only show results that can be "opened", such as a
// file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        // Filter to show only images, using the image MIME data type.
// If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
// To search for all documents available via installed storage providers,
// it would be "*/*".
//        intent.setType("audio/mpeg audio/aac audio/wav");
        intent.type = "audio/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onItemClick(url: String?, position: Int) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)
        var media: MutableList<Media>
        sqLiteDBHelper!!.allLocalMedia.use { allLocalMedia ->
            media = ArrayList(allLocalMedia.count)
            while (allLocalMedia.moveToNext()) {
                val uri = allLocalMedia.getString(allLocalMedia.getColumnIndex("uri"))
                val title = allLocalMedia.getString(allLocalMedia.getColumnIndex("title"))
                media.add(Media(uri, title))
            }
            val newPlayList = PlayList(position, media, SleepMediaType.LOCAL)
            model.playlist.value = newPlayList
        }
    }

    override fun onItemDeleteClick(position: Int) {
        val itemId = adapter!!.getItemId(position)
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)
        sqLiteDBHelper!!.deleteLocalMedia(itemId.toString())
        adapter!!.changeCursor(sqLiteDBHelper.allLocalMedia)
    }

    companion object {
        private const val READ_REQUEST_CODE = 42
    }

}