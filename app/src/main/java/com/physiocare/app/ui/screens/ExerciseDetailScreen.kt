@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.physiocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.physiocare.app.data.AppRepository
import com.physiocare.app.data.ExerciseLibrary
import com.physiocare.app.data.Mode
import com.physiocare.app.data.summary
import com.physiocare.app.ui.SunflowerSoft
import com.physiocare.app.ui.components.BigButton
import com.physiocare.app.ui.components.BigOutlinedButton
import com.physiocare.app.ui.components.ExerciseFigure
import com.physiocare.app.ui.components.Pill
import com.physiocare.app.ui.components.ScreenHeader
import com.physiocare.app.ui.components.SectionTitle
import com.physiocare.app.ui.components.Stepper
import com.physiocare.app.util.Speaker

@Composable
fun ExerciseDetailScreen(
    id: String,
    repo: AppRepository,
    speaker: Speaker,
    onBack: () -> Unit,
    onStart: (String) -> Unit,
) {
    val ex = ExerciseLibrary.byId(id) ?: return
    val target = repo.targetFor(ex)
    val inPlan = repo.inPlan(id)

    DisposableEffect(id) { onDispose { speaker.stop() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(ex.name, onBack, subtitle = "${ex.category.emoji} ${ex.category.title}")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(12.dp),
            ) {
                ExerciseFigure(ex.guide, Modifier.fillMaxWidth().height(250.dp), durationMs = 2000)
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(ex.summary(target))
                Pill(ex.level.label)
                if (ex.cameraCapable) {
                    Pill("📷 Camera counts for you", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                }
                if (ex.mode == Mode.HOLD) Pill("⏱️ Timed holds")
            }

            Text("Helps with: ${ex.goodFor}", style = MaterialTheme.typography.bodyLarge)

            BigButton("Start exercise", onClick = { onStart(id) }, icon = Icons.Filled.PlayArrow, minHeight = 76.dp)
            BigOutlinedButton(
                "🔊  Read the steps aloud",
                onClick = {
                    val text = buildString {
                        append(ex.name).append(". ")
                        ex.steps.forEachIndexed { i, s -> append("Step ${i + 1}. ").append(s).append(" ") }
                        ex.tips.forEach { append(it).append(" ") }
                    }
                    speaker.say(text)
                },
            )

            SectionTitle("How to do it")
            ex.steps.forEachIndexed { i, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 14.dp).weight(1f))
                }
            }

            if (ex.tips.isNotEmpty()) {
                SectionTitle("Tips")
                ex.tips.forEach { Text("💡 $it", style = MaterialTheme.typography.bodyLarge) }
            }
            ex.rule?.let { Text("📷 ${it.hint}", style = MaterialTheme.typography.bodyLarge) }

            Text(
                "⚠️ Stop if you feel sharp pain, chest pain, dizziness or shortness of breath. Mild stretching discomfort is normal.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SunflowerSoft)
                    .padding(16.dp),
            )

            SectionTitle("My target")
            Text(
                "Your physiotherapist may give you different numbers – set them here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Stepper(
                if (ex.mode == Mode.HOLD) "Holds" else "Repetitions",
                target.reps, { repo.setTarget(ex, target.copy(reps = it)) }, min = 1, max = 50,
            )
            if (ex.mode == Mode.HOLD) {
                Stepper("Seconds each", target.holdSec, { repo.setTarget(ex, target.copy(holdSec = it)) }, min = 5, max = 180, step = 5, unit = "s")
            }
            Stepper(
                if (ex.perSide) "Sets (each side)" else "Sets",
                target.sets, { repo.setTarget(ex, target.copy(sets = it)) }, min = 1, max = 6,
            )
            if (repo.isCustomTarget(ex)) {
                TextButton(onClick = { repo.setTarget(ex, ex.defaultTarget) }) {
                    Text("Reset to the usual amount", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (inPlan) {
                BigOutlinedButton("Remove from my plan", onClick = { repo.togglePlan(id) }, color = MaterialTheme.colorScheme.error)
            } else {
                BigButton(
                    "＋  Add to my plan",
                    onClick = { repo.togglePlan(id) },
                    container = MaterialTheme.colorScheme.secondary,
                    content = MaterialTheme.colorScheme.onSecondary,
                )
            }
            Text(
                if (inPlan) "This exercise is in your daily plan." else "Your plan is the list you start from the Home screen.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
