package com.nexus.intelligence.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexus.intelligence.ui.screens.dashboard.DashboardScreen
import com.nexus.intelligence.ui.screens.filemap.FileMapScreen
import com.nexus.intelligence.ui.screens.organizer.OrganizerScreen
import com.nexus.intelligence.ui.screens.search.SearchScreen
import com.nexus.intelligence.ui.screens.settings.SettingsScreen
import com.nexus.intelligence.ui.theme.NexusColors
import com.nexus.intelligence.ui.theme.NexusMonospace
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "NEXUS", Icons.Default.Dashboard)
    object Search : Screen("search/{query}", "SEARCH", Icons.Default.Search) {
        fun createRoute(query: String): String = "search/${URLEncoder.encode(query, "UTF-8")}"
    }
    object FileMap : Screen("filemap", "MAP", Icons.Default.Map)
    object Organizer : Screen("organizer", "ORGANIZER", Icons.Default.AutoAwesome)
    object Settings : Screen("settings", "CONFIG", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.FileMap,
    Screen.Organizer,
    Screen.Settings
)

@Composable
fun NexusNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current

    Scaffold(
        bottomBar = { NexusBottomBar(navController = navController) },
        containerColor = NexusColors.Black
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onSearch = { query -> navController.navigate(Screen.Search.createRoute(query)) },
                    onDocumentClick = { doc -> openDocument(context, doc.filePath, doc.mimeType) }
                )
            }

            composable(
                route = Screen.Search.route,
                arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query") ?: ""
                val decodedQuery = try { URLDecoder.decode(query, "UTF-8") } catch (e: Exception) { query }
                SearchScreen(
                    initialQuery = decodedQuery,
                    onBack = { navController.popBackStack() },
                    onDocumentClick = { doc -> openDocument(context, doc.filePath, doc.mimeType) }
                )
            }

            composable(Screen.FileMap.route) {
                FileMapScreen(
                    onBack = { navController.popBackStack() },
                    onDocumentClick = { doc -> openDocument(context, doc.filePath, doc.mimeType) }
                )
            }

            composable(Screen.Organizer.route) {
                OrganizerScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun NexusBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = NexusColors.DeepBlack,
        contentColor = NexusColors.Cyan,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route ||
                    (screen == Screen.Dashboard && currentRoute?.startsWith("dashboard") == true)

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        fontFamily = NexusMonospace,
                        fontSize = 8.sp,
                        letterSpacing = 1.5.sp
                    )
                },
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NexusColors.Cyan,
                    selectedTextColor = NexusColors.Cyan,
                    unselectedIconColor = NexusColors.TextDim,
                    unselectedTextColor = NexusColors.TextDim,
                    indicatorColor = NexusColors.CyanSubtle
                )
            )
        }
    }
}

private fun openDocument(context: Context, filePath: String, mimeType: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) { Toast.makeText(context, "Archivo no encontrado", Toast.LENGTH_SHORT).show(); return }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Sin app para abrir este tipo de archivo", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
