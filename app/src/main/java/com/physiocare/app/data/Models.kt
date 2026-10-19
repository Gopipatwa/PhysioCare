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
    RECOVERY("Stroke & Paralysis Recovery", "🧠", "Helped and small-movement exercises"),
    FACE("Face (Bell's palsy)", "🙂", "Gentle face exercises in front of a mirror"),
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

enum class Posture { STAND, SIT, LIE, FACE }

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

/** Face expression for Bell's palsy exercises. Each value 0 (relaxed) .. 1 (full). */
data class FacePose(
    val brow: Float = 0f,
    val eyeClose: Float = 0f,
    val smile: Float = 0f,
    val pucker: Float = 0f,
    val puff: Float = 0f,
    val nose: Float = 0f,
    val mouthOpen: Float = 0f,
) {
    fun lerp(o: FacePose, t: Float): FacePose {
        fun m(a: Float, b: Float) = a + (b - a) * t
        return FacePose(
            m(brow, o.brow), m(eyeClose, o.eyeClose), m(smile, o.smile), m(pucker, o.pucker),
            m(puff, o.puff), m(nose, o.nose), m(mouthOpen, o.mouthOpen),
        )
    }
}

/** Two key poses the guide animation moves between. front = viewed from the front. */
data class Guide(
    val posture: Posture,
    val front: Boolean,
    val a: FigurePose,
    val b: FigurePose = a,
    val faceA: FacePose? = null,
    val faceB: FacePose? = null,
)

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
    /** True when a carer moves the body part; the steps are written for the helper. */
    val helper: Boolean = false,
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

// ───────────── Paralysis & stroke recovery ─────────────

enum class Condition(
    val title: String,
    val emoji: String,
    val blurb: String,
    /** Used in the question "How much can you move …?" */
    val bodyPart: String,
    /** Whether one side is affected (then we ask which side). */
    val oneSided: Boolean,
) {
    STROKE("Stroke", "🧠", "One side of the body is weak or numb – arm, leg or face", "your weaker arm and leg", true),
    SCI_LEGS("Spinal cord injury – legs", "🦽", "Legs are paralysed but arms work (paraplegia)", "your legs", false),
    SCI_ALL("Spinal cord injury – arms and legs", "🧑‍🦽", "Arms, hands and legs are affected (tetraplegia)", "your arms and hands", false),
    BELLS("Bell's palsy", "🙂", "One side of the face droops or won't move", "the weak side of your face", true),
    NERVE("Guillain-Barré or nerve weakness", "⚡", "Arms and legs weakened by a nerve illness, slowly recovering", "your arms and legs", false),
}

enum class Ability(val title: String, val blurb: String) {
    NONE("Not yet – someone helps me", "No movement yet, or only a flicker. A carer or family member moves it for me."),
    LITTLE("A little", "Small or slow movements, or only with help from my stronger hand."),
    WEAK("Yes, but it's weak", "I can do the movement myself, but it tires quickly or isn't steady."),
}

enum class Side(val label: String) { LEFT("Left"), RIGHT("Right") }

data class RecoveryProfile(val condition: Condition, val side: Side?, val ability: Ability)
