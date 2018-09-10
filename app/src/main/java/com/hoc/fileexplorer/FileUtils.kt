package com.hoc.fileexplorer

import java.io.File

fun createNewFile(path: String, fileName: String): SuccessOrFailure<String> {
    return runCatching {
        val file = File(path, fileName)
        if (file.createNewFile()) {
            file.path
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

fun getFileModelsFromFiles(files: List<File>): List<FileModel> {
    return files.map {
        FileModel(
            it.path,
            it.name,
            it.length(),
            FileType.getFileType(it),
            it.extension,
            it.listFiles()?.size ?: 0
        )
    }
}