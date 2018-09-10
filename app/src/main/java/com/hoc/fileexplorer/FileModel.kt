package com.hoc.fileexplorer

import java.io.File

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

data class FileModel(
    val path: String,
    val name: String,
    val sizeInBytes: Long,
    val fileType: FileType,
    val extension: String = "",
    val subFiles: Int = 0
)