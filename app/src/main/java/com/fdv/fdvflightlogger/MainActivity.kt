package com.fdv.fdvflightlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.nav.AppNavHost
import com.fdv.fdvflightlogger.ui.nav.Routes
import com.fdv.fdvflightlogger.ui.theme.FDVFlightLoggerTheme

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state = appViewModel.state.collectAsStateWithLifecycle().value

            FDVFlightLoggerTheme {
                val startDestination = if (state.isSetupComplete) {
                    Routes.FLIGHT_LOG
                } else {
                    Routes.SETUP
                }

                AppNavHost(
                    appViewModel = appViewModel,
                    startDestination = startDestination
                )
            }
        }
    }
}
