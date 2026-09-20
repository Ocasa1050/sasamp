package com.russia.launcher.download

import com.russia.launcher.NetworkService
import com.russia.launcher.async.dto.response.FileInfo
import com.russia.launcher.config.Config.APK_FILE_NAME
import com.russia.launcher.ui.activity.LoaderActivity
import com.russia.launcher.utils.BytesTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import java.util.zip.Adler32

interface DownloadListener {
    fun onDownloadComplete()
    fun onDownloadFailed()
}

class FileDownloader(
    private val loaderActivity: LoaderActivity,
    private var filesList: MutableList<FileInfo>)
{

    private var lastTime: Long = System.currentTimeMillis()
    private var lastDonwloaded: Long = 0
    private var curSpeed: Long = 0

    private var totalFilesSize: Long = 0
    private var totalDownloadedSize: Long = 0

    private var downloadListener: DownloadListener? = null

    private var leftFilesList: MutableList<FileInfo> = filesList.toMutableList()

    fun setDownloadListener(listener: DownloadListener) {
        downloadListener = listener
    }

    fun downloadAndUnzipFiles() {
        filesList = leftFilesList.toMutableList()
        totalFilesSize = filesList.sumOf { it.size }
        totalDownloadedSize = 0

        GlobalScope.launch(Dispatchers.Default) {
            for (file in filesList) {
                try {
                    downloadAndUnzipFile(file)
                    leftFilesList.remove(file)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        downloadListener?.onDownloadFailed()
                    }
                    return@launch
                }
            }
            withContext(Dispatchers.Main) {
                downloadListener?.onDownloadComplete()
            }
        }
    }

    private fun downloadFile(from: String, to: String) {
        val url = URL(from)
        if (url.protocol != "https") {
            throw IOException("Only HTTPS downloads are allowed")
        }

        val connection = url.openConnection() as? HttpURLConnection
            ?: throw IOException("Unsupported download connection")
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true

        val outputFile = File(to)
        outputFile.parentFile?.mkdirs()
        val partialFile = File("$to.part")

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Download failed with HTTP $responseCode: $from")
            }

            connection.inputStream.use { inputStream ->
                FileOutputStream(partialFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)

                        totalDownloadedSize += bytesRead.toLong()

                        val percentDownloaded = if (totalFilesSize > 0) {
                            (totalDownloadedSize.toDouble() / totalFilesSize * 100)
                                .toInt()
                                .coerceAtMost(100)
                        } else {
                            0
                        }

                        val currentTime = System.currentTimeMillis()
                        val time = currentTime - lastTime
                        if (time >= 1000) {
                            curSpeed = totalDownloadedSize - lastDonwloaded
                            lastDonwloaded = totalDownloadedSize
                            lastTime = currentTime
                        }

                        val text = String.format(
                            "%s из %s (%s / сек.)",
                            BytesTo.convert(totalDownloadedSize),
                            BytesTo.convert(totalFilesSize),
                            BytesTo.convert(curSpeed)
                        )

                        loaderActivity.updateProgress(percentDownloaded, outputFile.name, text)
                    }
                }
            }

            if (outputFile.exists() && !outputFile.delete()) {
                throw IOException("Unable to replace existing file: $to")
            }
            if (!partialFile.renameTo(outputFile)) {
                throw IOException("Unable to finalize downloaded file: $to")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun downloadAndInstallFile() {
        totalFilesSize = filesList.sumOf { it.size }
        totalDownloadedSize = 0
        GlobalScope.launch(Dispatchers.Default) {
            try {
                downloadFile(
                    NetworkService.APK_URL,
                    loaderActivity.getExternalFilesDir(null).toString() + "/" + APK_FILE_NAME
                )

                loaderActivity.installApk()

                withContext(Dispatchers.Main) {
                    downloadListener?.onDownloadComplete()
                }
            }
            catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    downloadListener?.onDownloadFailed()
                }
            }
        }
    }

    private suspend fun downloadAndUnzipFile(fileInfo: FileInfo) = withContext(Dispatchers.IO) {
        val externalFilesDir = loaderActivity.getExternalFilesDir(null)
            ?: throw IOException("External files directory is unavailable")
        val downloadUrl = fileInfo.url.trim()
        if (downloadUrl.isEmpty()) {
            throw IOException("No download URL for ${fileInfo.path}")
        }

        val zipFile = File(externalFilesDir, "${fileInfo.path}.zip")
        val targetFile = File(externalFilesDir, fileInfo.path)

        downloadFile(
            downloadUrl,
            zipFile.path
        )
        println("Скачали файл ${fileInfo.path}")
        unzipFile(zipFile, targetFile)

        if (!isFileValid(targetFile, fileInfo)) {
            targetFile.delete()
            throw IOException("Downloaded file failed validation: ${fileInfo.path}")
        }
        zipFile.delete()
    }

    private fun unzipFile(zipFile: File, targetFile: File) {
        val partialTarget = File("${targetFile.path}.part")
        partialTarget.delete()
        targetFile.parentFile?.mkdirs()

        loaderActivity.runOnUiThread {
            loaderActivity.speedText?.text = "Распаковка ..."
        }

        ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
            val zipEntry = zipInputStream.nextEntry
                ?: throw IOException("Downloaded archive is empty")
            if (zipEntry.isDirectory) {
                throw IOException("Downloaded archive contains a directory instead of a file")
            }

            FileOutputStream(partialTarget).use { outputStream ->
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (zipInputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }

            zipInputStream.closeEntry()
            if (zipInputStream.nextEntry != null) {
                throw IOException("Downloaded archive contains more than one file")
            }
        }

        if (targetFile.exists() && !targetFile.delete()) {
            throw IOException("Unable to replace existing file: ${targetFile.path}")
        }
        if (!partialTarget.renameTo(targetFile)) {
            throw IOException("Unable to finalize extracted file: ${targetFile.path}")
        }

        println("Распаковка завершена: ${zipFile.path}")
    }

    private fun isFileValid(file: File, expected: FileInfo): Boolean {
        if (!file.isFile || file.length() != expected.size) {
            return false
        }

        val adler32 = Adler32()
        FileInputStream(file).use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                adler32.update(buffer, 0, bytesRead)
            }
        }
        return adler32.value == expected.hash
    }

}