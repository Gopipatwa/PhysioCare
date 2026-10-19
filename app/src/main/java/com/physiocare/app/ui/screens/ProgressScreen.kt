@file:OptIn(ExperimentalMaterial3Api::class)

package com.physiocare.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.physiocare.app.data.AppRepository
import com.physiocare.app.data.ExerciseLibrary
import com.physiocare.app.data.SessionRecord
import com.physiocare.app.ui.components.BigButton
import com.physiocare.app.ui.components.SectionTitle
import com.physiocare.app.ui.components.StatTile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayHeader = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ProgressScreen(repo: AppRepository) {
    val context = LocalContext.current
    var toDelete by remember { mutableStateOf<SessionRecord?>(null) }
    val history = repo.history.toList()
    val week = repo.last7Days()
    val rated = history.filter { it.pain >= 0 }.take(10).reversed()
    val grouped = history.groupBy { AppRepository.dayOf(it.time) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("My progress", style = MaterialTheme.typography.headlineLarge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("🔥", "${repo.streak()}", "day streak", Modifier.weight(1f))
                StatTile("📅", "${week.sumOf { it.second }}", "this week", Modifier.weight(1f))
                StatTile("💪", "${repo.totalReps()}", "total reps", Modifier.weight(1f))
            }
        }
        item {
            Panel {
                Text("Exercises done in the last 7 days", style = MaterialTheme.typography.titleMedium)
                WeekBars(week)
            }
        }
        if (rated.size >= 2) {
            item {
                Panel {
                    Text("Pain after exercise (latest on the right)", style = MaterialTheme.typography.titleMedium)
                    PainBars(rated)
                    Text(
                        "Lower is better. Share this with your physiotherapist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (history.isNotEmpty()) {
            item {
                BigButton(
                    "Send report to my physio or family",
                    icon = Icons.Filled.Share,
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "My PhysioCare exercise report")
                            putExtra(Intent.EXTRA_TEXT, repo.buildReport())
                        }
                        context.startActivity(Intent.createChooser(send, "Send report with…"))
                    },
                )
            }
        }
        if (history.isEmpty()) {
            item {
                Text(
                    "Nothing here yet. Finish your first exercise and it will appear here, with your streak and a weekly chart.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        grouped.forEach { (day, list) ->
            item(key = "h_$day") {
                SectionTitle(
                    when (day) {
                        LocalDate.now() -> "Today"
                        LocalDate.now().minusDays(1) -> "Yesterday"
                        else -> day.format(dayHeader)
                    }
                )
            }
            items(list, key = { it.id }) { r -> HistoryRow(r, onDelete = { toDelete = r }) }
        }
    }

    toDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this entry?", style = MaterialTheme.typography.headlineMedium) },
            text = { Text(ExerciseLibrary.byId(r.exerciseId)?.name ?: "", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                Button(onClick = {
                    repo.deleteSession(r.id)
                    toDelete = null
                }) { Text("Delete", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                OutlinedButton(onClick = { toDelete = null }) { Text("Keep", style = MaterialTheme.typography.labelLarge) }
            },
        )
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
private fun WeekBars(days: List<Pair<LocalDate, Int>>) {
    val max = (days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(170.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { (d, n) ->
            val today = d == LocalDate.now()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$n", style = MaterialTheme.typography.labelMedium)
                Box(
                    Modifier
                        .width(30.dp)
                        .height((110f * n / max).coerceAtLeast(4f).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (today) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary),
                )
                Text(
                    d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun PainBars(rated: List<SessionRecord>) {
    Row(
        Modifier.fillMaxWidth().height(130.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        rated.forEach { r ->
            val c = when {
                r.pain <= 2 -> Color(0xFF2F7D4F)
                r.pain <= 5 -> Color(0xFFE0A100)
                else -> Color(0xFFC2412D)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${r.pain}", style = MaterialTheme.typography.labelMedium)
                Box(
                    Modifier
                        .width(20.dp)
                        .height((9f * r.pain).coerceAtLeast(4f).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(c),
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(r: SessionRecord, onDelete: () -> Unit) {
    val ex = ExerciseLibrary.byId(r.exerciseId)
    val time = Instant.ofEpochMilli(r.time).atZone(ZoneId.systemDefault()).format(timeFmt)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(ex?.category?.emoji ?: "•", style = MaterialTheme.typography.headlineMedium)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(ex?.name ?: r.exerciseId, style = MaterialTheme.typography.titleMedium)
            val parts = buildList {
                add(time)
                add(if (r.holdSec > 0) "${r.reps} × ${r.holdSec}s" else "${r.reps} reps")
                add("${(r.durationSec + 59) / 60} min")
                if (r.camera) add("📷")
                if (!r.completed) add("stopped early")
                if (r.pain >= 0) add("pain ${r.pain}")
            }
            Text(
                parts.joinToString("   "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.outline)
        }
    }
}
