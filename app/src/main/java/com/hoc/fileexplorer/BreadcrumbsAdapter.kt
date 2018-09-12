package com.hoc.fileexplorer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.breadcrumb_item_layout.view.*

class BreadcrumbsAdapter(private val onClickListener: (FileModel) -> Unit) :
    ListAdapter<FileModel, BreadcrumbsAdapter.ViewHolder>(FilesAdapter.diffCallback) {
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
                if (it != RecyclerView.NO_POSITION) {
                    onClickListener(getItem(it))
                }
            }
        }
    }
}