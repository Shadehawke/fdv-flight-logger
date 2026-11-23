package com.fdv.fdvflightlogger.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.fdv.fdvflightlogger.data.update.AppRelease
import com.fdv.fdvflightlogger.ui.AppViewModel

/**
 * What the Update buttons call. On Android 13+ it first asks, once, to show notifications, so the
 * app can say "updated, tap to open" afterwards. Yes or no, the update then goes ahead.
 */
@Composable
fun rememberStartUpdate(appViewModel: AppViewModel): (AppRelease) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<AppRelease?>(null) }

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        pending?.let(appViewModel::installUpdate)
        pending = null
    }

    return { release ->
        val mustAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        if (mustAsk) {
            pending = release
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            appViewModel.installUpdate(release)
        }
    }
}