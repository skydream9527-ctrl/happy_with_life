package com.xiaoquexing.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xiaoquexing.app.ui.album.AlbumScreen
import com.xiaoquexing.app.ui.album.AlbumViewerScreen
import com.xiaoquexing.app.ui.home.HomeScreen
import com.xiaoquexing.app.ui.profile.AchievementScreen
import com.xiaoquexing.app.ui.profile.PlantGuideScreen
import com.xiaoquexing.app.ui.profile.PlantSelectionScreen
import com.xiaoquexing.app.ui.profile.ProfileScreen
import com.xiaoquexing.app.ui.record.RecordScreen
import com.xiaoquexing.app.ui.share.ShareScreen
import com.xiaoquexing.app.ui.timeline.TimelineScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object Record : Screen("record", "记录", Icons.Default.AddCircle)
    data object Timeline : Screen("timeline", "时间线", Icons.Default.DateRange)
    data object Album : Screen("album", "画册", Icons.Default.PhotoLibrary)
    data object Profile : Screen("profile", "我的", Icons.Default.AccountCircle)
    data object PlantSelection : Screen("plant_selection", "选择植物", null)
    data object PlantGuide : Screen("plant_guide", "植物图鉴", null)
    data object Achievement : Screen("achievement", "成就墙", null)
    data object Share : Screen("share/{recordId}", "分享", null) {
        fun createRoute(recordId: Long) = "share/$recordId"
    }
    data object AlbumViewer : Screen("album_viewer/{albumId}", "画册浏览", null) {
        fun createRoute(albumId: Long) = "album_viewer/$albumId"
    }
}

val bottomBarItems = listOf(
    Screen.Home,
    Screen.Record,
    Screen.Timeline,
    Screen.Album,
    Screen.Profile
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = bottomBarItems.any { it.route == currentDestination?.route }

            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomBarItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    screen.icon!!,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToRecord = { navController.navigate(Screen.Record.route) },
                    onNavigateToPlantSelection = { navController.navigate(Screen.PlantSelection.route) },
                    onNavigateToPlantGuide = { navController.navigate(Screen.PlantGuide.route) },
                    onNavigateToShare = { recordId -> navController.navigate(Screen.Share.createRoute(recordId)) }
                )
            }
            composable(Screen.Record.route) {
                RecordScreen(
                    onPublished = { navController.popBackStack(Screen.Home.route, false) }
                )
            }
            composable(Screen.Timeline.route) {
                TimelineScreen(
                    onNavigateToShare = { recordId -> navController.navigate(Screen.Share.createRoute(recordId)) }
                )
            }
            composable(Screen.Album.route) {
                AlbumScreen(
                    onOpenAlbum = { albumId -> navController.navigate(Screen.AlbumViewer.createRoute(albumId)) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToPlantSelection = { navController.navigate(Screen.PlantSelection.route) },
                    onNavigateToAchievement = { navController.navigate(Screen.Achievement.route) },
                    onNavigateToPlantGuide = { navController.navigate(Screen.PlantGuide.route) }
                )
            }
            composable(Screen.PlantSelection.route) {
                PlantSelectionScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.PlantGuide.route) {
                PlantGuideScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Achievement.route) {
                AchievementScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Share.route,
                arguments = listOf(navArgument("recordId") { type = NavType.LongType })
            ) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
                ShareScreen(
                    recordId = recordId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.AlbumViewer.route,
                arguments = listOf(navArgument("albumId") { type = NavType.LongType })
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getLong("albumId") ?: 0L
                AlbumViewerScreen(
                    albumId = albumId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
