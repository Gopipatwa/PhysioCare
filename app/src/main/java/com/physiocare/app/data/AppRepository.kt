package com.physiocare.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

data class SessionRecord(
    val id: Long,
    val exerciseId: String,
    val time: Long,
    val reps: Int,
    val sets: Int,
    val holdSec: Int,
    val durationSec: Int,
    val camera: Boolean,
    val completed: Boolean,
    val pain: Int = -1,
)

data class Settings(
    val name: String = "",
    val textScale: Float = 1f,
    val voice: Boolean = true,
    val frontCamera: Boolean = true,
    val reminderOn: Boolean = false,
    val reminderHour: Int = 10,
    val reminderMinute: Int = 0,
    val disclaimerAccepted: Boolean = false,
)

/**
 * Everything is stored on the phone only: history in a small JSON file,
 * settings / plan / custom targets in SharedPreferences. No account, no internet.
 */
class AppRepository private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun get(context: Context): AppRepository =
            instance ?: synchronized(this) {
                instance ?: AppRepository(context.applicationContext).also { instance = it }
            }

        fun dayOf(time: Long): LocalDate =
            Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    private val prefs = context.getSharedPreferences("physiocare", Context.MODE_PRIVATE)
    private val historyFile = File(context.filesDir, "history.json")
    private val io = Executors.newSingleThreadExecutor()

    val history = mutableStateListOf<SessionRecord>()
    val plan = mutableStateListOf<String>()
    private val overrides = mutableStateMapOf<String, Target>()

    var settings by mutableStateOf(Settings())
        private set

    /** Answers from the "Paralysis & stroke recovery" questions (null = not set up). */
    var recovery by mutableStateOf<RecoveryProfile?>(null)
        private set

    init {
        loadRecovery()
        loadSettings()
        loadPlan()
        loadOverrides()
        loadHistory()
    }

    // ───────────── Settings ─────────────
    private fun loadSettings() {
        settings = Settings(
            name = prefs.getString("name", "") ?: "",
            textScale = prefs.getFloat("textScale", 1f),
            voice = prefs.getBoolean("voice", true),
            frontCamera = prefs.getBoolean("frontCamera", true),
            reminderOn = prefs.getBoolean("reminderOn", false),
            reminderHour = prefs.getInt("reminderHour", 10),
            reminderMinute = prefs.getInt("reminderMinute", 0),
            disclaimerAccepted = prefs.getBoolean("disclaimer", false),
        )
    }

    fun updateSettings(s: Settings) {
        settings = s
        prefs.edit()
            .putString("name", s.name)
            .putFloat("textScale", s.textScale)
            .putBoolean("voice", s.voice)
            .putBoolean("frontCamera", s.frontCamera)
            .putBoolean("reminderOn", s.reminderOn)
            .putInt("reminderHour", s.reminderHour)
            .putInt("reminderMinute", s.reminderMinute)
            .putBoolean("disclaimer", s.disclaimerAccepted)
            .apply()
    }

    // ───────────── Recovery profile ─────────────
    private fun loadRecovery() {
        val c = prefs.getString("rec_condition", null) ?: return
        recovery = try {
            RecoveryProfile(
                condition = Condition.valueOf(c),
                side = prefs.getString("rec_side", null)?.let { Side.valueOf(it) },
                ability = Ability.valueOf(prefs.getString("rec_ability", Ability.LITTLE.name) ?: Ability.LITTLE.name),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun updateRecovery(p: RecoveryProfile?) {
        recovery = p
        prefs.edit()
            .putString("rec_condition", p?.condition?.name)
            .putString("rec_side", p?.side?.name)
            .putString("rec_ability", p?.ability?.name)
            .apply()
    }

    // ───────────── My Plan ─────────────
    private fun loadPlan() {
        val raw = prefs.getString("plan", null)
        val ids = if (raw == null) ExerciseLibrary.starterPlan else raw.split(",").filter { it.isNotBlank() }
        plan.clear()
        plan.addAll(ids.filter { ExerciseLibrary.byId(it) != null })
        if (raw == null) savePlan()
    }

    private fun savePlan() = prefs.edit().putString("plan", plan.joinToString(",")).apply()

    fun inPlan(id: String) = id in plan

    fun togglePlan(id: String) {
        if (id in plan) plan.remove(id) else plan.add(id)
        savePlan()
    }

    fun setPlan(ids: List<String>) {
        plan.clear()
        plan.addAll(ids.distinct().filter { ExerciseLibrary.byId(it) != null })
        savePlan()
    }

    fun movePlan(id: String, up: Boolean) {
        val i = plan.indexOf(id)
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j < 0 || j >= plan.size) return
        plan[i] = plan[j]
        plan[j] = id
        savePlan()
    }

    // ───────────── Custom targets (e.g. set by your physio) ─────────────
    private fun loadOverrides() {
        val raw = prefs.getString("overrides", null) ?: return
        try {
            val o = JSONObject(raw)
            o.keys().forEach { k ->
                val t = o.getJSONObject(k)
                overrides[k] = Target(t.getInt("r"), t.getInt("s"), t.getInt("h"))
            }
        } catch (_: Exception) {
        }
    }

    private fun saveOverrides() {
        val o = JSONObject()
        overrides.forEach { (k, t) -> o.put(k, JSONObject().put("r", t.reps).put("s", t.sets).put("h", t.holdSec)) }
        prefs.edit().putString("overrides", o.toString()).apply()
    }

    fun targetFor(ex: Exercise): Target = overrides[ex.id] ?: ex.defaultTarget

    fun isCustomTarget(ex: Exercise) = overrides.containsKey(ex.id)

    fun setTarget(ex: Exercise, t: Target) {
        if (t == ex.defaultTarget) overrides.remove(ex.id) else overrides[ex.id] = t
        saveOverrides()
    }

    // ───────────── History ─────────────
    private fun loadHistory() {
        if (!historyFile.exists()) return
        try {
            val arr = JSONArray(historyFile.readText())
            val list = ArrayList<SessionRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list += SessionRecord(
                    id = o.getLong("id"),
                    exerciseId = o.getString("ex"),
                    time = o.getLong("t"),
                    reps = o.optInt("reps"),
                    sets = o.optInt("sets"),
                    holdSec = o.optInt("hold"),
                    durationSec = o.optInt("dur"),
                    camera = o.optBoolean("cam"),
                    completed = o.optBoolean("done", true),
                    pain = o.optInt("pain", -1),
                )
            }
            history.clear()
            history.addAll(list.sortedByDescending { it.time })
        } catch (_: Exception) {
        }
    }

    private fun saveHistory() {
        val snapshot = history.toList()
        io.execute {
            val arr = JSONArray()
            snapshot.forEach { r ->
                arr.put(
                    JSONObject()
                        .put("id", r.id).put("ex", r.exerciseId).put("t", r.time)
                        .put("reps", r.reps).put("sets", r.sets).put("hold", r.holdSec)
                        .put("dur", r.durationSec).put("cam", r.camera)
                        .put("done", r.completed).put("pain", r.pain)
                )
            }
            val tmp = File(historyFile.parentFile, "history.tmp")
            tmp.writeText(arr.toString())
            if (!tmp.renameTo(historyFile)) {
                historyFile.writeText(arr.toString())
                tmp.delete()
            }
        }
    }

    fun addSession(r: SessionRecord): Long {
        history.add(0, r)
        saveHistory()
        return r.id
    }

    fun updatePain(id: Long, pain: Int) {
        val i = history.indexOfFirst { it.id == id }
        if (i >= 0) {
            history[i] = history[i].copy(pain = pain)
            saveHistory()
        }
    }

    fun deleteSession(id: Long) {
        history.removeAll { it.id == id }
        saveHistory()
    }

    fun clearHistory() {
        history.clear()
        saveHistory()
    }

    // ───────────── Stats ─────────────
    fun todaySessions(): List<SessionRecord> {
        val today = LocalDate.now()
        return history.filter { dayOf(it.time) == today }
    }

    fun streak(): Int {
        val days = history.map { dayOf(it.time) }.toHashSet()
        var d = LocalDate.now()
        if (d !in days) d = d.minusDays(1)
        var n = 0
        while (d in days) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    /** Session counts for the last 7 days, oldest first. */
    fun last7Days(): List<Pair<LocalDate, Int>> {
        val today = LocalDate.now()
        val counts = history.groupingBy { dayOf(it.time) }.eachCount()
        return (6 downTo 0).map { back -> today.minusDays(back.toLong()).let { it to (counts[it] ?: 0) } }
    }

    fun totalReps(): Int = history.sumOf { it.reps }

    fun buildReport(): String {
        val fmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm")
        val sb = StringBuilder()
        val who = settings.name.ifBlank { "Patient" }
        sb.append("PhysioCare exercise report – $who\n")
        sb.append("Current streak: ${streak()} day(s). Sessions recorded: ${history.size}. Total reps: ${totalReps()}.\n\n")
        history.take(60).forEach { r ->
            val ex = ExerciseLibrary.byId(r.exerciseId)
            val time = Instant.ofEpochMilli(r.time).atZone(ZoneId.systemDefault()).format(fmt)
            sb.append("• $time – ${ex?.name ?: r.exerciseId}: ${r.reps} reps")
            if (r.holdSec > 0) sb.append(" (${r.holdSec}s holds)")
            sb.append(", ${(r.durationSec + 59) / 60} min")
            if (!r.completed) sb.append(", stopped early")
            if (r.pain >= 0) sb.append(", pain ${r.pain}/10")
            sb.append("\n")
        }
        return sb.toString()
    }
}
