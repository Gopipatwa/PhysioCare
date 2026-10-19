@file:OptIn(ExperimentalMaterial3Api::class)

package com.physiocare.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.physiocare.app.data.AppRepository
import com.physiocare.app.ui.components.BigOutlinedButton
import com.physiocare.app.ui.components.SectionTitle
import com.physiocare.app.ui.components.Stepper
import com.physiocare.app.util.Reminder
import com.physiocare.app.util.Speaker

@Composable
fun SettingsScreen(repo: AppRepository, speaker: Speaker) {
    val context = LocalContext.current
    val s = repo.settings
    var confirmClear by remember { mutableStateOf(false) }

    fun setReminder(on: Boolean, hour: Int = s.reminderHour, minute: Int = s.reminderMinute) {
        val enabled = on && Reminder.canNotify(context)
        repo.updateSettings(repo.settings.copy(reminderOn = enabled, reminderHour = hour, reminderMinute = minute))
        if (enabled) Reminder.schedule(context, hour, minute) else Reminder.cancel(context)
    }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        setReminder(granted)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        SectionTitle("Your name")
        OutlinedTextField(
            value = s.name,
            onValueChange = { repo.updateSettings(repo.settings.copy(name = it.take(30))) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = { Text("So we can greet you", style = MaterialTheme.typography.bodyLarge) },
            shape = RoundedCornerShape(18.dp),
        )

        SectionTitle("Text size")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1f to "Normal", 1.15f to "Large", 1.3f to "Largest").forEach { (scale, label) ->
                Choice(label, selected = s.textScale == scale, modifier = Modifier.weight(1f)) {
                    repo.updateSettings(repo.settings.copy(textScale = scale))
                }
            }
        }

        SectionTitle("Voice coach")
        ToggleRow(
            "Speak instructions and counts",
            "Uses your phone's own voice. Works without internet.",
            s.voice,
        ) {
            repo.updateSettings(repo.settings.copy(voice = it))
            speaker.enabled = it
        }
        BigOutlinedButton("🔊  Test the voice", onClick = {
            speaker.enabled = true
            speaker.say("Hello${if (s.name.isNotBlank()) " ${s.name}" else ""}. I will count with you. One. Two. Three.")
            speaker.enabled = repo.settings.voice
        })

        SectionTitle("Camera")
        Text(
            "Which camera to use for counting. Front lets you see yourself on the screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice("Front", selected = s.frontCamera, modifier = Modifier.weight(1f)) {
                repo.updateSettings(repo.settings.copy(frontCamera = true))
            }
            Choice("Back", selected = !s.frontCamera, modifier = Modifier.weight(1f)) {
                repo.updateSettings(repo.settings.copy(frontCamera = false))
            }
        }

        SectionTitle("Daily reminder")
        ToggleRow(
            "Remind me to exercise",
            "A notification once a day at the time below.",
            s.reminderOn,
        ) { on ->
            if (on && Build.VERSION.SDK_INT >= 33 && !Reminder.canNotify(context)) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                setReminder(on)
            }
        }
        Text(
            "Reminder time: " +
                (if (s.reminderHour % 12 == 0) 12 else s.reminderHour % 12) + ":" +
                s.reminderMinute.toString().padStart(2, '0') +
                (if (s.reminderHour < 12) " AM" else " PM"),
            style = MaterialTheme.typography.titleLarge,
        )
        Stepper("Hour", s.reminderHour, { setReminder(s.reminderOn, hour = it) }, min = 0, max = 23)
        Stepper("Minutes", s.reminderMinute, { setReminder(s.reminderOn, minute = it) }, min = 0, max = 45, step = 15)

        SectionTitle("My data")
        Text(
            "Everything stays on this phone. No account, no internet, nothing is uploaded. " +
                "Camera pictures are only used live for counting and are never saved.",
            style = MaterialTheme.typography.bodyMedium,
        )
        BigOutlinedButton("Delete all my history", onClick = { confirmClear = true }, color = MaterialTheme.colorScheme.error)

        SectionTitle("About")
        Text(
            "PhysioCare gives general exercise guidance and is not a substitute for medical advice. " +
                "Check with your doctor or physiotherapist before starting, especially after surgery, a fall, or with heart or lung conditions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete all history?", style = MaterialTheme.typography.headlineMedium) },
            text = { Text("Your streak and progress charts will start again from zero. This can't be undone.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                Button(onClick = {
                    repo.clearHistory()
                    confirmClear = false
                }) { Text("Delete", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmClear = false }) { Text("Keep it", style = MaterialTheme.typography.labelLarge) }
            },
        )
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Text(
        (if (selected) "✓ " else "") + label,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        textAlign = TextAlign.Center,
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 6.dp),
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
