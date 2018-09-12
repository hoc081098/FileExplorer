package com.hoc.fileexplorer

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.File
import java.util.Date

enum class FileType {
    FILE, FOLDER;

    companion object {
        @JvmStatic
        fun getFileType(file: File): FileType {
            return when {
                file.isDirectory -> FOLDER
                file.isFile -> FILE
                else -> throw IllegalStateException()
            }
        }
    }
}

@Parcelize
data class FileModel(
    val path: String,
    val name: String,
    val sizeInBytes: Long,
    val fileType: FileType,
    val lastModified: Date,
    val extension: String = "",
    val subFiles: Int = 0
) : Parcelable