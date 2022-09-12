package com.ojhdtapp.parabox.core.util

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object DownloadManagerUtil {
    fun downloadWithManager(context: Context, url: String, fileName: String): Long? {
        return try {
            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(
                    MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url))
                )
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                setAllowedOverRoaming(false)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setTitle(fileName)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    File.pathSeparator + "Parabox" + File.separator + fileName
                )
            }
            Toast.makeText(context, "开始下载${fileName}到/Download/Parabox", Toast.LENGTH_SHORT).show()
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show()
            null
        }
    }

    fun retrieve(context: Context, id: Long): Flow<DownloadingState> {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return flow {
            val downloading = AtomicBoolean(true)

            while (downloading.get()) {
                val query = DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(query)

                cursor.moveToFirst()

                val bytesDownloaded =
                    cursor.intValue(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotal = cursor.intValue(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                if (isSuccessful(cursor)) downloading.set(false)
                cursor.close()
                emit(DownloadingState.Downloading(bytesDownloaded, bytesTotal))
                if (bytesDownloaded == bytesTotal) {
                    emit(DownloadingState.Done)
                }

                if (downloading.get()) delay(1000)
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun isSuccessful(cursor: Cursor) = status(cursor) == DownloadManager.STATUS_SUCCESSFUL
    private fun status(cursor: Cursor) = cursor.intValue(DownloadManager.COLUMN_STATUS)
    private fun Cursor.column(which: String) = this.getColumnIndex(which)
    private fun Cursor.intValue(which: String): Int = this.getInt(column(which))
}