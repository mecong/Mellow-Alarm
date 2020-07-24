package com.mecong.tenderalarm.sleep_assistant.media_selection

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.PlaylistEntity
import com.mecong.tenderalarm.model.PropertyName.*
import com.mecong.tenderalarm.model.SQLiteDBHelper
import com.mecong.tenderalarm.model.SQLiteDBHelper.Companion.sqLiteDBHelper
import com.mecong.tenderalarm.sleep_assistant.*
import com.zaphlabs.filechooser.KnotFileChooser
import com.zaphlabs.filechooser.Sorter
import com.zaphlabs.filechooser.filters.ExtensionFilter
import kotlinx.android.synthetic.main.fragment_local_media.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.io.File
import java.util.*


private const val READ_REQUEST_CODE = 42

class LocalFilesMediaFragment : Fragment(), FileItemClickListener, PlaylistItemClickListener {

    private var mediaItemViewAdapter: MediaItemViewAdapter? = null
    private var playlistViewAdapter: PlaylistViewAdapter? = null
    private var currentPlaylistID: Long = -1
    private var currentPlaylistTitle: String? = null
    private var myState: Bundle? = null
    private var buttonAddLastClickTime: Long = 0 // a hack to avoid double click on a button
    private lateinit var radioService: RadioService
    private var radioServiceBound = false


