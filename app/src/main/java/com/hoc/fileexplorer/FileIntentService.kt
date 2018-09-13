package com.hoc.fileexplorer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hoc.fileexplorer.FilesListFragment.Companion.FOLDER_PATHS
import com.hoc.fileexplorer.MainActivity.Companion.ACTION_CHANGE_FILES
import com.hoc.fileexplorer.MainActivity.Companion.ACTION_COPY_FILE
import com.hoc.fileexplorer.MainActivity.Companion.EXTRA_DESTINATION_PATH
import com.hoc.fileexplorer.MainActivity.Companion.EXTRA_FILE_PATH
import java.io.File

class FileIntentService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        when (intent.action) {
            ACTION_COPY_FILE -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: return
                val fileDestinationPath = intent.getStringExtra(EXTRA_DESTINATION_PATH) ?: return
                copyFile(filePath, fileDestinationPath)
            }
        }
    }

    private fun copyFile(filePath: String, folderPath: String) {
        Log.d("MY_TAG", "$filePath $folderPath")
        val srcFile = File(filePath)
        val targetFile = createTempFile(
            srcFile.nameWithoutExtension,
            ".${srcFile.extension}",
            File(folderPath)
        )
        try {
            srcFile.copyRecursively(targetFile, overwrite = true)
            LocalBroadcastManager.getInstance(applicationContext)
                .sendBroadcast(Intent(ACTION_CHANGE_FILES).apply {
                    putStringArrayListExtra(FOLDER_PATHS, arrayListOf(targetFile.parent, targetFile.parentFile?.parent))
                })
            Log.d("MY_TAG", "done ${targetFile.name}")
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Copy file successfully")
                .setContentText("from ${srcFile.name} to ${targetFile.parentFile.name}")
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setWhen(System.currentTimeMillis())
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()
                .let {
                    (getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager).notify(System.currentTimeMillis().toInt(), it)
                }
        } catch (e: Exception) {
            Log.d("MY_TAG", e.message, e)
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Copy file failure")
                .setContentText("from ${srcFile.name} to ${targetFile.parentFile.name}")
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setStyle(NotificationCompat.BigTextStyle().bigText("Error: ${e.message}"))
                .build()
                .let {
                    (getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager).notify(System.currentTimeMillis().toInt(), it)
                }
        }
    }

    companion object {
        @JvmStatic
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, FileIntentService::class.java, 101, work)
        }

        const val CHANNEL_ID = "com.hoc.fileexplorer.channelid"
    }
}
