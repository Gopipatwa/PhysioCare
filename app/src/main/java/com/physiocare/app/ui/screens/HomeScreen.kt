@file:OptIn(ExperimentalMaterial3Api::class)

package com.physiocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.physiocare.app.data.AppRepository
import com.physiocare.app.data.ExerciseLibrary
import com.physiocare.app.ui.Pine
import com.physiocare.app.ui.components.BigButton
import com.physiocare.app.ui.components.BigOutlinedButton
import com.physiocare.app.ui.components.ExerciseRow
import com.physiocare.app.ui.components.SectionTitle
import com.physiocare.app.ui.components.StatTile
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    repo: AppRepository,
    onStartPlan: (List<String>) -> Unit,
    onOpen: (String) -> Unit,
    onBrowse: () -> Unit,
    onRecovery: () -> Unit,
) {
    val s = repo.settings
    val greet = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val today = repo.todaySessions()
    val minutes = (today.sumOf { it.durationSec } + 59) / 60
    val streak = repo.streak()
    val plan = repo.plan.toList()
    val doneToday = today.map { it.exerciseId }.toSet()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Pine)
                    .padding(20.dp),
            ) {
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
                Text(
                    greet + if (s.name.isNotBlank()) ",\n${s.name}" else "",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    when {
                        today.isNotEmpty() -> "You've moved today. Well done."
                        streak > 0 -> "Keep your $streak-day streak going today."
                        else -> "A few gentle minutes today make a real difference."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("✅", "${today.size}", "done today", Modifier.weight(1f))
                StatTile("⏱️", "$minutes", "minutes today", Modifier.weight(1f))
                StatTile("🔥", "$streak", "day streak", Modifier.weight(1f))
            }
        }
        item { RecoveryEntryCard(repo, onRecovery) }
        item {
            if (plan.isNotEmpty()) {
                BigButton(
                    "Start my plan (${plan.size} exercises)",
                    onClick = { onStartPlan(plan) },
                    icon = Icons.Filled.PlayArrow,
                    minHeight = 80.dp,
                )
            } else {
                BigButton("Choose exercises for my plan", onClick = onBrowse, minHeight = 80.dp)
            }
        }
        if (plan.isNotEmpty()) {
            item {
                SectionTitle("My plan")
                Text(
                    "Change these by opening any exercise and tapping “Add to my plan”.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(plan, key = { "plan_$it" }) { id ->
                val ex = ExerciseLibrary.byId(id) ?: return@items
                ExerciseRow(
                    ex, repo.targetFor(ex), onClick = { onOpen(id) },
                    trailing = if (id in doneToday) DoneMark else null,
                )
            }
        }
        item { SectionTitle("Good for everyone over 60") }
        items(ExerciseLibrary.recommended.filter { it !in plan }, key = { "rec_$it" }) { id ->
            val ex = ExerciseLibrary.byId(id) ?: return@items
            ExerciseRow(ex, repo.targetFor(ex), onClick = { onOpen(id) })
        }
        item { BigOutlinedButton("See all exercises", onClick = onBrowse) }
        item {
            Text(
                "These exercises are general guidance. Follow your physiotherapist's advice, and stop if you feel sharp pain, chest pain or dizziness.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

private val DoneMark: @Composable () -> Unit = {
    Text("✅", style = MaterialTheme.typography.headlineMedium)
}