    private var serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
            radioService = (binder as RadioService.LocalBinder).service
            radioServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            radioServiceBound = false
        }
    }

    private fun bindRadioService() {
        val intent = Intent(this.context, RadioService::class.java)
        this.activity!!.application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        return inflater.inflate(R.layout.fragment_local_media, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindRadioService()

        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!

        val listOfPlaylists = sqLiteDBHelper.getAllPlaylists().use {
            generateSequence { if (it.moveToNext()) it else null }
                    .map { PlaylistEntity.fromCursor(it) }
                    .toList()
        }

        playlistViewAdapter = PlaylistViewAdapter(this.context!!, listOfPlaylists, this)

        mediaItemViewAdapter =
                MediaItemViewAdapter(this.context!!, 0, emptyList(), this, false)

        val savedActiveTab = sqLiteDBHelper.getPropertyInt(ACTIVE_TAB)

        val myStatePlaylistID: Long = myState?.getLong(PLAYLIST_ID_KEY, -1) ?: -1
        currentPlaylistID = sqLiteDBHelper.getPropertyLong(PLAYLIST_ID) ?: myStatePlaylistID


        val selectedPosition: Int
        if (savedActiveTab == 0 && myState == null) {
            selectedPosition = sqLiteDBHelper.getPropertyInt(TRACK_NUMBER) ?: 0
            initPlaylist(selectedPosition, false)
        }

        currentPlaylistTitle = sqLiteDBHelper.getPropertyString(PLAYLIST_TITLE)
                ?: context?.getString(R.string.local_audio)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        mediaListView.layoutManager = LinearLayoutManager(this.context!!)

        setMode(sqLiteDBHelper)

        val goBack: (v: View) -> Unit = {
            currentPlaylistTitle = context?.getString(R.string.local_audio)
            currentPlaylistID = -1
            setMode(sqLiteDBHelper)
        }

        fragmentTitle.setOnClickListener(goBack)
        backButton.setOnClickListener(goBack)

        val savedShuffle = sqLiteDBHelper.getPropertyString(SHUFFLE)?.toBoolean() ?: false
        ibPlayOrder.setImageResource(
                if (savedShuffle) {
                    R.drawable.ic_baseline_shuffle_24
                } else {
                    R.drawable.ic_baseline_trending_flat_24
                }
        )

        ibPlayOrder.setOnClickListener {
            radioService.shuffleModeEnabled = !radioService.shuffleModeEnabled
            val shuffleModeEnabled = radioService.shuffleModeEnabled

            ibPlayOrder.setImageResource(
                    if (shuffleModeEnabled) {
                        Toast.makeText(this.context, R.string.shuffle_mode_on, LENGTH_SHORT).show()
                        sqLiteDBHelper.setPropertyString(SHUFFLE, "true")
                        R.drawable.ic_baseline_shuffle_24
                    } else {
                        Toast.makeText(this.context, R.string.shuffle_mode_off, LENGTH_SHORT).show()
                        sqLiteDBHelper.setPropertyString(SHUFFLE, "false")
                        R.drawable.ic_baseline_trending_flat_24
                    }
            )
        }
    }


    @Subscribe
    fun onPlayFileChanged(playList: SleepAssistantPlayList) {
        if (currentPlaylistID == playList.playListId) {
            mediaListView?.scrollToPosition(playList.index)
            mediaItemViewAdapter?.selectedPosition = playList.index
            mediaItemViewAdapter?.notifyDataSetChanged()
        }
    }

    private fun setMode(sqLiteDBHelper: SQLiteDBHelper) {
        var trackPosition = -1
        mediaListView.adapter = when (currentPlaylistID) {
            -1L -> {
                buttonAdd.setOnClickListener { addPlaylist() }
                backButton.visibility = GONE
                playlistViewAdapter?.updateDataSet(sqLiteDBHelper.getAllPlaylists())
                playlistViewAdapter
            }

            else -> {
                buttonAdd.setOnClickListener { performNewFileSearch() }

                backButton.visibility = VISIBLE
                val savedActiveTab = sqLiteDBHelper.getPropertyInt(ACTIVE_TAB)
                val savedPlayListId: Long = (sqLiteDBHelper.getPropertyInt(PLAYLIST_ID) ?: -1).toLong()

                if (savedActiveTab == 0 && savedPlayListId == currentPlaylistID) {
                    trackPosition = sqLiteDBHelper.getPropertyInt(TRACK_NUMBER) ?: 0
                }

                mediaItemViewAdapter?.updateDataSet(sqLiteDBHelper.getLocalMedia(currentPlaylistID))
                mediaItemViewAdapter?.selectedPosition = trackPosition
                mediaItemViewAdapter
            }
        }

        fragmentTitle.text = currentPlaylistTitle
        mediaListView.scrollToPosition(trackPosition)
    }

    private fun addPlaylist() {
        val dialog = Dialog(context!!, R.style.UrlDialogCustom)
        dialog.setContentView(R.layout.url_input_dialog)
        val textUrl = dialog.findViewById<EditText>(R.id.textUrl)
        val textTitle = dialog.findViewById<EditText>(R.id.textTitle)
        textTitle.visibility = GONE
        textUrl.setText(context!!.getString(R.string.playlist_def_name, playlistViewAdapter!!.itemCount + 1))
        textUrl.hint = "Playlist name"

        val buttonOk = dialog.findViewById<Button>(R.id.buttonOk)
        val buttonOkTop = dialog.findViewById<Button>(R.id.buttonOkTop)
        val buttonCancel = dialog.findViewById<Button>(R.id.buttonCancel)
        val buttonCancelTop = dialog.findViewById<Button>(R.id.buttonCancelTop)

        val okOnclickListener: (v: View) -> Unit = {
            val title = textUrl.text.toString()
            if (title.isNotBlank()) {
                val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
                val addedPlaylistId = sqLiteDBHelper.addPlaylist(title)
                dialog.dismiss()
                onPlaylistItemClick(title, addedPlaylistId, -1)
            } else {
                Toast.makeText(context, context!!.getString(R.string.empty_playlist_name_warning), Toast.LENGTH_SHORT).show()
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

        // Gets a content resolver instance
        // Gets a content resolver instance
        val cr: ContentResolver = this.context!!.contentResolver

        // response to some other intent, and the code below shouldn't run at all.
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            var folderName: String? = null

            val uriList = mutableListOf<Uri?>()
            val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
            if (resultData != null) {

                val clipData = resultData.clipData
                if (clipData == null) {
                    val uri = resultData.data!!
                    val type = cr.getType(uri)
                    Timber.i("type: $type")


                    //content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2FEnigma%2FOST%2BTunguska%2BSecret%2BFiles%2B3%2BQueen%2Bfor%2Ba%2BDay%2BSecret%2BFiles%2B3%2BOST.mp3
                    //content://com.android.providers.downloads.documents/document/msf%3A25

                    if (folderName == null) {
                        val realPath = RealPathUtil.getRealPath(this.context!!, uri)
                        Timber.i("RealPath: $realPath")
                        if (realPath != null) {
                            val file = File(realPath)
                            folderName = file.parentFile?.name
                        }
                    }
                    uriList.add(uri)

                    //HyperLog.i(AlarmUtils.TAG, "Uri: " + uri?.path)
                    sqLiteDBHelper.addLocalMediaUrl(currentPlaylistID, uri.toString(), getFileNameFromContentProvider(uri))
                } else {
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        if (folderName == null) {
                            val realPath = RealPathUtil.getRealPath(this.context!!, uri!!)
                            Timber.i("RealPath: $realPath")
                            if (realPath != null) {
                                val file = File(realPath)
                                folderName = file.parentFile?.name
                            }
                        }
                        uriList.add(uri)
                        //HyperLog.i(AlarmUtils.TAG, "Uri: " + uri.path!! + " i=$i")
                        sqLiteDBHelper.addLocalMediaUrl(currentPlaylistID, uri.toString(), getFileNameFromContentProvider(uri))
                    }
                }

                renamePlaylist(folderName)
                val contentResolver = context!!.contentResolver
                val persistedUriPermissions = contentResolver.persistedUriPermissions
                Timber.i("persistedUriPermissions size: ${persistedUriPermissions.size}")

                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION

                AsyncTask.execute {
                    var count = 0

                    uriList.filterNotNull().forEach {
                        try {
                            contentResolver.takePersistableUriPermission(it, takeFlags)
                            count++
                        } catch (ex: Exception) {
                            //private static final int MAX_PERSISTED_URI_GRANTS = 128;
                            Timber.e(ex, "Count: $count")
                        }
                    }
                }

                val cursor = sqLiteDBHelper.getLocalMedia(currentPlaylistID)
                mediaItemViewAdapter?.updateDataSet(cursor)
            }
        }
    }

    private fun renamePlaylist(newTitle: String?) {
        if (newTitle != null && mediaItemViewAdapter?.itemCount == 0) {
            onPlaylistItemEditClick(newTitle, currentPlaylistID, -1)
            currentPlaylistTitle = newTitle
            fragmentTitle.text = newTitle
        }
    }

    private fun getFileNameFromContentProvider(uri: Uri?): String? {
        activity?.contentResolver
                ?.query(uri!!, arrayOf(OpenableColumns.DISPLAY_NAME), null,
                        null, null, null).use { cursor ->
                    // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
                    // "if there's anything to look at, look at it" conditionals.
                    if (cursor != null && cursor.moveToFirst()) {
                        // Note it's called "Display Name".  This is
                        // provider-specific, and might not necessarily be the file name.

                        return cursor.getString(
                                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                }
        return null
    }

    private fun isReadStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this.context!!, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Timber.v("Filesystem read Permission is granted")
                true
            } else {
                Timber.v("Filesystem read is revoked")
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3)
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Timber.v("Filesystem read Permission is granted automatically")
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            3 -> {
                Timber.d("External storage1")
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.v("""Permission: ${permissions[0]}was ${grantResults[0]}""")
                    //resume tasks needing this permission
                    openFilesViaKnot()
                } else {
                    performFileSearch()
                }
            }
        }
    }

    private fun openFilesViaKnot() {
        KnotFileChooser(this.context!!,
                allowBrowsing = true, // Allow User Browsing
                allowCreateFolder = false, // Allow User to create Folder
                allowMultipleFiles = true, // Allow User to Select Multiple Files
                allowSelectFolder = false, // Allow User to Select Folder
                minSelectedFiles = 1, // Allow User to Selec Minimum Files Selected
                maxSelectedFiles = 500, // Allow User to Selec Minimum Files Selected
                showFiles = true, // Show Files or Show Folder Only
                showFoldersFirst = true, // Show Folders First or Only Files
                showFolders = true, //Show Folders
                showHiddenFiles = false, // Show System Hidden Files
//                initialFolder = Environment.getExternalStorageDirectory(), //Initial Folder
//                initialFolder = File("/mnt/sdcard"),
                initialFolder = Environment.getExternalStorageDirectory().absoluteFile,
//                initialFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), //Initial Folder
//                initialFolder = this.context!!.getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!, //Initial Folder
//                initialFolder = File("/storage/sdcard"),
//                initialFolder = File("/"),
                restoreFolder = true, //Restore Folder After Adding
//                fileType = FileType.AUDIO, //Select Which Files you want to show (By Default : ALL)
                cancelable = true) //Dismiss Dialog On Cancel (Optional)
                .title(this.context?.getString(R.string.select_audio_files)) // Title of Dialog
                .addFilter(ExtensionFilter("mp3", "flac", "ogg", "wav", "amr", "wma", "aac"))
                .sorter(Sorter.ByNameInAscendingOrder) // Sort Data (Optional)

                .onSelectedFilesListener { filesList -> // Callback Returns Selected File Object  (Optional)
//                    Toast.makeText(this.context!!, filesList.toString(), Toast.LENGTH_SHORT).show()
                    val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
                    filesList.forEach {
                        sqLiteDBHelper.addLocalMediaUrl(currentPlaylistID, it.toString(), it.name)
                    }

                    renamePlaylist(filesList.firstOrNull()?.parentFile?.name)

                    val cursor = sqLiteDBHelper.getLocalMedia(currentPlaylistID)
                    mediaItemViewAdapter?.updateDataSet(cursor)
                }
                .show()
    }


    private fun performNewFileSearch() {
        if (SystemClock.elapsedRealtime() - buttonAddLastClickTime < 800) {
            return
        }
        buttonAddLastClickTime = SystemClock.elapsedRealtime()

        if (isReadStoragePermissionGranted()) {
            openFilesViaKnot()
        } else {
            performFileSearch()
        }
    }


    private fun performFileSearch() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        // To search for all documents available via installed storage providers,
        intent.type = "*/*"

