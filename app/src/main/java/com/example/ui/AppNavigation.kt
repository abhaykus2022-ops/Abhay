package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.ui.screens.AiDirectorScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanDim
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.SlateSurface
import com.example.viewmodel.AiDirectorViewModel
import com.example.viewmodel.ProjectsViewModel
import com.example.viewmodel.StudioViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ProjectsRoute

@Serializable
data class StudioRoute(val projectId: Long = -1L)

@Serializable
data class AiDirectorRoute(val projectId: Long? = null)

private data class TopNavItem(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
    val isProjects: Boolean = false,
    val isStudio: Boolean = false,
    val isDirector: Boolean = false
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val projectsViewModel: ProjectsViewModel = viewModel()
    val studioViewModel: StudioViewModel = viewModel()
    val aiDirectorViewModel: AiDirectorViewModel = viewModel()

    val activeProject by studioViewModel.project.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navItems = listOf(
        TopNavItem(
            labelRes = R.string.nav_projects,
            selectedIcon = Icons.Filled.VideoLibrary,
            unselectedIcon = Icons.Outlined.VideoLibrary,
            tag = "nav_projects",
            isProjects = true
        ),
        TopNavItem(
            labelRes = R.string.nav_studio,
            selectedIcon = Icons.Filled.MovieFilter,
            unselectedIcon = Icons.Outlined.MovieFilter,
            tag = "nav_studio",
            isStudio = true
        ),
        TopNavItem(
            labelRes = R.string.nav_ai_director,
            selectedIcon = Icons.Filled.AutoAwesome,
            unselectedIcon = Icons.Outlined.AutoAwesome,
            tag = "nav_ai_director",
            isDirector = true
        )
    )

    fun isRouteSelected(item: TopNavItem): Boolean = when {
        item.isProjects -> currentDestination?.hasRoute<ProjectsRoute>() == true
        item.isStudio -> currentDestination?.hasRoute<StudioRoute>() == true
        item.isDirector -> currentDestination?.hasRoute<AiDirectorRoute>() == true
        else -> false
    }

    fun navigateToItem(item: TopNavItem) {
        val route: Any = when {
            item.isProjects -> ProjectsRoute
            item.isStudio -> StudioRoute(projectId = activeProject?.id ?: -1L)
            else -> AiDirectorRoute(projectId = activeProject?.id)
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        if (useRail) {
            Scaffold(
                containerColor = MidnightBg,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = SlateSurface.copy(alpha = 0.86f)
                    ) {
                        navItems.forEach { item ->
                            val selected = isRouteSelected(item)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToItem(item) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyanDim,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }

                    StudioNavHost(
                        navController = navController,
                        projectsViewModel = projectsViewModel,
                        studioViewModel = studioViewModel,
                        aiDirectorViewModel = aiDirectorViewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = MidnightBg,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = SlateSurface.copy(alpha = 0.88f),
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navItems.forEach { item ->
                            val selected = isRouteSelected(item)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToItem(item) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyanDim,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                StudioNavHost(
                    navController = navController,
                    projectsViewModel = projectsViewModel,
                    studioViewModel = studioViewModel,
                    aiDirectorViewModel = aiDirectorViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun StudioNavHost(
    navController: androidx.navigation.NavHostController,
    projectsViewModel: ProjectsViewModel,
    studioViewModel: StudioViewModel,
    aiDirectorViewModel: AiDirectorViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = ProjectsRoute,
        modifier = modifier
    ) {
        composable<ProjectsRoute> {
            ProjectsScreen(
                viewModel = projectsViewModel,
                onOpenProject = { projectId ->
                    navController.navigate(StudioRoute(projectId = projectId)) {
                        launchSingleTop = true
                    }
                },
                onOpenAiDirector = {
                    navController.navigate(AiDirectorRoute(projectId = null)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<StudioRoute> { backStackEntry ->
            val route: StudioRoute = backStackEntry.toRoute()
            StudioScreen(
                viewModel = studioViewModel,
                projectId = route.projectId.takeIf { it > 0L },
                onNavigateToProjects = {
                    navController.navigate(ProjectsRoute) {
                        popUpTo(ProjectsRoute) { inclusive = true }
                    }
                },
                onNavigateToAiDirector = { projectId ->
                    navController.navigate(AiDirectorRoute(projectId = projectId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<AiDirectorRoute> { backStackEntry ->
            val route: AiDirectorRoute = backStackEntry.toRoute()
            AiDirectorScreen(
                viewModel = aiDirectorViewModel,
                projectId = route.projectId
            )
        }
    }
}
