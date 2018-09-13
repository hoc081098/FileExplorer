package com.hoc.fileexplorer

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hoc.fileexplorer.FilesListFragment.Companion.FOLDER_PATHS
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_create_new_layout.view.*
import kotlinx.coroutines.DefaultDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.UI
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import com.hoc.fileexplorer.deleteFile as deleteSelectedFile

class MainActivity : AppCompatActivity(), FilesListFragment.OnItemClickListener {
    private val breadcrumbsAdapter = BreadcrumbsAdapter(::onBreadcrumbItemClick)
    private val parentJob = Job()

    private var isCopyActive = false
    private var copySelectedFile: FileModel? = null

    private lateinit var files: List<FileModel>

    private fun onBreadcrumbItemClick(fileModel: FileModel) {
        supportFragmentManager.popBackStack(fileModel.path, 0)
        files = files.dropLastWhile { it != fileModel }
        updateRecyclerBreadcrumbs(files)
    }

    private fun updateRecyclerBreadcrumbs(files: List<FileModel>) {
        breadcrumbsAdapter.submitList(files)
        if (files.isNotEmpty()) recycler_breadcrumbs.smoothScrollToPosition(files.lastIndex)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "File explorer"

        setupRecyclerBreadcrumbs()
        files = savedInstanceState?.getParcelableArrayList(KEY_FILES) ?: emptyList()
        updateRecyclerBreadcrumbs(files)

        isCopyActive = savedInstanceState?.getBoolean(KEY_COPY_SELECTED_FILE) ?: false
        if (isCopyActive) invalidateOptionsMenu()

        copySelectedFile = savedInstanceState?.getParcelable(KEY_COPY_SELECTED_FILE)

        if (savedInstanceState === null) {
            when (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)) {
                PERMISSION_GRANTED -> addRootFragment()
                else -> ActivityCompat.requestPermissions(
                    this,
                    arrayOf(WRITE_EXTERNAL_STORAGE),
                    RC_WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    private fun addRootFragment() {
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        supportFragmentManager.beginTransaction()
            .add(
                R.id.main_container,
                FilesListFragment.newInstance(rootPath)
            )
            .addToBackStack(rootPath)
            .commit()
        files = listOf(FileModel(rootPath, "/", 0, FileType.FOLDER, Date(0)))
        updateRecyclerBreadcrumbs(files)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RC_WRITE_EXTERNAL_STORAGE -> if (grantResults.firstOrNull() == PERMISSION_GRANTED) {
                addRootFragment()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.run {
            putParcelableArrayList(KEY_FILES, ArrayList(files))
            putParcelable(KEY_COPY_SELECTED_FILE, copySelectedFile)
            putBoolean(KEY_IS_COPY_ACTIVE, isCopyActive)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        parentJob.cancel()
    }

    private fun setupRecyclerBreadcrumbs() {
        recycler_breadcrumbs.run {
            adapter = breadcrumbsAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity, HORIZONTAL, false)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        files = files.dropLast(1)
        updateRecyclerBreadcrumbs(files)

        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        }
    }

    override fun onLongClick(fileModel: FileModel) {

        FileOptionsDialog().run {
            onDeleteClick = { showDeleteAlertDialog(fileModel) }
            onCopyClick = {
                copySelectedFile = fileModel
                isCopyActive = true
                invalidateOptionsMenu()
            }
            show(supportFragmentManager, FILE_OPTIONS_DIALOG_TAG)
        }
    }

    private fun showDeleteAlertDialog(fileModel: FileModel) {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Delete")
            .setIcon(R.drawable.ic_warning_black_24dp)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()

                launch(UI, parent = parentJob) {
                    withContext(DefaultDispatcher) { deleteSelectedFile(fileModel.path) }
                        .onSuccess {
                            Toast.makeText(
                                this@MainActivity,
                                "Delete ${if (fileModel.fileType == FileType.FOLDER) "folder" else "file"} successfully",
                                Toast.LENGTH_SHORT
                            ).show()


                            LocalBroadcastManager.getInstance(this@MainActivity)
                                .sendBroadcast(Intent(ACTION_CHANGE_FILES).apply {
                                    putStringArrayListExtra(FOLDER_PATHS, arrayListOf(it.parent, it.parentFile?.parent))
                                })
                        }
                        .onFailure {
                            Toast.makeText(
                                this@MainActivity,
                                it.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .show()
    }

    override fun onClick(fileModel: FileModel) {
        if (fileModel.fileType == FileType.FOLDER) {
            addFragment(fileModel)
        } else {
            val intent = Intent(ACTION_VIEW).apply {
                addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                data = FileProvider.getUriForFile(
                    this@MainActivity,
                    AUTHORITY,
                    File(fileModel.path)
                )
            }
            startActivity(Intent.createChooser(intent, "Open file"))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (isCopyActive) menuInflater.inflate(R.menu.menu_main_action_mode, menu)
        else menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_new_file -> {
                files.lastOrNull()?.let { createNew(::createNewFile, it.path) }
                true
            }
            R.id.action_new_folder -> {
                files.lastOrNull()?.let { createNew(::createNewFolder, it.path) }
                true
            }
            R.id.action_cancel -> {
                copySelectedFile = null
                isCopyActive = false
                invalidateOptionsMenu()
                true
            }
            R.id.action_paste -> {
                FileIntentService.enqueueWork(
                    this@MainActivity,
                    Intent(ACTION_COPY_FILE).apply {
                        putExtra(EXTRA_FILE_PATH, copySelectedFile?.path)
                        putExtra(EXTRA_DESTINATION_PATH, files.lastOrNull()?.path)
                    })
                copySelectedFile = null
                isCopyActive = false
                invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private inline fun createNew(
        crossinline operation: (String, String) -> SuccessOrFailure<File>,
        path: String
    ) {
        BottomSheetDialog(this).run {
            setContentView(
                LayoutInflater.from(context).inflate(
                    R.layout.dialog_create_new_layout,
                    null
                ).also { view ->
                    view.button_create.setOnClickListener {
                        val fileName = view.text_input_layout.editText?.text
                        if (fileName.isNullOrEmpty()) {
                            view.text_input_layout.error = "Invalid file name"
                        } else {
                            launch(UI, parent = parentJob) {
                                withContext(DefaultDispatcher) {
                                    operation(
                                        path,
                                        fileName.toString()
                                    )
                                }.onFailure {
                                    Toast.makeText(
                                        this@MainActivity,
                                        it.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }.onSuccess {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Create ${if (it.isDirectory) "folder" else "file"} successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    LocalBroadcastManager.getInstance(this@MainActivity)
                                        .sendBroadcast(Intent(ACTION_CHANGE_FILES).apply {
                                            putStringArrayListExtra(FOLDER_PATHS, arrayListOf(it.parent, it.parentFile?.parent))
                                        })
                                }
                                dismiss()
                            }
                        }
                    }
                })
            show()
        }
    }

    private fun addFragment(fileModel: FileModel) {
        files += fileModel
        updateRecyclerBreadcrumbs(files)

        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, FilesListFragment.newInstance(fileModel.path))
            .addToBackStack(fileModel.path)
            .commit()
    }

    companion object {
        const val KEY_FILES = "KEY_FILES"
        const val KEY_COPY_SELECTED_FILE = "KEY_COPY_SELECTED_FILE"
        const val KEY_IS_COPY_ACTIVE = "KEY_IS_COPY_ACTIVE"

        const val ACTION_CHANGE_FILES = "ACTION_CHANGE_FILES"
        const val FILE_OPTIONS_DIALOG_TAG = "FILE_OPTIONS_DIALOG_TAG"
        const val AUTHORITY = "com.hoc.fileexplorer.fileprovider"
        const val RC_WRITE_EXTERNAL_STORAGE = 2

        const val ACTION_COPY_FILE = "ACTION_COPY_FILE"
        const val EXTRA_FILE_PATH = "EXTRA_FILE_PATH"
        const val EXTRA_DESTINATION_PATH = "EXTRA_DESTINATION_PATH"
    }
}

