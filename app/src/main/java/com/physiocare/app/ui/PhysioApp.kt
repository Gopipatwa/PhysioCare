@file:OptIn(ExperimentalMaterial3Api::class)

package com.physiocare.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.physiocare.app.data.AppRepository
import com.physiocare.app.data.Category
import com.physiocare.app.ui.screens.CategoryScreen
import com.physiocare.app.ui.screens.ExerciseDetailScreen
import com.physiocare.app.ui.screens.HomeScreen
import com.physiocare.app.ui.screens.LibraryScreen
import com.physiocare.app.ui.screens.ProgressScreen
import com.physiocare.app.ui.screens.RecoveryScreen
import com.physiocare.app.ui.screens.SessionScreen
import com.physiocare.app.ui.screens.SettingsScreen
import com.physiocare.app.util.Speaker

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("library", "Exercises", Icons.Filled.List),
    Tab("progress", "Progress", Icons.Filled.DateRange),
    Tab("settings", "Settings", Icons.Filled.Settings),
)

@Composable
fun PhysioApp(repo: AppRepository, speaker: Speaker) {
    val settings = repo.settings
    LaunchedEffect(settings.voice) { speaker.enabled = settings.voice }

    // Text-size setting scales every font in the app (on top of the phone's own setting).
    val density = LocalDensity.current
    val fontScale = (density.fontScale * settings.textScale).coerceAtMost(1.8f)
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        PhysioTheme {
            val nav = rememberNavController()
            val entry by nav.currentBackStackEntryAsState()
            val route = entry?.destination?.route ?: "home"
            val inSession = route.startsWith("session")

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = { if (!inSession) BottomTabs(route, nav) },
            ) { padding ->
                NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                    composable("home") {
                        HomeScreen(
                            repo = repo,
                            onStartPlan = { ids -> nav.navigate("session/${ids.joinToString(",")}") },
                            onOpen = { nav.navigate("exercise/$it") },
                            onBrowse = { nav.switchTab("library") },
                            onRecovery = { nav.navigate("recovery") },
                        )
                    }
                    composable("library") {
                        LibraryScreen(
                            repo = repo,
                            onCategory = { nav.navigate("category/${it.name}") },
                            onOpen = { nav.navigate("exercise/$it") },
                            onRecovery = { nav.navigate("recovery") },
                        )
                    }
                    composable("category/{name}") { back ->
                        val cat = runCatching { Category.valueOf(back.arguments?.getString("name") ?: "") }.getOrNull()
                        if (cat != null) {
                            CategoryScreen(cat, repo, onBack = { nav.popBackStack() }, onOpen = { nav.navigate("exercise/$it") })
                        }
                    }
                    composable("exercise/{id}") { back ->
                        ExerciseDetailScreen(
                            id = back.arguments?.getString("id") ?: "",
                            repo = repo,
                            speaker = speaker,
                            onBack = { nav.popBackStack() },
                            onStart = { nav.navigate("session/$it") },
                        )
                    }
                    composable("session/{ids}") { back ->
                        val ids = (back.arguments?.getString("ids") ?: "").split(",").filter { it.isNotBlank() }
                        SessionScreen(ids, repo, speaker, onExit = { nav.popBackStack() })
                    }
                    composable("recovery") {
                        RecoveryScreen(
                            repo = repo,
                            onBack = { nav.popBackStack() },
                            onOpen = { nav.navigate("exercise/$it") },
                            onStart = { ids -> if (ids.isNotEmpty()) nav.navigate("session/${ids.joinToString(",")}") },
                        )
                    }
                    composable("progress") { ProgressScreen(repo) }
                    composable("settings") { SettingsScreen(repo, speaker) }
                }
            }

            if (!settings.disclaimerAccepted) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Welcome to PhysioCare", style = MaterialTheme.typography.headlineMedium) },
                    text = {
                        Text(
                            "Before you start:\n\n" +
                                "• Check with your doctor or physiotherapist that these exercises are right for you.\n" +
                                "• Move slowly and hold onto something steady when standing.\n" +
                                "• Stop if you feel sharp pain, chest pain, dizziness or shortness of breath.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { repo.updateSettings(repo.settings.copy(disclaimerAccepted = true)) },
                            modifier = Modifier.heightIn(min = 56.dp),
                        ) { Text("I understand", style = MaterialTheme.typography.labelLarge) }
                    },
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Big labelled tabs – easier to hit and read than the standard bar. */
@Composable
private fun BottomTabs(route: String, nav: NavHostController) {
    val current = when {
        route.startsWith("category") || route.startsWith("exercise") || route.startsWith("recovery") -> "library"
        else -> route
    }
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TABS.forEach { tab ->
                val selected = current == tab.route
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .clickable(role = Role.Tab) {
                            if (route != tab.route) nav.switchTab(tab.route)
                        }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        tab.icon, contentDescription = null, modifier = Modifier.size(30.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
