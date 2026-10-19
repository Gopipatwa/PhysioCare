@file:OptIn(ExperimentalMaterial3Api::class)

package com.physiocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.physiocare.app.data.Ability
import com.physiocare.app.data.AppRepository
import com.physiocare.app.data.Condition
import com.physiocare.app.data.ExerciseLibrary
import com.physiocare.app.data.Recovery
import com.physiocare.app.data.RecoveryProfile
import com.physiocare.app.data.Side
import com.physiocare.app.ui.SunflowerSoft
import com.physiocare.app.ui.components.BigButton
import com.physiocare.app.ui.components.BigOutlinedButton
import com.physiocare.app.ui.components.ExerciseRow
import com.physiocare.app.ui.components.ScreenHeader
import com.physiocare.app.ui.components.SectionTitle

private enum class Step { CONDITION, SIDE, ABILITY, RESULT }

@Composable
fun RecoveryScreen(
    repo: AppRepository,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onStart: (List<String>) -> Unit,
) {
    val saved = repo.recovery
    var step by remember { mutableStateOf(if (saved == null) Step.CONDITION else Step.RESULT) }
    var condition by remember { mutableStateOf(saved?.condition) }
    var side by remember { mutableStateOf(saved?.side) }

    fun back() {
        step = when (step) {
            Step.CONDITION -> if (saved != null) Step.RESULT else return onBack()
            Step.SIDE -> Step.CONDITION
            Step.ABILITY -> if (condition?.oneSided == true) Step.SIDE else Step.CONDITION
            Step.RESULT -> return onBack()
        }
    }

    when (step) {
        Step.CONDITION -> Question(
            title = "What happened?",
            subtitle = "Step 1 of 3. Choose the one that matches best.",
            onBack = { back() },
        ) {
            Condition.values().forEach { c ->
                OptionCard(c.emoji, c.title, c.blurb, selected = condition == c) {
                    condition = c
                    step = if (c.oneSided) Step.SIDE else Step.ABILITY
                }
            }
            Text(
                "Not listed? Ask your physiotherapist which exercises are safe, then pick them from the Exercises tab.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Step.SIDE -> Question(
            title = "Which side is affected?",
            subtitle = "Step 2 of 3. The weaker side of the ${if (condition == Condition.BELLS) "face" else "body"}.",
            onBack = { back() },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Side.values().forEach { s ->
                    OptionCard(
                        if (s == Side.LEFT) "⬅️" else "➡️", "${s.label} side", null,
                        selected = side == s, modifier = Modifier.weight(1f),
                    ) {
                        side = s
                        step = Step.ABILITY
                    }
                }
            }
        }

        Step.ABILITY -> Question(
            title = "How much can you move ${condition?.bodyPart ?: "it"}?",
            subtitle = "Step 3 of 3. You can change this as you get stronger.",
            onBack = { back() },
        ) {
            val emojis = mapOf(Ability.NONE to "👐", Ability.LITTLE to "🤏", Ability.WEAK to "💪")
            Ability.values().forEach { a ->
                OptionCard(emojis[a] ?: "", a.title, a.blurb, selected = saved?.ability == a) {
                    val c = condition ?: return@OptionCard
                    repo.updateRecovery(RecoveryProfile(c, if (c.oneSided) side else null, a))
                    step = Step.RESULT
                }
            }
        }

        Step.RESULT -> {
            val p = repo.recovery
            if (p == null) {
                step = Step.CONDITION
                return
            }
            Result(
                profile = p,
                repo = repo,
                onBack = onBack,
                onOpen = onOpen,
                onStart = onStart,
                onChange = { step = Step.CONDITION },
            )
        }
    }
}

@Composable
private fun Question(title: String, subtitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(title, onBack, subtitle = subtitle) }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}

@Composable
private fun OptionCard(
    emoji: String,
    title: String,
    blurb: String?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(if (selected) 3.dp else 1.dp, border, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (blurb != null) {
                Text(blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Result(
    profile: RecoveryProfile,
    repo: AppRepository,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onStart: (List<String>) -> Unit,
    onChange: () -> Unit,
) {
    val now = Recovery.exercisesFor(profile.condition, profile.ability)
    val later = Recovery.nextStep(profile.condition, profile.ability)
    var confirmPlan by remember { mutableStateOf(false) }
    val sideText = profile.side?.let { " – ${it.label.lowercase()} side" } ?: ""

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                "${profile.condition.emoji}  ${profile.condition.title}$sideText",
                onBack,
                subtitle = "Movement: ${profile.ability.title}",
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("Start these exercises (${now.size})", onClick = { onStart(now) }, icon = Icons.Filled.PlayArrow, minHeight = 76.dp)
                BigOutlinedButton("Make these my daily plan", onClick = { confirmPlan = true })
                if (profile.side != null) {
                    Text(
                        if (profile.condition == Condition.BELLS) {
                            "Focus on the ${profile.side.label.lowercase()} side of your face, and watch both sides in a mirror."
                        } else {
                            "Focus on your ${profile.side.label.lowercase()} side. When an exercise says “each side”, you can do extra sets on the ${profile.side.label.lowercase()}."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (Recovery.smallMoves(profile)) {
                    Text(
                        "📷 Camera counting is set to count small movements for you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SunflowerSoft)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("⚠️ Stay safe", style = MaterialTheme.typography.titleMedium)
                    Recovery.safety(profile.condition).forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
                    Text(
                        "Use these only with your doctor's or physiotherapist's agreement.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                SectionTitle("Your exercises now")
            }
        }
        items(now, key = { "now_$it" }) { id ->
            val ex = ExerciseLibrary.byId(id) ?: return@items
            Column(Modifier.padding(horizontal = 16.dp)) { ExerciseRow(ex, repo.targetFor(ex), onClick = { onOpen(id) }) }
        }
        if (later.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionTitle("When you get stronger")
                    Text(
                        "Move on to these when the ones above feel easy. Update your answers to switch.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(later, key = { "later_$it" }) { id ->
                val ex = ExerciseLibrary.byId(id) ?: return@items
                Column(Modifier.padding(horizontal = 16.dp)) { ExerciseRow(ex, repo.targetFor(ex), onClick = { onOpen(id) }) }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BigOutlinedButton("Change my answers", onClick = onChange)
                BigOutlinedButton(
                    "Remove my recovery answers",
                    onClick = {
                        repo.updateRecovery(null)
                        onBack()
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (confirmPlan) {
        AlertDialog(
            onDismissRequest = { confirmPlan = false },
            title = { Text("Replace my daily plan?", style = MaterialTheme.typography.headlineMedium) },
            text = {
                Text(
                    "Your daily plan on the Home screen will become these ${now.size} exercises. Your history is kept.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                Button(onClick = {
                    repo.setPlan(now)
                    confirmPlan = false
                }) { Text("Replace", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmPlan = false }) { Text("Cancel", style = MaterialTheme.typography.labelLarge) }
            },
        )
    }
}

/** Entry point shown on Home and Exercises. */
@Composable
fun RecoveryEntryCard(repo: AppRepository, onClick: () -> Unit) {
    val p = repo.recovery
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(p?.condition?.emoji ?: "🧠", style = MaterialTheme.typography.displayMedium)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            if (p == null) {
                Text("Recovering from a stroke or paralysis?", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Answer 3 questions to see exercises that suit you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                Text("My recovery exercises", style = MaterialTheme.typography.titleMedium)
                Text(
                    p.condition.title + (p.side?.let { " – ${it.label.lowercase()} side" } ?: "") + ". Movement: ${p.ability.title.lowercase()}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
