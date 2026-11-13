package com.fdv.fdvflightlogger.data.update

import android.content.Context
import com.fdv.fdvflightlogger.data.network.AppHttp
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** Downloads a release's APK into the app's cache and proves it's exactly what GitHub published. */
internal object UpdateDownloader {

    private const val DIR = "updates"

    /** Returns the verified APK. [onProgress] gets whole percents (0–100), and only when the number changes. */
    @Throws(UpdateException::class)
    suspend fun download(context: Context, release: AppRelease, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val url = release.apkUrl ?: throw UpdateException("This release has no APK attached yet.")
            val dir = File(context.cacheDir, DIR).apply { mkdirs() }
            val target = File(dir, "fdv-flight-logger-${release.version}.apk")

            // APKs for other versions are never needed again
            dir.listFiles()?.filter { it != target }?.forEach { it.delete() }

            // Already downloaded and verified (e.g. the pilot cancelled Android's prompt): reuse it
            if (target.exists() && matchesPublished(target, release)) {
                onProgress(100)
                return@withContext target
            }

            // Write to a .part file and rename only once verified, so a half-download never looks finished
            val partial = File(dir, "${target.name}.part")
            try {
                AppHttp.client.prepareGet(url) {
                    // The shared client's 20 s request timeout suits API calls, not a 20 MB download
                    timeout {
                        requestTimeoutMillis = 10 * 60_000L
                        socketTimeoutMillis = 60_000L
                    }
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        throw UpdateException("Download failed (HTTP ${response.status.value}). Try again in a moment.")
                    }
                    val total = response.contentLength() ?: release.apkSize ?: -1L
                    val channel = response.bodyAsChannel()
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var lastPercent = -1

                    partial.outputStream().use { out ->
                        while (true) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            done += read
                            if (total > 0) {
                                val percent = ((done * 100) / total).toInt().coerceIn(0, 100)
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(percent)
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                partial.delete()
                throw e
            } catch (e: IOException) {
                partial.delete()
                throw UpdateException("The download was interrupted. Check your connection and try again.")
            }

            if (!matchesPublished(partial, release)) {
                partial.delete()
                throw UpdateException("The download didn't match the published update, so it wasn't installed. Try again.")
            }
            if (!partial.renameTo(target)) throw UpdateException("Couldn't save the update.")
            target
        }

    // Size and SHA-256 must match what GitHub published, whenever it published them
    private fun matchesPublished(file: File, release: AppRelease): Boolean {
        release.apkSize?.let { if (file.length() != it) return false }
        val expected = release.apkSha256 ?: return true
        return sha256(file).equals(expected, ignoreCase = true)
    }

    internal fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}