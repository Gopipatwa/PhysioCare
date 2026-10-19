@file:OptIn(ExperimentalMaterial3Api::class)

package com.physiocare.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.pose.Pose
import com.physiocare.app.data.AppRepository
import com.physiocare.app.data.Exercise
import com.physiocare.app.data.ExerciseLibrary
import com.physiocare.app.data.Mode
import com.physiocare.app.data.SessionRecord
import com.physiocare.app.pose.CameraPoseView
import com.physiocare.app.pose.PoseMath
import com.physiocare.app.pose.RepCounter
import com.physiocare.app.ui.Coral
import com.physiocare.app.ui.Ink
import com.physiocare.app.ui.Sunflower
import com.physiocare.app.ui.components.BigButton
import com.physiocare.app.ui.components.BigOutlinedButton
import com.physiocare.app.ui.components.ExerciseFigure
import com.physiocare.app.util.Speaker
import kotlinx.coroutines.delay

private enum class Phase { READY, COUNTDOWN, ACTIVE, REST, DONE }

/** Runs one exercise, or a whole plan one exercise after another. */
@Composable
fun SessionScreen(ids: List<String>, repo: AppRepository, speaker: Speaker, onExit: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val ex = ids.getOrNull(index)?.let { ExerciseLibrary.byId(it) }
    if (ex == null) {
        LaunchedEffect(Unit) { onExit() }
        return
    }
    key(index) {
        ExerciseRunner(
            ex = ex,
            repo = repo,
            speaker = speaker,
            position = if (ids.size > 1) "Exercise ${index + 1} of ${ids.size}" else null,
            hasNext = index < ids.lastIndex,
            onNext = { index++ },
            onExit = onExit,
        )
    }
}