//        <data android:mimeType="application/mp4*"/>
//        <data android:mimeType="application/mpeg*"/>
//        <data android:mimeType="application/itunes"/>
//        <data android:mimeType="application/ogg"/>
//        <data android:mimeType="application/opus"/>
//        <data android:mimeType="application/x-ogg"/>
//        <data android:mimeType="application/x-flac"/>
//        <data android:mimeType="application/x-mpegurl"/>
//        <data android:mimeType="application/x-extension-mp4"/>
//        <data android:mimeType="application/vnd.apple.mpegurl"/>
//        <data android:mimeType="application/mpegurl"/>


        val extraMimeTypes = arrayOf("audio/mpeg", "audio/aac", "audio/wav",
                "audio/x-flac", "audio/flac", "audio/ogg", "audio/vorbis", "audio/mid")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes)

        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onFileItemClick(url: String?, position: Int) {
        initPlaylist(position, true)
    }

    private fun initPlaylist(position: Int, active: Boolean) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        var media: MutableList<Media>
        sqLiteDBHelper.getLocalMedia(currentPlaylistID).use { allLocalMedia ->
            media = ArrayList(allLocalMedia.count)
            while (allLocalMedia.moveToNext()) {
                val uri = allLocalMedia.getString(allLocalMedia.getColumnIndex("uri"))
                val title = allLocalMedia.getString(allLocalMedia.getColumnIndex("title"))
                media.add(Media(uri, title))
            }

            val newPlayList = if (active)
                SleepAssistantPlayListActive(position, media, SleepMediaType.LOCAL, currentPlaylistID)
            else
                SleepAssistantPlayListIdle(position, media, SleepMediaType.LOCAL, currentPlaylistID)

            EventBus.getDefault().postSticky(newPlayList)
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
        currentPlaylistTitle = title
        sqLiteDBHelper.setPropertyString(PLAYLIST_TITLE, title)

        setMode(sqLiteDBHelper)
    }

    override fun onPlaylistItemEditClick(newTitle: String, id: Long, position: Int) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!
        val storedPlaylistID = sqLiteDBHelper.getPropertyLong(PLAYLIST_ID)
        if (storedPlaylistID == id) {
            sqLiteDBHelper.setPropertyString(PLAYLIST_TITLE, newTitle)
        }

        sqLiteDBHelper.renamePlaylist(id, newTitle)
        playlistViewAdapter?.updateDataSet(sqLiteDBHelper.getAllPlaylists())
    }

    override fun onPlaylistDeleteClick(id: Long, position: Int) {
        val sqLiteDBHelper = sqLiteDBHelper(this.context!!)!!

        val savedPlaylistId = sqLiteDBHelper.getPropertyLong(PLAYLIST_ID)
        if (savedPlaylistId == id) {
            currentPlaylistID = -1
            sqLiteDBHelper.setPropertyString(PLAYLIST_ID, "-1")
            sqLiteDBHelper.setPropertyString(ACTIVE_TAB, "2")
            sqLiteDBHelper.setPropertyString(TRACK_NUMBER, "0")
            sqLiteDBHelper.setPropertyString(PLAYLIST_TITLE, null)

            setMode(sqLiteDBHelper)
        }

        sqLiteDBHelper.deletePlaylist(id)
        playlistViewAdapter?.updateDataSet(sqLiteDBHelper.getAllPlaylists())
    }

    companion object {
        val PLAYLIST_ID_KEY = "playlist_id_key"
    }
}