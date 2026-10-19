package com.physiocare.app.data

/** Body areas shown on the Exercises screen. */
enum class Category(val title: String, val emoji: String, val blurb: String) {
    NECK("Neck", "🙆", "Stiff neck, posture, headaches"),
    SHOULDERS("Shoulders", "💪", "Frozen shoulder, rotator cuff, reaching"),
    ARMS("Arms, Wrists & Hands", "✋", "Grip, arthritis, elbow and wrist"),
    BACK("Back & Spine", "🧍", "Low back pain, stiffness, mobility"),
    CORE("Core & Posture", "🎯", "Stability, sitting tall, trunk strength"),
    HIPS("Hips", "🦴", "Hip arthritis, hip replacement, strength"),
    KNEES("Knees", "🦵", "Knee pain, arthritis, knee replacement"),
    FEET("Ankles & Feet", "🦶", "Swelling, sprains, circulation"),
    BALANCE("Balance & Daily Strength", "⚖️", "Fall prevention, walking, standing up"),
    BREATHING("Breathing & Relaxation", "🌬️", "Lungs, calm, recovery"),
}

enum class Level(val label: String) { EASY("Easy"), MEDIUM("Medium"), HARD("Harder") }

/** How a session is counted. CAMERA falls back to tap counting when the person prefers. */
enum class Mode { CAMERA, MANUAL, HOLD }

/** AVG: both sides move together (squat). EACH: either side can complete a rep (one arm at a time). */
enum class SideMode { AVG, EACH }

/** ML Kit landmark ids for the left and right side of each joint. */
enum class Joint(val left: Int, val right: Int, val label: String) {
    EAR(7, 8, "head"),
    SHOULDER(11, 12, "shoulders"),
    ELBOW(13, 14, "elbows"),
    WRIST(15, 16, "wrists"),
    HIP(23, 24, "hips"),
    KNEE(25, 26, "knees"),
    ANKLE(27, 28, "ankles"),
    HEEL(29, 30, "heels"),
    FOOT(31, 32, "feet"),
}

/**
 * Camera counting rule: the angle at joint [b] (between a-b-c) must travel from
 * [start] to [target] and back to [start] to count one repetition.
 */
data class AngleRule(
    val a: Joint,
    val b: Joint,
    val c: Joint,
    val start: Float,
    val target: Float,
    val side: SideMode = SideMode.EACH,
    val aOpposite: Boolean = false,
    val hint: String,
)

enum class Posture { STAND, SIT, LIE }

/**
 * Joint angles (degrees) for the animated guide figure.
 * Limb angle 0 = hanging along the body; positive = moving forward / outward.
 * trunk: 0 upright, +90 face-down, -90 lying on the back.
 */
data class FigurePose(
    val trunk: Float = 0f,
    val head: Float = 0f,
    val shrug: Float = 0f,
    val lArm: Float = 0f,
    val lElbow: Float = 0f,
    val rArm: Float = 0f,
    val rElbow: Float = 0f,
    val lHip: Float = 0f,
    val lKnee: Float = 0f,
    val lAnkle: Float = 0f,
    val rHip: Float = 0f,
    val rKnee: Float = 0f,
    val rAnkle: Float = 0f,
) {
    fun lerp(o: FigurePose, t: Float): FigurePose {
        fun m(a: Float, b: Float) = a + (b - a) * t
        return FigurePose(
            m(trunk, o.trunk), m(head, o.head), m(shrug, o.shrug),
            m(lArm, o.lArm), m(lElbow, o.lElbow), m(rArm, o.rArm), m(rElbow, o.rElbow),
            m(lHip, o.lHip), m(lKnee, o.lKnee), m(lAnkle, o.lAnkle),
            m(rHip, o.rHip), m(rKnee, o.rKnee), m(rAnkle, o.rAnkle),
        )
    }
}

/** Two key poses the guide animation moves between. front = viewed from the front. */
data class Guide(val posture: Posture, val front: Boolean, val a: FigurePose, val b: FigurePose = a)

data class Target(val reps: Int, val sets: Int, val holdSec: Int)

data class Exercise(
    val id: String,
    val name: String,
    val category: Category,
    val level: Level,
    val goodFor: String,
    val steps: List<String>,
    val tips: List<String>,
    val mode: Mode,
    val reps: Int,
    val sets: Int,
    val holdSec: Int,
    val restSec: Int,
    val perSide: Boolean,
    val rule: AngleRule?,
    val guide: Guide,
    /** Seconds per breath for breathing exercises (0 = not a breathing exercise). */
    val breathCycle: Int,
) {
    val defaultTarget: Target get() = Target(reps, sets, holdSec)
    val cameraCapable: Boolean get() = rule != null
}

fun Exercise.summary(t: Target): String {
    val side = if (perSide) " each side" else ""
    val sets = if (t.sets == 1) "1 set" else "${t.sets} sets"
    return if (mode == Mode.HOLD) "Hold ${t.holdSec}s × ${t.reps}, $sets$side"
    else "${t.reps} reps × $sets$side"
}
