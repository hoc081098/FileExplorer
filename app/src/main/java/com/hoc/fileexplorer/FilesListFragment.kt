package com.hoc.fileexplorer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import com.hoc.fileexplorer.MainActivity.Companion.ACTION_CHANGE_FILES
import kotlinx.android.synthetic.main.fragment_files_list.*
import kotlinx.coroutines.DefaultDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.UI
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesListFragment : Fragment() {
    private val fileAdapter = FilesAdapter({ callback.onClick(it) }, { callback.onLongClick(it) })
    private val parentJob = Job()
    private val broadcastReceiver = UpdateReceive()
    private val intentFilter = IntentFilter().apply { addAction(ACTION_CHANGE_FILES) }

    private var listState: Parcelable? = null
    private var files: List<FileModel>? = null

    private lateinit var path: String
    private lateinit var callback: OnItemClickListener
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        callback = context as OnItemClickListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_files_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        path = arguments?.getString(ARG_PATH)!!

        recycler.run {
            layoutManager = LinearLayoutManager(requireContext()).also { linearLayoutManager = it }
            setHasFixedSize(true)
            adapter = fileAdapter


            addItemDecoration(
                DividerItemDecoration(requireContext(), VERTICAL).apply {
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.divider
                    )?.let(::setDrawable)
                }
            )
        }

        files = savedInstanceState?.getParcelableArrayList(FILES)
        if (files.isNullOrEmpty()) {
            getDateAndUpdateList()
        } else {
            updateList(files)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putParcelable(LIST_STATE, linearLayoutManager.onSaveInstanceState())
            files?.let { putParcelableArrayList(FILES, ArrayList(it)) }
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(
                broadcastReceiver,
                intentFilter
            )
        listState?.let { linearLayoutManager.onRestoreInstanceState(it) }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        parentJob.cancel()
    }

    private fun getDateAndUpdateList() {
        launch(UI, parent = parentJob) {
            progress_bar.visibility = View.VISIBLE
            withContext(DefaultDispatcher) {
                getFileModelsFromFiles(getAllFilesFromPath(path)).also(::print)
            }.let {
                Log.d("MY_TAG", "update.................")
                updateList(it)
                files = it
            }
        }
    }

    private fun updateList(files: List<FileModel>?) {
        progress_bar.visibility = View.INVISIBLE
        empty_list_layout.visibility = if (files.isNullOrEmpty()) View.VISIBLE else View.INVISIBLE
        fileAdapter.submitList(files)
    }

    interface OnItemClickListener {
        fun onLongClick(fileModel: FileModel)
        fun onClick(fileModel: FileModel)
    }

    inner class UpdateReceive : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                ACTION_CHANGE_FILES -> {
                    if (path in intent.getStringArrayListExtra(FOLDER_PATHS)) {
                        Log.d(
                            "MY_TAG",
                            "${intent.action} ${intent.getStringExtra(FOLDER_PATHS)} ${path} should update list!"
                        )
                        getDateAndUpdateList()
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(path: String): FilesListFragment {
            return FilesListFragment().apply {
                arguments = Bundle().apply { putString(ARG_PATH, path) }
            }
        }

        const val ARG_PATH = "ARG_PATH"
        const val FOLDER_PATHS = "FOLDER_PATHS"
        const val LIST_STATE = "LIST_STATE"
        const val FILES = "KEY_FILES"
    }
}