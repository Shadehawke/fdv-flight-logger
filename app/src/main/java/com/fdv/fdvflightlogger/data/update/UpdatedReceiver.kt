package com.fdv.fdvflightlogger.data.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fdv.fdvflightlogger.R

/**
 * Android closes the app to install its update and never reopens it. This runs in the new version
 * as soon as it's installed, and offers a one-tap way back in.
 */
class UpdatedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        // Every run from Android Studio "updates" a debug build; don't notify for those
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) return
        UpdateNotifier.showUpdated(context)
    }
}

object UpdateNotifier {

    private const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 1001

    fun showUpdated(context: Context) {
        // No permission (Android 13+) or notifications switched off: nothing to show, and that's fine
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return

        // Creating a channel that already exists does nothing, so this is safe every time
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Says when an update has finished installing"
            }
        )

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?: return
        val open = PendingIntent.getActivity(
            context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle("FDV Flight Logger updated")
            .setContentText(if (version.isNotEmpty()) "Now on version $version. Tap to open." else "Tap to open.")
            .setContentIntent(open)
            .setAutoCancel(true)          // disappears once tapped
            .build()
        notifications.notify(NOTIFICATION_ID, notification)
    }
}