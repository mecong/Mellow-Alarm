package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hypertrack.hyperlog.HyperLog
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.alarm.AlarmUtils
import com.mecong.tenderalarm.model.PlaylistEntity
import com.mecong.tenderalarm.model.SQLiteDBHelper
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.Media
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantFragment
import com.mecong.tenderalarm.sleep_assistant.SleepAssistantPlayListModel.SleepAssistantPlayList
import kotlinx.android.synthetic.main.fragment_local_media.*
import java.util.*


private const val READ_REQUEST_CODE = 42

class LocalFilesMediaFragment : Fragment(), FileItemClickListener, PlaylistItemClickListener {

    private var mediaItemViewAdapter: MediaItemViewAdapter? = null
    private var playlistViewAdapter: PlaylistViewAdapter? = null
    private var currentPlaylistID: Long = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_local_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        mediaListView.layoutManager = LinearLayoutManager(view.context)

        val list = sqLiteDBHelper.getAllPlaylists().use {
            generateSequence { if (it.moveToNext()) it else null }
                    .map { PlaylistEntity.fromCursor(it) }
                    .toList()
        }

        playlistViewAdapter = PlaylistViewAdapter(this.context!!, list, this)

        mediaItemViewAdapter =
                MediaItemViewAdapter(this.context!!, emptyList(), this, false)

        setMode(sqLiteDBHelper)

        val function: (v: View) -> Unit = {
            fragmentTitle.text = context?.getString(R.string.local_audio)
            currentPlaylistID = -1
            setMode(sqLiteDBHelper)
        }
        fragmentTitle.setOnClickListener(function)
        backButton.setOnClickListener(function)
    }

    private fun setMode(sqLiteDBHelper: SQLiteDBHelper) {

        mediaListView.adapter = when (currentPlaylistID) {
            -1L -> {
                backButton.visibility = GONE
                playlistViewAdapter?.updateDataSet(sqLiteDBHelper.getAllPlaylists())
                playlistViewAdapter
            }

            else -> {
                backButton.visibility = VISIBLE
                mediaItemViewAdapter?.updateDataSet(sqLiteDBHelper.getLocalMedia(currentPlaylistID))
                mediaItemViewAdapter?.selectedPosition =
                        if (currentPlaylistID == SleepAssistantFragment.playListModel.playlist.value?.playListId) {
                            SleepAssistantFragment.playListModel.playlist.value?.index ?: 0
                        } else {
                            0
                        }
                mediaItemViewAdapter
            }
        }

        buttonAdd.setOnClickListener {
            if (currentPlaylistID == -1L) {
                addPlaylist()
            } else {
                performFileSearch()
            }
        }
    }

    private fun addPlaylist() {
        val dialog = Dialog(context!!, R.style.UrlDialogCustom)
        dialog.setContentView(R.layout.url_input_dialog)
        val textUrl = dialog.findViewById<EditText>(R.id.textUrl)
        textUrl.setText("Playlist ${playlistViewAdapter!!.itemCount + 1}")
        textUrl.hint = "Playlist name"

        val buttonOk = dialog.findViewById<Button>(R.id.buttonOk)
        val buttonOkTop = dialog.findViewById<Button>(R.id.buttonOkTop)
        val buttonCancel = dialog.findViewById<Button>(R.id.buttonCancel)
        val buttonCancelTop = dialog.findViewById<Button>(R.id.buttonCancelTop)

        val okOnclickListener: (v: View) -> Unit = {
            val title = textUrl.text.toString()
            if (title.isNotBlank()) {
                val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
                sqLiteDBHelper.addPlaylist(title)
                dialog.dismiss()
                playlistViewAdapter?.updateDataSet(sqLiteDBHelper.getAllPlaylists())
            } else {
                Toast.makeText(context, "Playlist name should not be empty", Toast.LENGTH_SHORT).show()
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
        lp.gravity = Gravity.CENTER
        dialog.show()
        dialog.window?.attributes = lp
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().

            if (resultData != null) {
                val clipData = resultData.clipData
                if (clipData == null) {
                    val uri = resultData.data
                    HyperLog.i(AlarmUtils.TAG, "Uri: " + uri?.path)
                    context!!.contentResolver.takePersistableUriPermission(uri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addLocalFileMediaRecord(uri.toString(), dumpFileMetaData(uri))
                } else {
                    for (i in 0 until clipData.itemCount) {
                        val path = clipData.getItemAt(i)
                        HyperLog.i(AlarmUtils.TAG, "Uri: " + path.uri.path!! + " i=$i")
                        context!!.contentResolver.takePersistableUriPermission(path.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addLocalFileMediaRecord(path.uri.toString(), dumpFileMetaData(path.uri))
                    }
                }
            }
        }
    }

    private fun addLocalFileMediaRecord(url: String, title: String?) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        sqLiteDBHelper.addLocalMediaUrl(currentPlaylistID, url, title)
        mediaItemViewAdapter?.updateDataSet(sqLiteDBHelper.getLocalMedia(currentPlaylistID))
    }

    private fun dumpFileMetaData(uri: Uri?): String? {
        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        activity?.contentResolver
                ?.query(uri!!, null, null, null, null, null).use { cursor ->
                    // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
                    // "if there's anything to look at, look at it" conditionals.
                    if (cursor != null && cursor.moveToFirst()) {
                        // Note it's called "Display Name".  This is
                        // provider-specific, and might not necessarily be the file name.
                        val displayName = cursor.getString(
                                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        // If the size is unknown, the value stored is null.  But since an
                        // int can't be null in Java, the behavior is implementation-specific,
                        // which is just a fancy term for "unpredictable".  So as
                        // a rule, check if it's null before assigning to an int.  This will
                        // happen often:  The storage API allows for remote files, whose
                        // size might not be locally known.

                        val size: String = if (!cursor.isNull(sizeIndex)) {
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

    private fun performFileSearch() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
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

    override fun onFileItemClick(url: String?, position: Int) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        var media: MutableList<Media>
        sqLiteDBHelper.getLocalMedia(currentPlaylistID).use { allLocalMedia ->
            media = ArrayList(allLocalMedia.count)
            while (allLocalMedia.moveToNext()) {
                val uri = allLocalMedia.getString(allLocalMedia.getColumnIndex("uri"))
                val title = allLocalMedia.getString(allLocalMedia.getColumnIndex("title"))
                media.add(Media(uri, title))
            }
            val newPlayList = SleepAssistantPlayList(position, media, SleepMediaType.LOCAL, currentPlaylistID)
            SleepAssistantFragment.playListModel.playlist.value = newPlayList
        }
    }

    override fun onFileItemDeleteClick(itemId: Int) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        sqLiteDBHelper.deleteLocalMedia(itemId.toString())
        mediaItemViewAdapter?.updateDataSet(sqLiteDBHelper.getLocalMedia(currentPlaylistID))
    }

    override fun onPlaylistItemClick(title: String?, id: Long, position: Int) {
        currentPlaylistID = id
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        setMode(sqLiteDBHelper)
        fragmentTitle.text = title
    }


    override fun onPlaylistItemEditClick(newTitle: String, id: Long, position: Int) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!

        sqLiteDBHelper.renamePlaylist(id, newTitle)
        playlistViewAdapter?.updateDataSet(sqLiteDBHelper.getAllPlaylists())
    }


    override fun onPlaylistDeleteClick(id: Long, position: Int) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!

        sqLiteDBHelper.deletePlaylist(id)
        playlistViewAdapter?.updateDataSet(sqLiteDBHelper.getAllPlaylists())
    }
}