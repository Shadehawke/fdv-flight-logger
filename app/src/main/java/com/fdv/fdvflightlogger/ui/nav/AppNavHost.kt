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
import com.fdv.fdvflightlogger.ui.screens.FlightLandingScreen
import com.fdv.fdvflightlogger.ui.screens.FlightLogScreen
import com.fdv.fdvflightlogger.ui.screens.SetupScreen
import com.fdv.fdvflightlogger.ui.screens.SettingsScreen
import com.fdv.fdvflightlogger.ui.screens.SplashScreen
import com.fdv.fdvflightlogger.ui.mappers.FlightTemplate

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

        composable(Routes.LANDING) {
            FlightLandingScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }

        composable(Routes.SETUP) {
            SetupScreen(
                appViewModel = appViewModel,
                onSetupComplete = {
                    navController.navigate(Routes.LANDING) {
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

        composable(
            route = Routes.LOG_FROM,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("template") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            val template = backStackEntry.arguments?.getString("template")
                ?.let { name -> FlightTemplate.entries.firstOrNull { it.name == name } }
                ?: return@composable

            FlightLogScreen(
                appViewModel = appViewModel,
                navController = navController,
                copyFromId = id,
                template = template
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

