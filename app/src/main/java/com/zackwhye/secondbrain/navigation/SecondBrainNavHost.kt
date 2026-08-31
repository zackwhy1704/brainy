package com.zackwhye.secondbrain.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zackwhye.secondbrain.feature.ask.ui.AskRoute
import com.zackwhye.secondbrain.feature.home.ui.HomeRoute
import com.zackwhye.secondbrain.feature.itemdetail.ui.ItemDetailRoute
import com.zackwhye.secondbrain.feature.person.ui.PersonRoute

/** Nav graph: Home ↔ Item detail ↔ Person, Ask via bottom nav; Ask citations open Item detail. */
@Composable
fun SecondBrainNavHost(navController: NavHostController = rememberNavController(), modifier: Modifier = Modifier) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isTopLevel = backStackEntry?.destination?.hasRoute<Destinations.Home>() == true ||
        backStackEntry?.destination?.hasRoute<Destinations.Ask>() == true

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    NavigationBarItem(
                        selected = backStackEntry?.destination?.hasRoute<Destinations.Home>() == true,
                        onClick = { navController.navigate(Destinations.Home) { launchSingleTop = true } },
                        icon = { Text("⌂") },
                        label = { Text("Home") },
                    )
                    NavigationBarItem(
                        selected = backStackEntry?.destination?.hasRoute<Destinations.Ask>() == true,
                        onClick = { navController.navigate(Destinations.Ask) { launchSingleTop = true } },
                        icon = { Text("⌁") },
                        label = { Text("Ask") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController = navController, startDestination = Destinations.Home, modifier = Modifier) {
            composable<Destinations.Home> {
                HomeRoute(
                    modifier = Modifier.padding(padding),
                    onItemClick = { id -> navController.navigate(Destinations.ItemDetail(id)) },
                )
            }
            composable<Destinations.ItemDetail> {
                ItemDetailRoute(
                    modifier = Modifier.padding(padding),
                    onBackClick = { navController.popBackStack() },
                    onPersonClick = { subject -> navController.navigate(Destinations.Person(subject)) },
                )
            }
            composable<Destinations.Person> {
                PersonRoute(
                    modifier = Modifier.padding(padding),
                    onBackClick = { navController.popBackStack() },
                    onSourceClick = { id -> navController.navigate(Destinations.ItemDetail(id)) },
                )
            }
            composable<Destinations.Ask> {
                AskRoute(
                    modifier = Modifier.padding(padding),
                    onCitationClick = { id -> navController.navigate(Destinations.ItemDetail(id)) },
                )
            }
        }
    }
}
