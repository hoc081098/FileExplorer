package com.hoc.fileexplorer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.file_item_layout.view.*

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
        private val image = itemView.imageView2!!
        private val textName = itemView.text_name!!
        private val textSizeOrSubFiles = itemView.text_size_or_sub_files!!

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bind(fileModel: FileModel) {
            image.setImageResource(
                when (fileModel.fileType) {
                    FileType.FOLDER -> R.drawable.ic_folder_black_24dp
                    FileType.FILE -> R.drawable.ic_insert_drive_file_black_24dp
                }
            )

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
                if (it != RecyclerView.NO_POSITION) {
                    onClickListener(getItem(it))
                }
            }
        }

        override fun onLongClick(v: View): Boolean {
            return when (
                val pos = adapterPosition) {
                RecyclerView.NO_POSITION -> false
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
