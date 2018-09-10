package com.hoc.fileexplorer

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hoc.fileexplorer.FilesAdapter.Companion.diffCallback
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.breadcrumb_item_layout.view.*
import kotlinx.android.synthetic.main.dialog_create_new_layout.view.*
import kotlinx.coroutines.DefaultDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.UI
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BreadcrumbsAdapter(private val onClickListener: (FileModel) -> Unit) :
    ListAdapter<FileModel, BreadcrumbsAdapter.ViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.breadcrumb_item_layout, parent, false)
            .let(::ViewHolder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val textView = itemView.textView

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(fileModel: FileModel) {
            textView.text = fileModel.name
        }

        override fun onClick(v: View) {
            adapterPosition.let {
                if (it != NO_POSITION) {
                    onClickListener(getItem(it))
                }
            }
        }
    }
}

class MainActivity : AppCompatActivity(), FilesListFragment.OnItemClickListener {
    private val breadcrumbsAdapter = BreadcrumbsAdapter(::onBreadcrumbItemClick)
    private var files = emptyList<FileModel>()
    private val parentJob = Job()

    private fun onBreadcrumbItemClick(fileModel: FileModel) {
        supportFragmentManager.popBackStack(fileModel.path, 0)
        files = files.dropLastWhile { it != fileModel }
        updateRecyclerBreadcrumbs()
    }

    private fun updateRecyclerBreadcrumbs() {
        breadcrumbsAdapter.submitList(files)
        if (files.isNotEmpty()) recycler_breadcrumbs.smoothScrollToPosition(files.lastIndex)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "File explorer"

        if (savedInstanceState === null) {
            val rootPath = Environment.getExternalStorageDirectory().absolutePath
            supportFragmentManager.beginTransaction()
                .add(
                    R.id.main_container,
                    FilesListFragment.newInstance(rootPath)
                )
                .addToBackStack(rootPath)
                .commit()
            files = listOf(FileModel(rootPath, "/", 0, FileType.FOLDER))
            updateRecyclerBreadcrumbs()
        }

        setupRecyclerBreadcrumbs()
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
        updateRecyclerBreadcrumbs()

        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        }
    }

    override fun onLongClick(fileModel: FileModel) {
        Toast.makeText(this@MainActivity, "Long click", Toast.LENGTH_SHORT).show()
    }

    override fun onClick(fileModel: FileModel) {
        if (fileModel.fileType == FileType.FOLDER) {
            addFragment(fileModel)
        } else {
            val intent = Intent(ACTION_VIEW).apply {
                addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                data = FileProvider.getUriForFile(
                    this@MainActivity,
                    "com.hoc.fileexplorer.fileprovider",
                    File(fileModel.path)
                )
            }
            startActivity(Intent.createChooser(intent, "Open file"))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_new_file -> {
                files.lastOrNull()?.let { createNewFile(it.path) }
                true
            }
            R.id.action_new_folder -> {
                createNewFolder()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createNewFolder() {
    }


    private fun createNewFile(path: String) {
        BottomSheetDialog(this).apply {
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
                                    createNewFile(
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
                                        "Create file successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    LocalBroadcastManager.getInstance(this@MainActivity).sendBroadcast(Intent("CREATE_NEW_FILE"))
                                }
                                this@apply.dismiss()
                            }
                        }
                    }
                })
            show()
        }
    }

    private fun addFragment(fileModel: FileModel) {
        files += fileModel
        updateRecyclerBreadcrumbs()

        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, FilesListFragment.newInstance(fileModel.path))
            .addToBackStack(fileModel.path)
            .commit()
    }
}