@Composable
private fun ExerciseRunner(
    ex: Exercise,
    repo: AppRepository,
    speaker: Speaker,
    position: String?,
    hasNext: Boolean,
    onNext: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val target = remember(ex.id) { repo.targetFor(ex) }
    val totalSets = if (ex.perSide) target.sets * 2 else target.sets

    var phase by remember { mutableStateOf(Phase.READY) }
    var setIndex by remember { mutableIntStateOf(0) }
    var repsInSet by remember { mutableIntStateOf(0) }
    var totalReps by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var useCamera by remember { mutableStateOf(ex.mode == Mode.CAMERA && ex.rule != null) }
    var frontCamera by remember { mutableStateOf(repo.settings.frontCamera) }
    var autoTempo by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }
    var restLeft by remember { mutableIntStateOf(0) }
    var holdLeft by remember { mutableIntStateOf(target.holdSec) }
    var relaxLeft by remember { mutableIntStateOf(0) }
    var activeSeconds by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var showStop by remember { mutableStateOf(false) }
    var savedId by remember { mutableLongStateOf(-1L) }
    var pain by remember { mutableIntStateOf(-1) }
    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val mode = when {
        ex.mode == Mode.HOLD -> Mode.HOLD
        useCamera && ex.rule != null -> Mode.CAMERA
        else -> Mode.MANUAL
    }
    val counter = remember(setIndex, mode) { if (mode == Mode.CAMERA) ex.rule?.let { RepCounter(it) } else null }
    val highlight = remember(ex.id) { ex.rule?.let { PoseMath.ruleLandmarks(it) } ?: emptySet() }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            speaker.stop()
        }
    }

    fun sideName(i: Int) = when {
        !ex.perSide -> ""
        i % 2 == 0 -> "Left side"
        else -> "Right side"
    }

    fun finishSet() {
        if (setIndex + 1 >= totalSets) {
            phase = Phase.DONE
        } else {
            restLeft = if (ex.perSide && setIndex % 2 == 0) 10 else ex.restSec
            phase = Phase.REST
            speaker.say("Well done. Rest now.")
        }
    }

    fun addRep() {
        if (phase != Phase.ACTIVE) return
        repsInSet++
        totalReps++
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (repsInSet >= target.reps) {
            speaker.say("$repsInSet. That set is done!")
            finishSet()
        } else {
            speaker.say("$repsInSet")
        }
    }

    fun startNextSet() {
        setIndex++
        repsInSet = 0
        holdLeft = target.holdSec
        relaxLeft = 0
        progress = 0f
        phase = Phase.ACTIVE
        speaker.say("Begin. ${sideName(setIndex)}")
    }

    fun begin() {
        phase = Phase.COUNTDOWN
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamPermission = granted
        if (!granted) useCamera = false
        begin()
    }

    // ---- Phase transitions ----
    LaunchedEffect(phase) {
        when (phase) {
            Phase.COUNTDOWN -> {
                speaker.say(if (ex.perSide) "Get ready. Start with your left side." else "Get ready.")
                delay(1800)
                for (i in 3 downTo 1) {
                    countdown = i
                    speaker.say("$i")
                    delay(1000)
                }
                if (mode != Mode.HOLD) speaker.say("Begin")
                phase = Phase.ACTIVE
            }
            Phase.REST -> {
                while (restLeft > 0) {
                    delay(1000)
                    if (!paused) restLeft--
                    if (restLeft == 3) speaker.say("Get ready")
                }
                startNextSet()
            }
            Phase.DONE -> {
                speaker.say(if (totalReps > 0) "Exercise complete. Great job!" else "Stopped.")
                if (totalReps > 0 && savedId < 0) {
                    savedId = repo.addSession(
                        SessionRecord(
                            id = System.currentTimeMillis(),
                            exerciseId = ex.id,
                            time = System.currentTimeMillis(),
                            reps = totalReps,
                            sets = (setIndex + 1).coerceAtMost(totalSets),
                            holdSec = if (ex.mode == Mode.HOLD) target.holdSec else 0,
                            durationSec = activeSeconds,
                            camera = mode == Mode.CAMERA,
                            completed = totalReps >= target.reps * totalSets,
                        )
                    )
                }
            }
            else -> Unit
        }
    }

    // ---- Active-time clock ----
    LaunchedEffect(phase, paused) {
        while (phase == Phase.ACTIVE && !paused) {
            delay(1000)
            activeSeconds++
        }
    }

    // ---- Timed holds (and guided breathing) ----
    LaunchedEffect(phase, paused, setIndex) {
        if (phase != Phase.ACTIVE || paused || mode != Mode.HOLD) return@LaunchedEffect
        val breathing = ex.breathCycle > 0
        if (relaxLeft == 0) speaker.say(if (breathing) "Breathe in" else "Hold")
        while (true) {
            delay(1000)
            if (relaxLeft > 0) {
                relaxLeft--
                if (relaxLeft == 0) {
                    holdLeft = target.holdSec
                    speaker.say(if (breathing) "Breathe in" else "Hold")
                }
                continue
            }
            holdLeft--
            val elapsed = target.holdSec - holdLeft
            if (breathing) {
                val c = ex.breathCycle
                val inhale = maxOf(2, c * 2 / 5)
                if (holdLeft > 0) {
                    when (elapsed % c) {
                        0 -> speaker.say("Breathe in")
                        inhale -> speaker.say("Breathe out")
                    }
                }
            } else if (holdLeft in 1..3) {
                speaker.say("$holdLeft")
            }
            if (holdLeft <= 0) {
                repsInSet++
                totalReps++
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (repsInSet >= target.reps) {
                    speaker.say("And relax. That set is done!")
                    finishSet()
                    break
                }
                speaker.say("Relax")
                relaxLeft = 5
            }
        }
    }

    // ---- Voice tempo for tap-counted exercises ----
    LaunchedEffect(phase, paused, autoTempo, setIndex) {
        if (phase != Phase.ACTIVE || paused || !autoTempo || mode != Mode.MANUAL) return@LaunchedEffect
        delay(800)
        while (true) {
            speaker.say("Up")
            delay(2200)
            speaker.say("And down")
            delay(2200)
            addRep()
            delay(1100)
        }
    }

    // ---- Camera frames ----
    val onPose: (Pose?) -> Unit = { pose ->
        val rule = ex.rule
        if (pose == null || pose.allPoseLandmarks.isEmpty()) {
            feedback = "I can't see you yet. Move back so your whole body is in the picture."
        } else if (rule != null && counter != null) {
            val (l, r) = PoseMath.ruleAngles(pose, rule)
            if (l == null && r == null) {
                feedback = "Make sure the camera can see your ${rule.b.label}."
            } else if (phase == Phase.ACTIVE && !paused) {
                if (counter.update(l, r)) addRep()
                progress = counter.progress
                feedback = when {
                    counter.reached -> "Good! Now slowly come back."
                    counter.progress > 0.6f -> "Almost there – a little more."
                    counter.progress > 0.1f -> "Keep going…"
                    else -> "Ready. Start the movement."
                }
            } else {
                feedback = "I can see you. Press Start when you are ready."
            }
        }
    }

    fun requestStop() {
        if (phase == Phase.READY || phase == Phase.DONE) onExit() else showStop = true
    }
    BackHandler { requestStop() }

    // ───────────────────────── UI ─────────────────────────
    val showCamera = mode == Mode.CAMERA && hasCamPermission && phase != Phase.DONE
    val setLabel = buildString {
        append("Set ${if (ex.perSide) setIndex / 2 + 1 else setIndex + 1} of ${target.sets}")
        if (ex.perSide) append(" – ${sideName(setIndex)}")
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { requestStop() }, modifier = Modifier.heightIn(min = 52.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(if (phase == Phase.READY || phase == Phase.DONE) "Back" else "Stop", style = MaterialTheme.typography.labelLarge)
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(ex.name, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                if (position != null) {
                    Text(position, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (showCamera) {
            // Camera layout: live picture on top, big controls underneath.
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
            ) {
                CameraPoseView(frontCamera, highlight, onPose, Modifier.fillMaxSize())
                if (feedback.isNotEmpty() && phase != Phase.REST) {
                    Text(
                        feedback,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                if (phase == Phase.ACTIVE) {
                    Text(
                        "$repsInSet",
                        style = MaterialTheme.typography.displayLarge,
                        color = Ink,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Sunflower)
                            .padding(horizontal = 22.dp),
                    )
                }
                if (phase == Phase.COUNTDOWN) BigOverlayNumber("$countdown")
                if (phase == Phase.REST) RestOverlay(restLeft, sideName(setIndex + 1), onSkip = { startNextSet() })
                TextButton(
                    onClick = { frontCamera = !frontCamera },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) { Text("🔄 Switch camera", color = Color.White, style = MaterialTheme.typography.labelMedium) }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (phase) {
                    Phase.READY -> {
                        Text(ex.rule?.hint ?: "", style = MaterialTheme.typography.bodyLarge)
                        BigButton("Start", onClick = { begin() }, icon = Icons.Filled.PlayArrow, minHeight = 76.dp)
                        CameraToggle(useCamera) { useCamera = it }
                    }
                    Phase.ACTIVE -> {
                        Text(
                            "$repsInSet of ${target.reps}   ·   $setLabel",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        ActiveControls(paused, onPause = { paused = !paused }, onPlusOne = { addRep() }, onStop = { requestStop() })
                    }
                    else -> Text(setLabel, style = MaterialTheme.typography.titleLarge)
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (phase) {
                    Phase.READY -> ReadyContent(
                        ex = ex,
                        mode = mode,
                        useCamera = useCamera,
                        hasCamPermission = hasCamPermission,
                        autoTempo = autoTempo,
                        onCamera = { useCamera = it },
                        onTempo = { autoTempo = it },
                        onStart = {
                            if (useCamera && ex.rule != null && !hasCamPermission) {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                begin()
                            }
                        },
                    )
                    Phase.COUNTDOWN -> {
                        FigureCard(ex)
                        Text(
                            "$countdown",
                            style = MaterialTheme.typography.displayLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text("Get ready…", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    Phase.ACTIVE -> {
                        FigureCard(ex)
                        Text(setLabel, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        if (mode == Mode.HOLD) {
                            HoldDisplay(
                                holdLeft = holdLeft,
                                holdTotal = target.holdSec,
                                relaxLeft = relaxLeft,
                                done = repsInSet,
                                total = target.reps,
                                breathing = ex.breathCycle > 0,
                                paused = paused,
                            )
                        } else {
                            TapCounter(repsInSet, target.reps, autoTempo, onTap = { addRep() })
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Voice says “up… and down” and counts", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Switch(checked = autoTempo, onCheckedChange = { autoTempo = it })
                            }
                        }
                        ActiveControls(paused, onPause = { paused = !paused }, onPlusOne = null, onStop = { requestStop() })
                    }
                    Phase.REST -> {
                        FigureCard(ex)
                        RestPanel(restLeft, sideName(setIndex + 1), onSkip = { startNextSet() })
                    }
                    Phase.DONE -> DoneContent(
                        totalReps = totalReps,
                        minutes = (activeSeconds + 59) / 60,
                        saved = savedId >= 0,
                        pain = pain,
                        onPain = { v ->
                            pain = v
                            if (savedId >= 0) repo.updatePain(savedId, v)
                        },
                        hasNext = hasNext,
                        onNext = onNext,
                        onFinish = onExit,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showStop) {
        AlertDialog(
            onDismissRequest = { showStop = false },
            title = { Text("Stop this exercise?", style = MaterialTheme.typography.headlineMedium) },
            text = { Text("What you have done so far will be saved.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                Button(
                    onClick = {
                        showStop = false
                        phase = Phase.DONE
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral),
                    modifier = Modifier.heightIn(min = 56.dp),
                ) { Text("Stop", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStop = false }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("Keep going", style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

// ───────────────────────── Pieces ─────────────────────────

@Composable
private fun FigureCard(ex: Exercise) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(10.dp),
    ) {
        ExerciseFigure(ex.guide, Modifier.fillMaxWidth().height(190.dp), durationMs = 2000)
    }
}

@Composable
private fun CameraToggle(on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Count with the camera", style = MaterialTheme.typography.titleMedium)
            Text(
                if (on) "The camera watches and counts for you. Nothing is recorded or sent anywhere."
                else "You tap the big button to count instead.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = on, onCheckedChange = onChange)
    }
}

@Composable
private fun ReadyContent(
    ex: Exercise,
    mode: Mode,
    useCamera: Boolean,
    hasCamPermission: Boolean,
    autoTempo: Boolean,
    onCamera: (Boolean) -> Unit,
    onTempo: (Boolean) -> Unit,
    onStart: () -> Unit,
) {
    FigureCard(ex)
    ex.steps.forEachIndexed { i, s -> Text("${i + 1}.  $s", style = MaterialTheme.typography.bodyLarge) }
    if (ex.rule != null) {
        CameraToggle(useCamera, onCamera)
        if (useCamera && !hasCamPermission) {
            Text(
                "When you press Start, Android will ask to use the camera. Choose “Allow”.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (mode == Mode.MANUAL) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Voice counts for me", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Follow a slow “up… and down” rhythm. No tapping needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = autoTempo, onCheckedChange = onTempo)
        }
    }
    BigButton("Start", onClick = onStart, icon = Icons.Filled.PlayArrow, minHeight = 80.dp)
}

@Composable
private fun TapCounter(count: Int, total: Int, autoTempo: Boolean, onTap: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .clickable(onClick = onTap)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("$count", style = MaterialTheme.typography.displayLarge, color = Ink)
        Text("of $total", style = MaterialTheme.typography.titleLarge, color = Ink)
        Text(
            if (autoTempo) "Counting with your voice coach" else "Tap here after each one",
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun HoldDisplay(holdLeft: Int, holdTotal: Int, relaxLeft: Int, done: Int, total: Int, breathing: Boolean, paused: Boolean) {
    val relaxing = relaxLeft > 0
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { if (relaxing) relaxLeft / 5f else holdLeft / holdTotal.toFloat().coerceAtLeast(1f) },
            modifier = Modifier.size(220.dp),
            strokeWidth = 18.dp,
            color = if (relaxing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (relaxing) "$relaxLeft" else "$holdLeft", style = MaterialTheme.typography.displayLarge)
            Text(
                when {
                    paused -> "Paused"
                    relaxing -> "Relax"
                    breathing -> "Breathe slowly"
                    else -> "Hold"
                },
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
    Text(
        if (breathing) "Round ${minOf(done + 1, total)} of $total" else "Hold ${minOf(done + 1, total)} of $total",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ActiveControls(paused: Boolean, onPause: () -> Unit, onPlusOne: (() -> Unit)?, onStop: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BigOutlinedButton(if (paused) "▶ Resume" else "⏸ Pause", onClick = onPause, modifier = Modifier.weight(1f))
        if (onPlusOne != null) {
            BigOutlinedButton("+1", onClick = onPlusOne, modifier = Modifier.weight(0.6f))
        }
        BigOutlinedButton("■ Stop", onClick = onStop, modifier = Modifier.weight(1f), color = Coral)
    }
}

@Composable
private fun BigOverlayNumber(text: String) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.displayLarge, color = Color.White)
    }
}

@Composable
private fun RestOverlay(secondsLeft: Int, nextSide: String, onSkip: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rest", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text("$secondsLeft", style = MaterialTheme.typography.displayLarge, color = Sunflower)
            if (nextSide.isNotEmpty()) Text("Next: $nextSide", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Button(onClick = onSkip, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Skip rest", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun RestPanel(secondsLeft: Int, nextSide: String, onSkip: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Rest", style = MaterialTheme.typography.headlineLarge)
        Text("$secondsLeft", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Text("Breathe easy and sip some water.", style = MaterialTheme.typography.bodyLarge)
        if (nextSide.isNotEmpty()) Text("Next: $nextSide", style = MaterialTheme.typography.titleLarge)
    }
    BigButton("Skip rest", onClick = onSkip)
}

private val PAIN = listOf(0 to "😀", 2 to "🙂", 4 to "😐", 6 to "😕", 8 to "😣", 10 to "😫")

@Composable
private fun DoneContent(
    totalReps: Int,
    minutes: Int,
    saved: Boolean,
    pain: Int,
    onPain: (Int) -> Unit,
    hasNext: Boolean,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Spacer(Modifier.height(8.dp))
    Text(if (totalReps > 0) "🎉" else "👍", style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(
        if (totalReps > 0) "Well done!" else "No problem",
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        if (totalReps > 0) "You did $totalReps in about $minutes minute${if (minutes == 1) "" else "s"}. It's saved in My Progress."
        else "Nothing was recorded this time. You can try again whenever you like.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    if (saved) {
        Text("How does it feel now?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        Text("0 = no pain, 10 = worst pain", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            PAIN.forEach { (v, face) ->
                val selected = pain == v
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(
                            2.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp),
                        )
                        .clickable { onPain(v) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(face, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "$v",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        if (pain >= 6) {
            Text(
                "If pain is worse than usual, rest and talk to your physiotherapist before doing this again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    if (hasNext) {
        BigButton("Next exercise", onClick = onNext, icon = Icons.Filled.PlayArrow, minHeight = 76.dp)
        BigOutlinedButton("Finish for today", onClick = onFinish)
    } else {
        BigButton("Finish", onClick = onFinish, minHeight = 76.dp)
    }
}
