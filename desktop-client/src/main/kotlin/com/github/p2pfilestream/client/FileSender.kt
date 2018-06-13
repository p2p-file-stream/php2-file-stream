package com.github.p2pfilestream.client

import com.github.p2pfilestream.chat.BinaryMessageChunk
import com.github.p2pfilestream.chat.FileDownloader
import com.github.p2pfilestream.chat.FileUploader
import mu.KLogging
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

class FileSender(
    private val file: File,
    private val downloader: FileDownloader,
    /** Size of a chunk in bytes  */
    private val chunkSize: Int = 1024,
    /** If byte-arrays should be cloned, needed for unit-testing */
    private val cloneBytes: Boolean = false
) : FileUploader {
    private val inputStream = file.inputStream()
    private var sending = false
    private var cancelled = false
    private var chunkCount = 0

    private companion object : KLogging()

    /** Start or resume sending chunks */
    override fun start() {
        sending = true
        thread(name = "Reader for ${file.name}") { read() }
    }

    /** Pause sending chunks because of full buffer (backpressure). */
    override fun pause() {
        sending = false
    }

    /** Cancel the upload */
    override fun cancel() {
        cancelled = true
        sending = false
    }

    /**
     * Read file and parse into message-chunk
     * Stop if sending becomes false
     */
    private fun read() {
        val data = ByteArray(chunkSize)
        while (sending) {
            val bytesRead: Int
            try {
                bytesRead = inputStream.read(data)
            } catch (e: IOException) {
                downloader.close(true)
                closeInputStream()
                return
            }
            if (bytesRead == -1) {
                // End of file
                downloader.close()
                closeInputStream()
                return
            }
            val chunkBytes = when {
                bytesRead != chunkSize -> {
                    // At the last chuck, bytes-read is not the full chunk size
                    // So only take the bytes that were read
                    data.take(bytesRead).toByteArray()
                }
                cloneBytes -> data.clone()
                else -> data
            }
            // If chunkBytes are not cloned, the receiver should immediately process them
            downloader.chunk(BinaryMessageChunk(chunkCount++, chunkBytes))
        }
        if (cancelled) {
            closeInputStream()
        }
    }

    private fun closeInputStream() {
        logger.info { "Closing InputStream" }
        inputStream.close()
    }
}