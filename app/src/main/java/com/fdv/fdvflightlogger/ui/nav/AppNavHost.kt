package com.fdv.fdvflightlogger.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.fdv.fdvflightlogger.ui.screens.SettingsScreen
import com.fdv.fdvflightlogger.ui.screens.SplashScreen

@Composable
fun AppNavHost(
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            // You already have state in MainActivity, but you can read it here too.
            val state = appViewModel.state.collectAsStateWithLifecycle().value

            SplashScreen(
                isSetupComplete = state.isSetupComplete,
                onNavigateNext = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

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

        composable(Routes.SETTINGS) {
            SettingsScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
    }
}

