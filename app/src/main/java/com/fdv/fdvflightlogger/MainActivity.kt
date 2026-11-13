package com.fdv.fdvflightlogger

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdv.fdvflightlogger.data.auth.FdvAuth
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.UiEvent
import com.fdv.fdvflightlogger.ui.nav.AppNavHost
import com.fdv.fdvflightlogger.ui.theme.FDVFlightLoggerTheme

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        setTheme(R.style.Theme_FDVFlightLogger)

        super.onCreate(savedInstanceState)

        // Cold start from the redirect (savedInstanceState is null only on a genuinely new launch,
        // so a rotation doesn't handle the same link twice)
        if (savedInstanceState == null) handleAuthIntent(intent)
        // Warm return: singleTask delivers the redirect to this running activity instead
        addOnNewIntentListener { newIntent -> handleAuthIntent(newIntent) }

        setContent {
            val state = appViewModel.state.collectAsStateWithLifecycle().value

            LaunchedEffect(state.settings.keepScreenOn) {
                if (state.settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            FDVFlightLoggerTheme(
                themeMode = state.settings.themeMode
            ) {
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    appViewModel.events.collect { event ->
                        when (event) {
                            is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
                            is UiEvent.ExportSuccess ->
                                snackbarHostState.showSnackbar("Exported ${event.fileName}")
                            is UiEvent.ExportError -> snackbarHostState.showSnackbar(event.message)
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { padding ->
                    AppNavHost(
                        appViewModel = appViewModel,
                        modifier = Modifier.padding(padding)
                    )
                }
                val needsInstallPermission by appViewModel.needsInstallPermission.collectAsStateWithLifecycle()
                if (needsInstallPermission) {
                    AlertDialog(
                        onDismissRequest = { appViewModel.dismissInstallPermission() },
                        title = { Text("Allow app updates") },
                        text = {
                            Text(
                                "To install updates, Android needs your OK for FDV Flight Logger to install apps. " +
                                        "This is a one-time step: switch on \"Allow from this source\", come back, " +
                                        "and tap Update again."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                appViewModel.dismissInstallPermission()
                                // Opens this app's own entry in "Install unknown apps"
                                startActivity(
                                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:$packageName".toUri())
                                )
                            }) { Text("Open settings") }
                        },
                        dismissButton = {
                            TextButton(onClick = { appViewModel.dismissInstallPermission() }) { Text("Not now") }
                        }
                    )
                }
            }
        }
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (!uri.toString().startsWith(FdvAuth.REDIRECT_URI)) return

        val params = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }
        appViewModel.handleAuthRedirect(params)
    }
}