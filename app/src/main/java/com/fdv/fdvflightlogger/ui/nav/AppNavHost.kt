package com.fdv.fdvflightlogger.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fdv.fdvflightlogger.ui.AppViewModel
import com.fdv.fdvflightlogger.ui.screens.FlightDetailScreen
import com.fdv.fdvflightlogger.ui.screens.FlightHistoryScreen
import com.fdv.fdvflightlogger.ui.screens.FlightLogScreen
import com.fdv.fdvflightlogger.ui.screens.SetupScreen

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

        // Normal “new flight” entry
        composable(Routes.FLIGHT_LOG) {
            FlightLogScreen(appViewModel = appViewModel, navController = navController)
        }

        // History list
        composable(Routes.HISTORY) {
            FlightHistoryScreen(appViewModel = appViewModel, navController = navController)
        }

        // Detail screen
        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            FlightDetailScreen(
                appViewModel = appViewModel,
                navController = navController,
                flightId = id
            )
        }

        // Edit entry (prefilled)
        composable(
            route = "log/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            FlightLogScreen(
                appViewModel = appViewModel,
                navController = navController,
                editFlightId = id
            )
        }
    }
}
