package com.example.hello_kotlin.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hello_kotlin.ui.screens.AutoTagRulesScreen
import com.example.hello_kotlin.ui.screens.DashboardScreen
import com.example.hello_kotlin.ui.screens.TagManagerScreen
import com.example.hello_kotlin.ui.screens.TransactionsScreen
import com.example.hello_kotlin.ui.theme.DarkSurface
import com.example.hello_kotlin.ui.theme.AccentPrimary
import com.example.hello_kotlin.ui.theme.TextSecondary
import com.example.hello_kotlin.viewmodel.AutoTagViewModel
import com.example.hello_kotlin.viewmodel.DashboardViewModel
import com.example.hello_kotlin.viewmodel.TagViewModel
import com.example.hello_kotlin.viewmodel.TransactionViewModel
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    data object Transactions : Screen("transactions", "History", Icons.Filled.Receipt, Icons.Outlined.Receipt)
    data object Tags : Screen("tags", "Tags", Icons.AutoMirrored.Filled.Label, Icons.AutoMirrored.Outlined.Label)
    data object AutoRules : Screen("auto_rules", "Rules", Icons.AutoMirrored.Filled.Rule, Icons.AutoMirrored.Outlined.Rule)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.Tags,
    Screen.AutoRules
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val dashboardViewModel: DashboardViewModel = viewModel()
    val tagViewModel: TagViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()
    val autoTagViewModel: AutoTagViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = com.example.hello_kotlin.ui.theme.StitchSurfaceContainerLowest,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.label
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = com.example.hello_kotlin.ui.theme.StitchPrimary,
                            selectedTextColor = com.example.hello_kotlin.ui.theme.StitchPrimary,
                            unselectedIconColor = com.example.hello_kotlin.ui.theme.StitchOnSurfaceVariant,
                            unselectedTextColor = com.example.hello_kotlin.ui.theme.StitchOnSurfaceVariant,
                            indicatorColor = com.example.hello_kotlin.ui.theme.StitchPrimary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(250)) }
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    transactionViewModel = transactionViewModel,
                    tagViewModel = tagViewModel,
                    autoTagViewModel = autoTagViewModel
                )
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    dashboardViewModel = dashboardViewModel,
                    transactionViewModel = transactionViewModel,
                    tagViewModel = tagViewModel
                )
            }
            composable(Screen.Tags.route) {
                TagManagerScreen(
                    viewModel = tagViewModel,
                    dashboardViewModel = dashboardViewModel
                )
            }
            composable(Screen.AutoRules.route) {
                AutoTagRulesScreen(
                    autoTagViewModel = autoTagViewModel,
                    tagViewModel = tagViewModel
                )
            }
        }
    }
}
