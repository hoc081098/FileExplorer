package com.hoc.fileexplorer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import kotlinx.android.synthetic.main.file_item_layout.view.*
import kotlinx.android.synthetic.main.fragment_files_list.*
import kotlinx.coroutines.DefaultDispatcher
import kotlinx.coroutines.android.UI
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesAdapter(
    private val onClickListener: (FileModel) -> Unit,
    private val onLongClickListener: (FileModel) -> Unit
) : ListAdapter<FileModel, FilesAdapter.ViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.file_item_layout, parent, false)
            .let(::ViewHolder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {
        private val textName = itemView.text_name!!
        private val textSizeOrSubFiles = itemView.text_size_or_sub_files!!

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bind(fileModel: FileModel) {
            textName.text = fileModel.name

            textSizeOrSubFiles.text = when (fileModel.fileType) {
                FileType.FOLDER -> "${fileModel.subFiles} files"
                FileType.FILE -> {
                    val sizeInMBs = fileModel.sizeInBytes / (1024 * 1024.0)
                    "${"%.2f".format(sizeInMBs)} MB"
                }
            }
        }

        override fun onClick(v: View) {
            adapterPosition.let {
                if (it != NO_POSITION) {
                    onClickListener(getItem(it))
                }
            }
        }

        override fun onLongClick(v: View): Boolean {
            return when (
                val pos = adapterPosition) {
                NO_POSITION -> false
                else -> {
                    onLongClickListener(getItem(pos))
                    true
                }
            }
        }
    }

    companion object {
        @JvmField
        val diffCallback = object : DiffUtil.ItemCallback<FileModel>() {
            override fun areItemsTheSame(oldItem: FileModel, newItem: FileModel): Boolean {
                return oldItem.path == newItem.path
            }

            override fun areContentsTheSame(oldItem: FileModel, newItem: FileModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class FilesListFragment : Fragment() {
    private val fileAdapter = FilesAdapter({ callback.onClick(it) }, { callback.onLongClick(it) })
    private lateinit var path: String
    private lateinit var callback: OnItemClickListener
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "CREATE_NEW_FILE" -> {
                    getDateAndUpdateList()
                }
            }
        }
    }

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
            layoutManager = LinearLayoutManager(requireContext())
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

        getDateAndUpdateList()

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(broadcastReceiver, IntentFilter().apply { addAction("CREATE_NEW_FILE") })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(broadcastReceiver)
    }

    private fun getDateAndUpdateList() {
        launch(UI) {
            progress_bar.visibility = View.VISIBLE

            val files = withContext(DefaultDispatcher) {
                getFileModelsFromFiles(getAllFilesFromPath(path)).also(::print)
            }

            progress_bar.visibility = View.INVISIBLE
            empty_list_layout.visibility = if (files.isEmpty()) View.VISIBLE else View.INVISIBLE
            fileAdapter.submitList(files)
        }
    }

    interface OnItemClickListener {
        fun onLongClick(fileModel: FileModel)
        fun onClick(fileModel: FileModel)
    }

    companion object {
        @JvmStatic
        fun newInstance(path: String): FilesListFragment {
            return FilesListFragment().apply {
                arguments = Bundle().apply { putString(ARG_PATH, path) }
            }
        }

        const val ARG_PATH = "com.hoc.fileexplorer.fileslist.path"
    }
}