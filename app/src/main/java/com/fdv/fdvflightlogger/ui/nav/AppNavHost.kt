package com.fdv.fdvflightlogger.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.screens.FlightLogScreen
import com.fdv.fdvflightlogger.ui.screens.SetupScreen
import com.fdv.fdvflightlogger.ui.screens.FlightHistoryScreen

@Composable
fun AppNavHost(
    appViewModel: AppViewModel,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.SETUP) {
            SetupScreen(
                appViewModel = appViewModel,
                onSetupComplete = {
                    navController.navigate(Routes.FLIGHT_LOG) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.FLIGHT_LOG) {
            FlightLogScreen(appViewModel = appViewModel, navController = navController)
        }

        composable(Routes.HISTORY) {
            FlightHistoryScreen(appViewModel = appViewModel, navController = navController)
        }
    }
}
