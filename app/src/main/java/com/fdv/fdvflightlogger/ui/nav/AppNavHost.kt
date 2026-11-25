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
import com.fdv.fdvflightlogger.ui.mappers.FlightTemplate
import com.fdv.fdvflightlogger.ui.screens.EventsScreen
import com.fdv.fdvflightlogger.ui.screens.FlightDetailScreen
import com.fdv.fdvflightlogger.ui.screens.FlightHistoryScreen
import com.fdv.fdvflightlogger.ui.screens.FlightLandingScreen
import com.fdv.fdvflightlogger.ui.screens.FlightLogScreen
import com.fdv.fdvflightlogger.ui.screens.HelpScreen
import com.fdv.fdvflightlogger.ui.screens.HelpSectionScreen
import com.fdv.fdvflightlogger.ui.screens.HelpTopicScreen
import com.fdv.fdvflightlogger.ui.screens.SettingsScreen
import com.fdv.fdvflightlogger.ui.screens.SetupScreen
import com.fdv.fdvflightlogger.ui.screens.SplashScreen
import com.fdv.fdvflightlogger.ui.screens.TrophyCaseScreen
import com.fdv.fdvflightlogger.ui.screens.RouteMapScreen

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
                onOpenHelp = { navController.navigate(Routes.HELP) },
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

        composable(Routes.TROPHIES) {
            TrophyCaseScreen(appViewModel = appViewModel, navController = navController)
        }

        composable(Routes.EVENTS) {
            EventsScreen(appViewModel = appViewModel, navController = navController)
        }

        composable(
            route = Routes.ROUTE_MAP,
            arguments = listOf(navArgument("filter") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            RouteMapScreen(
                appViewModel = appViewModel,
                navController = navController,
                initialFilterJson = backStackEntry.arguments?.getString("filter")
            )
        }

        composable(Routes.HELP) {
            HelpScreen(navController = navController)
        }

        composable(
            route = Routes.HELP_SECTION,
            arguments = listOf(navArgument("section") { type = NavType.StringType })
        ) { backStackEntry ->
            HelpSectionScreen(
                navController = navController,
                sectionId = backStackEntry.arguments?.getString("section").orEmpty()
            )
        }

        composable(
            route = Routes.HELP_TOPIC,
            arguments = listOf(
                navArgument("section") { type = NavType.StringType },
                navArgument("topic") { type = NavType.StringType },
                // Optional: only set when the topic was opened from a search
                navArgument("q") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            HelpTopicScreen(
                navController = navController,
                sectionId = args?.getString("section").orEmpty(),
                topicId = args?.getString("topic").orEmpty(),
                query = args?.getString("q").orEmpty()
            )
        }
    }
}

