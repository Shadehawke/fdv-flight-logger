package com.fdv.fdvflightlogger.data.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

/** Hands a verified APK to Android's installer. Android shows its own confirmation screen. */
internal object UpdateInstaller {

    @Throws(UpdateException::class)
    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
        }
        val sessionId = installer.createSession(params)

        try {
            installer.openSession(sessionId).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("base.apk", 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                // Explicit and mutable: the installer adds the status extras to this intent
                val status = Intent(context, InstallStatusReceiver::class.java)
                val pending = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    status,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                session.commit(pending.intentSender)
            }
        } catch (e: Exception) {
            // Several unrelated exception types (I/O, security, a bad APK); all mean "this attempt is over"
            installer.abandonSession(sessionId)
            throw UpdateException("Couldn't hand the update to Android's installer.")
        }
    }
}

/** Install failures for the ViewModel to show; success needs no message (Android restarts the app). */
internal object UpdateStatus {
    private val _failures = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val failures: SharedFlow<String> = _failures.asSharedFlow()

    fun report(message: String) {
        _failures.tryEmit(message)
    }
}

class InstallStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            // Android needs the pilot's OK: show its confirmation screen
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.let { confirm ->
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            // The app is being replaced; the old process ends here
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> UpdateStatus.report(
                messageFor(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
            )
        }
    }

    private fun messageFor(status: Int, detail: String?): String = when (status) {
        PackageInstaller.STATUS_FAILURE_ABORTED -> "Update cancelled. Tap Update to try again."
        PackageInstaller.STATUS_FAILURE_CONFLICT ->
            "This update can't replace the installed app: they're signed with different keys. " +
                    "(Builds from Android Studio can't be updated by a release.)"
        PackageInstaller.STATUS_FAILURE_STORAGE -> "Not enough storage to install the update."
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "This update isn't compatible with this device."
        else -> detail?.let { "The update couldn't be installed: $it" } ?: "The update couldn't be installed."
    }
}