package com.fdv.fdvflightlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

        setContent {
            val state = appViewModel.state.collectAsStateWithLifecycle().value

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
            }
        }
    }
}