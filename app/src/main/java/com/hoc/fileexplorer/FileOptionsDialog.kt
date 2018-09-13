package com.hoc.fileexplorer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.dialog_file_options_layout.*

class FileOptionsDialog : BottomSheetDialogFragment(), View.OnClickListener {
    var onDeleteClick: (() -> Unit)? = null
    var onCopyClick: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_file_options_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        text_delete.setOnClickListener(this)
        text_copy.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.text_delete -> onDeleteClick?.invoke()
            R.id.text_copy -> onCopyClick?.invoke()
        }
        dismiss()
    }
}