package com.hoc.fileexplorer

import java.io.File
import java.util.Date

fun deleteFile(path: String): SuccessOrFailure<File> {
    return runCatching {
        val file = File(path)
        val result = if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
        if (result) {
            file
        } else {
            throw IllegalStateException("Cannot delete ${if (file.isDirectory) "folder" else "file"}")
        }
    }
}

fun createNewFolder(path: String, folderName: String): SuccessOrFailure<File> {
    return runCatching {
        val file = File(path, folderName)
        if (file.exists()) {
            throw IllegalStateException("Folder already exists")
        }
        if (file.mkdir()) {
            file
        } else {
            throw IllegalStateException("Cannot create new file")
        }
    }
}

fun createNewFile(path: String, fileName: String): SuccessOrFailure<File> {
    return runCatching {
        val file = File(path, fileName)
        if (file.createNewFile()) {
            file
        } else {
            throw IllegalStateException("Cannot create new file")
        }
    }
}

fun getAllFilesFromPath(
    path: String,
    showHiddenFiles: Boolean = false,
    onlyFolers: Boolean = false
): List<File> {
    return File(path)
        .listFiles { pathname ->
            (showHiddenFiles || !pathname.isHidden)
                && (!onlyFolers || pathname.isDirectory)
        }.toList()
}

val selectorFileType: (FileModel) -> Int = { -it.fileType.ordinal }
val selectorName: (FileModel) -> String = { it.name }

fun getFileModelsFromFiles(
    files: List<File>
): List<FileModel> {
    return files.map {
        FileModel(
            it.path,
            it.name,
            it.length(),
            FileType.getFileType(it),
            Date(it.lastModified()),
            it.extension,
            it.listFiles()?.size ?: 0
        )
    }.sortedWith(compareBy(selectorFileType, selectorName))
}