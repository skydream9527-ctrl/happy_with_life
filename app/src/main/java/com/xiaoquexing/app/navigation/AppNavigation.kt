package com.xiaoquexing.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xiaoquexing.app.ui.album.AlbumScreen
import com.xiaoquexing.app.ui.album.AlbumViewerScreen
import com.xiaoquexing.app.ui.auth.LoginScreen
import com.xiaoquexing.app.ui.home.HomeScreen
import com.xiaoquexing.app.ui.profile.AchievementScreen
import com.xiaoquexing.app.ui.profile.PlantGuideScreen
import com.xiaoquexing.app.ui.profile.PlantSelectionScreen
import com.xiaoquexing.app.ui.profile.ProfileScreen
import com.xiaoquexing.app.ui.profile.ConflictScreen
import com.xiaoquexing.app.ui.profile.ReviewScreen
import com.xiaoquexing.app.ui.profile.FootprintScreen
import com.xiaoquexing.app.ui.profile.PlanScreen
import com.xiaoquexing.app.ui.profile.SettingsScreen
import com.xiaoquexing.app.ui.profile.SharedSpaceScreen
import com.xiaoquexing.app.ui.record.RecordDetailScreen
import com.xiaoquexing.app.ui.record.RecordScreen
import com.xiaoquexing.app.ui.share.ShareScreen
import com.xiaoquexing.app.ui.timeline.TimelineScreen

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "首页")
    data object Record : Screen("record", "记录")
    data object RecordEdit : Screen("record/{recordId}", "编辑") {
        fun createRoute(recordId: Long) = "record/$recordId"
    }
    data object RecordDetail : Screen("record_detail/{recordId}", "记录") {
        fun createRoute(recordId: Long) = "record_detail/$recordId"
    }
    data object Timeline : Screen("timeline", "时间线")
    data object Album : Screen("album", "画册")
    data object Profile : Screen("profile", "我的")
    data object Login : Screen("login", "登录")
    data object Spaces : Screen("spaces", "共享空间")
    data object Review : Screen("review", "月年回顾")
    data object Conflicts : Screen("conflicts", "同步冲突")
    data object Settings : Screen("settings", "设置")
    data object Plan : Screen("plan", "订阅")
    data object Footprints : Screen("footprints", "足迹")
    data object PlantSelection : Screen("plant_selection", "选择植物")
    data object PlantGuide : Screen("plant_guide", "植物图鉴")
    data object Achievement : Screen("achievement", "成就墙")
    data object Share : Screen("share/{recordId}", "分享") {
        fun createRoute(recordId: Long) = "share/$recordId"
    }
    data object AlbumViewer : Screen("album_viewer/{albumId}", "画册浏览") {
        fun createRoute(albumId: Long) = "album_viewer/$albumId"
    }
}

private data class TabItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// 设计稿顺序：首页 / 时间线 / [中央记录] / 画册 / 我的
private val tabItems = listOf(
    TabItem(Screen.Home, Icons.Filled.Home, Icons.Outlined.Home),
    TabItem(Screen.Timeline, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    TabItem(Screen.Album, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    TabItem(Screen.Profile, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
)

@Composable
fun AppNavigation(openCompose: Boolean = false) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(openCompose) {
        if (openCompose) navController.navigate(Screen.Record.route)
    }
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = listOf(
        Screen.Home.route, Screen.Timeline.route,
        Screen.Album.route, Screen.Profile.route
    ).any { it == currentDestination?.route }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
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
                    onPublished = { navController.popBackStack(Screen.Home.route, false) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.RecordEdit.route,
                arguments = listOf(navArgument("recordId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("recordId") ?: 0L
                RecordScreen(
                    recordId = id,
                    onPublished = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Screen.RecordDetail.route,
                arguments = listOf(navArgument("recordId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("recordId") ?: 0L
                RecordDetailScreen(
                    recordId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Screen.RecordEdit.createRoute(it)) },
                    onShare = { navController.navigate(Screen.Share.createRoute(it)) },
                )
            }
            composable(Screen.Timeline.route) {
                TimelineScreen(
                    onNavigateToShare = { recordId -> navController.navigate(Screen.RecordDetail.createRoute(recordId)) }
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
                    onNavigateToPlantGuide = { navController.navigate(Screen.PlantGuide.route) },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                    onNavigateToSpaces = { navController.navigate(Screen.Spaces.route) },
                    onNavigateToReview = { navController.navigate(Screen.Review.route) },
                    onNavigateToConflicts = { navController.navigate(Screen.Conflicts.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToPlan = { navController.navigate(Screen.Plan.route) },
                    onNavigateToFootprints = { navController.navigate(Screen.Footprints.route) },
                )
            }
            composable(Screen.Footprints.route) {
                FootprintScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Plan.route) {
                PlanScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Review.route) {
                ReviewScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Conflicts.route) {
                ConflictScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Spaces.route) {
                SharedSpaceScreen(
                    onBack = { navController.popBackStack() },
                    onNeedLogin = { navController.navigate(Screen.Login.route) },
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(onBack = { navController.popBackStack() })
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

        // 悬浮 iOS 风格 Tab Bar（中央凸起记录按钮）
        if (showBottomBar) {
            FloatingTabBar(
                currentRoute = currentDestination?.route,
                onTabClick = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onRecordClick = { navController.navigate(Screen.Record.route) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(10f)
            )
        }
    }
}

@Composable
private fun FloatingTabBar(
    currentRoute: String?,
    onTabClick: (Screen) -> Unit,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 10.dp)
    ) {
        // 胶囊形主体
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(barColor)
                .border(0.5.dp, borderColor, RoundedCornerShape(24.dp)),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabItems.take(2).forEach { tab ->
                    TabItemButton(
                        tab = tab,
                        isSelected = currentRoute == tab.screen.route,
                        onClick = { onTabClick(tab.screen) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 中央记录按钮占位
                Spacer(modifier = Modifier.size(56.dp))

                tabItems.drop(2).forEach { tab ->
                    TabItemButton(
                        tab = tab,
                        isSelected = currentRoute == tab.screen.route,
                        onClick = { onTabClick(tab.screen) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 中央凸起记录按钮
        val recordActive = currentRoute == Screen.Record.route
        val scale by animateFloatAsState(
            targetValue = if (recordActive) 1.06f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "recordScale"
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-10).dp)
                .size(52.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = GreenShadow,
                    spotColor = GreenShadow
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            RecordBtnDark
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onRecordClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "记录",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// 135° 渐变深端（#4A7D54）与投影色
private val RecordBtnDark = Color(0xFF4A7D54)
private val GreenShadow = Color(0x595E9B6A)

@Composable
private fun TabItemButton(
    tab: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        label = "tabColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = tab.screen.title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.screen.title,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}
