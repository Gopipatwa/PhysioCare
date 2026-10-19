package com.physiocare.app.data

import com.physiocare.app.data.Category.ARMS
import com.physiocare.app.data.Category.BACK
import com.physiocare.app.data.Category.BALANCE
import com.physiocare.app.data.Category.BREATHING
import com.physiocare.app.data.Category.CORE
import com.physiocare.app.data.Category.FEET
import com.physiocare.app.data.Category.HIPS
import com.physiocare.app.data.Category.KNEES
import com.physiocare.app.data.Category.NECK
import com.physiocare.app.data.Category.SHOULDERS
import com.physiocare.app.data.Joint.ANKLE
import com.physiocare.app.data.Joint.ELBOW
import com.physiocare.app.data.Joint.FOOT
import com.physiocare.app.data.Joint.HIP
import com.physiocare.app.data.Joint.KNEE
import com.physiocare.app.data.Joint.SHOULDER
import com.physiocare.app.data.Joint.WRIST

// ---- Key poses reused by the guide animations ----
private val STANDING = FigurePose()
private val F_STANDING = FigurePose(lArm = 8f, rArm = 8f, lHip = 5f, rHip = 5f)
private val SITTING = FigurePose(lHip = 90f, rHip = 90f, lKnee = 90f, rKnee = 90f)
private val F_SITTING = FigurePose(lArm = 8f, rArm = 8f, lHip = 8f, rHip = 8f)
private val SUPINE = FigurePose(trunk = -90f)
private val HOOK = FigurePose(trunk = -90f, lHip = 45f, rHip = 45f, lKnee = 100f, rKnee = 100f)
private val PRONE = FigurePose(trunk = 90f)
private val QUAD = FigurePose(
    trunk = 90f, lArm = 90f, rArm = 90f, lHip = 90f, rHip = 90f, lKnee = 90f, rKnee = 90f
)
private val SIT_LEG_OUT = SITTING.copy(rKnee = 0f)
private val HANDS_FORWARD = SITTING.copy(rArm = 10f, rElbow = 80f)
private val SIDE_LYING = FigurePose(trunk = -90f)

private fun side(p: Posture, a: FigurePose, b: FigurePose = a) = Guide(p, false, a, b)
private fun front(p: Posture, a: FigurePose, b: FigurePose = a) = Guide(p, true, a, b)

private fun ex(
    id: String,
    name: String,
    cat: Category,
    level: Level,
    goodFor: String,
    steps: List<String>,
    tips: List<String> = emptyList(),
    mode: Mode = Mode.MANUAL,
    reps: Int = 10,
    sets: Int = 2,
    hold: Int = 0,
    rest: Int = 30,
    perSide: Boolean = false,
    rule: AngleRule? = null,
    guide: Guide,
    breath: Int = 0,
) = Exercise(
    id = id, name = name, category = cat, level = level, goodFor = goodFor,
    steps = steps, tips = tips,
    mode = if (rule != null && mode == Mode.MANUAL) Mode.CAMERA else mode,
    reps = reps, sets = sets, holdSec = hold, restSec = rest,
    perSide = perSide, rule = rule, guide = guide, breathCycle = breath,
)

private const val SIDE_ON = "Stand side-on to the phone, about 2–3 metres away, so your whole body is visible."
private const val FACE_ON = "Face the phone, about 2–3 metres away, so your whole body is visible."

object ExerciseLibrary {

    val all: List<Exercise> = listOf(
        // ───────────────────────── NECK ─────────────────────────
        ex(
            "neck_chin_tuck", "Chin Tucks", NECK, Level.EASY,
            "Neck stiffness, forward-head posture, headaches",
            listOf(
                "Sit tall in a firm chair with your shoulders relaxed.",
                "Look straight ahead and gently slide your chin straight back, like making a double chin.",
                "Hold, feeling a light stretch at the back of your neck.",
                "Relax and let your head return to the start.",
            ),
            listOf("Keep your eyes level – don't nod your head down."),
            mode = Mode.HOLD, reps = 8, sets = 1, hold = 5,
            guide = side(Posture.SIT, SITTING, SITTING.copy(head = -14f)),
        ),
        ex(
            "neck_side_tilt", "Neck Side Stretch", NECK, Level.EASY,
            "Tight neck and upper shoulders",
            listOf(
                "Sit tall and let your shoulders drop.",
                "Slowly tilt your ear towards your shoulder until you feel a gentle stretch.",
                "Hold, breathing slowly.",
                "Return to the middle, then repeat on the other side.",
            ),
            listOf("Keep your shoulders down – bring your ear to your shoulder, not the shoulder to the ear."),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 15, perSide = true,
            guide = front(Posture.SIT, F_SITTING, F_SITTING.copy(head = 30f)),
        ),
        ex(
            "neck_rotation", "Neck Turns", NECK, Level.EASY,
            "Turning the head when driving or crossing roads",
            listOf(
                "Sit tall, looking straight ahead.",
                "Slowly turn your head to look over one shoulder, as far as is comfortable.",
                "Come back to the middle, then turn to the other side.",
                "One turn to each side counts as one rep.",
            ),
            reps = 10, sets = 1,
            guide = front(Posture.SIT, F_SITTING, F_SITTING.copy(head = 12f)),
        ),
        ex(
            "neck_flex_ext", "Look Down and Up", NECK, Level.EASY,
            "Neck movement and stiffness",
            listOf(
                "Sit tall with your shoulders relaxed.",
                "Slowly lower your chin towards your chest.",
                "Lift your head back to the middle and look slightly up at the ceiling.",
                "Return to looking straight ahead.",
            ),
            listOf("Move gently. Skip looking up if it makes you dizzy."),
            reps = 10, sets = 1,
            guide = side(Posture.SIT, SITTING.copy(head = -15f), SITTING.copy(head = 35f)),
        ),

        // ───────────────────────── SHOULDERS ─────────────────────────
        ex(
            "sh_shrugs", "Shoulder Shrugs", SHOULDERS, Level.EASY,
            "Neck and shoulder tension",
            listOf(
                "Sit or stand with your arms resting by your sides.",
                "Lift both shoulders up towards your ears.",
                "Hold for a moment, then let them drop slowly.",
            ),
            reps = 10, sets = 2,
            guide = front(Posture.STAND, F_STANDING, F_STANDING.copy(shrug = 7f)),
        ),
        ex(
            "sh_rolls", "Shoulder Rolls", SHOULDERS, Level.EASY,
            "Stiff shoulders, rounded posture",
            listOf(
                "Sit or stand tall with arms relaxed.",
                "Lift your shoulders up, squeeze them back, then let them drop down in a circle.",
                "Do all your circles backwards, then repeat forwards.",
            ),
            reps = 10, sets = 2,
            guide = side(Posture.STAND, STANDING, STANDING.copy(shrug = 6f, rArm = -12f, lArm = -12f)),
        ),
        ex(
            "sh_pendulum", "Pendulum Swings", SHOULDERS, Level.EASY,
            "Frozen shoulder, early recovery after shoulder injury or surgery",
            listOf(
                "Stand beside a table and rest your good hand on it.",
                "Lean forward and let your sore arm hang down loosely.",
                "Gently sway your body so the arm swings in small circles.",
                "Keep going for the full time, then change direction.",
            ),
            listOf("Let your body move the arm – keep the arm muscles relaxed."),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 30,
            guide = side(
                Posture.STAND,
                FigurePose(trunk = 45f, lHip = 45f, rHip = 45f, lArm = 90f, rArm = 32f),
                FigurePose(trunk = 45f, lHip = 45f, rHip = 45f, lArm = 90f, rArm = 60f),
            ),
        ),
        ex(
            "sh_flexion", "Forward Arm Raise", SHOULDERS, Level.EASY,
            "Reaching up to shelves, shoulder stiffness",
            listOf(
                "Stand tall with your arm by your side, thumb pointing forward.",
                "Slowly raise your straight arm forward and up as high as is comfortable.",
                "Lower it slowly back down, then raise the other arm.",
                "Each raise counts as one.",
            ),
            listOf("Don't arch your back to get higher."),
            reps = 10, sets = 2,
            rule = AngleRule(HIP, SHOULDER, ELBOW, start = 35f, target = 135f, hint = SIDE_ON),
            guide = side(Posture.STAND, STANDING, STANDING.copy(rArm = 160f)),
        ),
        ex(
            "sh_abduction", "Side Arm Raise", SHOULDERS, Level.EASY,
            "Shoulder strength, dressing and washing hair",
            listOf(
                "Stand tall facing forward, arms by your sides.",
                "Raise both arms out to the sides up to shoulder height, palms down.",
                "Pause, then lower slowly.",
            ),
            listOf("Hold light water bottles to make it harder."),
            reps = 10, sets = 2,
            rule = AngleRule(HIP, SHOULDER, ELBOW, start = 30f, target = 80f, side = SideMode.AVG, hint = FACE_ON),
            guide = front(Posture.STAND, F_STANDING, F_STANDING.copy(lArm = 90f, rArm = 90f)),
        ),
        ex(
            "sh_overhead_press", "Overhead Press", SHOULDERS, Level.MEDIUM,
            "Shoulder and upper-arm strength",
            listOf(
                "Stand or sit tall. Raise your elbows to shoulder height with hands pointing up.",
                "Push both hands straight up above your head.",
                "Slowly lower back to the start position.",
            ),
            listOf("Use light weights only after it feels easy without them."),
            reps = 10, sets = 2,
            rule = AngleRule(HIP, SHOULDER, ELBOW, start = 110f, target = 150f, side = SideMode.AVG, hint = FACE_ON),
            guide = front(
                Posture.STAND,
                F_STANDING.copy(lArm = 90f, rArm = 90f, lElbow = 90f, rElbow = 90f),
                F_STANDING.copy(lArm = 168f, rArm = 168f),
            ),
        ),
        ex(
            "sh_scap_squeeze", "Shoulder Blade Squeeze", SHOULDERS, Level.EASY,
            "Rounded shoulders, upper back posture",
            listOf(
                "Sit tall with your arms relaxed and elbows bent.",
                "Squeeze your shoulder blades together and down, as if holding a pencil between them.",
                "Hold, then relax.",
            ),
            mode = Mode.HOLD, reps = 10, sets = 1, hold = 5,
            guide = side(
                Posture.SIT,
                SITTING.copy(lElbow = 90f, rElbow = 90f),
                SITTING.copy(lArm = -25f, rArm = -25f, lElbow = 90f, rElbow = 90f),
            ),
        ),
        ex(
            "sh_crossbody", "Cross-Body Stretch", SHOULDERS, Level.EASY,
            "Tight back of the shoulder",
            listOf(
                "Bring one arm across the front of your chest.",
                "With the other hand, gently press the arm closer to your chest.",
                "Hold the stretch, then change arms.",
            ),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 20, perSide = true,
            guide = front(
                Posture.STAND, F_STANDING,
                F_STANDING.copy(rArm = -80f, lArm = 30f, lElbow = -120f),
            ),
        ),
        ex(
            "sh_wall_walk", "Finger Wall Walk", SHOULDERS, Level.EASY,
            "Frozen shoulder, regaining reach after injury",
            listOf(
                "Stand facing a wall, about an arm's length away.",
                "Place your fingertips on the wall at waist height.",
                "Walk your fingers up the wall as high as is comfortable.",
                "Hold for a moment, then walk them back down.",
            ),
            reps = 8, sets = 2, perSide = true,
            guide = side(Posture.STAND, STANDING.copy(rArm = 60f), STANDING.copy(rArm = 150f)),
        ),
        ex(
            "sh_ext_rotation", "Elbows-In Outward Turn", SHOULDERS, Level.MEDIUM,
            "Rotator cuff strength",
            listOf(
                "Sit tall with elbows bent at a right angle and tucked into your sides.",
                "Keeping elbows in, turn your forearms outwards like opening a gate.",
                "Return slowly. Hold a resistance band between your hands to make it harder.",
            ),
            reps = 12, sets = 2,
            guide = front(
                Posture.SIT,
                F_SITTING.copy(lArm = 4f, rArm = 4f, lElbow = 25f, rElbow = 25f),
                F_SITTING.copy(lArm = 4f, rArm = 4f, lElbow = 95f, rElbow = 95f),
            ),
        ),

        // ───────────────────────── ARMS, WRISTS & HANDS ─────────────────────────
        ex(
            "arm_bicep_curl", "Arm Curls", ARMS, Level.EASY,
            "Lifting shopping, carrying, arm strength",
            listOf(
                "Sit or stand with arms by your sides, palms facing forward.",
                "Bend your elbow to bring your hand up towards your shoulder.",
                "Lower slowly until the arm is straight.",
                "Swap arms each time – every curl counts as one.",
            ),
            listOf("Hold a water bottle or tin for extra strength work."),
            reps = 12, sets = 2,
            rule = AngleRule(SHOULDER, ELBOW, WRIST, start = 140f, target = 65f, hint = SIDE_ON),
            guide = side(Posture.STAND, STANDING, STANDING.copy(rElbow = 140f)),
        ),
        ex(
            "arm_triceps", "Overhead Arm Straighten", ARMS, Level.MEDIUM,
            "Pushing up from a chair, back of the arm strength",
            listOf(
                "Sit or stand tall. Raise one arm up with the elbow bent, hand behind your head.",
                "Support the elbow with your other hand.",
                "Straighten the arm up towards the ceiling, then lower slowly.",
            ),
            reps = 10, sets = 2, perSide = true,
            rule = AngleRule(SHOULDER, ELBOW, WRIST, start = 95f, target = 150f, hint = SIDE_ON),
            guide = side(
                Posture.STAND,
                STANDING.copy(rArm = 170f, rElbow = 120f),
                STANDING.copy(rArm = 170f, rElbow = 5f),
            ),
        ),
        ex(
            "arm_wall_pushup", "Wall Push-Ups", ARMS, Level.MEDIUM,
            "Upper body strength, getting up from the floor",
            listOf(
                "Stand an arm's length from a wall and place your palms on it at shoulder height.",
                "Keeping your body straight, bend your elbows and lean towards the wall.",
                "Push back to the start.",
            ),
            listOf("Step further from the wall to make it harder."),
            reps = 10, sets = 2,
            rule = AngleRule(SHOULDER, ELBOW, WRIST, start = 145f, target = 105f, side = SideMode.AVG, hint = SIDE_ON),
            guide = side(
                Posture.STAND,
                FigurePose(trunk = 20f, lArm = 110f, rArm = 110f),
                FigurePose(trunk = 34f, lArm = 70f, rArm = 70f, lElbow = 85f, rElbow = 85f),
            ),
        ),
        ex(
            "arm_chair_pushup", "Chair Push-Ups", ARMS, Level.MEDIUM,
            "Strength to stand up from a chair",
            listOf(
                "Sit in a sturdy chair with armrests, feet flat on the floor.",
                "Place your hands on the armrests.",
                "Push down to lift your bottom slightly off the seat.",
                "Lower yourself slowly back down.",
            ),
            reps = 8, sets = 2,
            guide = side(Posture.SIT, SITTING.copy(rArm = -10f, rElbow = 40f), SITTING.copy(rArm = -10f, rElbow = 5f)),
        ),
        ex(
            "wrist_flex_ext", "Wrist Bends", ARMS, Level.EASY,
            "Wrist stiffness, arthritis, after a wrist fracture",
            listOf(
                "Rest your forearm on a table with your hand over the edge.",
                "Slowly bend your wrist up, then down, as far as comfortable.",
                "Keep the forearm still on the table.",
            ),
            reps = 15, sets = 2, perSide = true,
            guide = side(Posture.SIT, SITTING.copy(rArm = 10f, rElbow = 70f), SITTING.copy(rArm = 10f, rElbow = 100f)),
        ),
        ex(
            "wrist_turns", "Palm Up, Palm Down", ARMS, Level.EASY,
            "Turning door handles and keys",
            listOf(
                "Sit with your elbow bent at your side, forearm resting forward.",
                "Turn your palm up to face the ceiling.",
                "Turn it down to face the floor. That is one rep.",
            ),
            reps = 15, sets = 2, perSide = true,
            guide = side(Posture.SIT, HANDS_FORWARD, HANDS_FORWARD.copy(rElbow = 90f)),
        ),
        ex(
            "hand_fist", "Make a Fist", ARMS, Level.EASY,
            "Hand arthritis, grip, swelling in the fingers",
            listOf(
                "Hold your hand up with fingers straight.",
                "Slowly curl your fingers into a gentle fist, thumb on the outside.",
                "Open your hand wide and spread your fingers.",
            ),
            reps = 10, sets = 2, perSide = true,
            guide = side(Posture.SIT, HANDS_FORWARD, HANDS_FORWARD.copy(rElbow = 95f)),
        ),
        ex(
            "hand_thumb_touch", "Thumb to Fingertips", ARMS, Level.EASY,
            "Finger dexterity: buttons, writing, picking up coins",
            listOf(
                "Hold your hand up in front of you.",
                "Touch your thumb to each fingertip in turn, making an 'O' shape.",
                "Going from first finger to little finger and back counts as one rep.",
            ),
            reps = 5, sets = 2, perSide = true,
            guide = side(Posture.SIT, HANDS_FORWARD, HANDS_FORWARD.copy(rElbow = 90f)),
        ),
        ex(
            "hand_squeeze", "Soft Ball Squeeze", ARMS, Level.EASY,
            "Grip strength for jars, bags and handrails",
            listOf(
                "Hold a soft ball, rolled sock or sponge in your hand.",
                "Squeeze it as firmly as is comfortable.",
                "Hold, then relax your hand slowly.",
            ),
            mode = Mode.HOLD, reps = 10, sets = 1, hold = 5, perSide = true,
            guide = side(Posture.SIT, HANDS_FORWARD),
        ),

        // ───────────────────────── BACK & SPINE ─────────────────────────
        ex(
            "back_cat_cow", "Cat and Cow", BACK, Level.MEDIUM,
            "Back stiffness, spine mobility",
            listOf(
                "Get onto your hands and knees on a mat, hands under shoulders.",
                "Round your back up towards the ceiling, tucking your chin in.",
                "Then let your tummy sink gently and lift your head.",
                "Move slowly between the two positions.",
            ),
            listOf("Only do this if you can get down to and up from the floor safely."),
            reps = 10, sets = 1,
            guide = side(Posture.LIE, QUAD.copy(trunk = 96f, head = 30f), QUAD.copy(trunk = 84f, head = -25f)),
        ),
        ex(
            "back_knee_chest", "Knee to Chest Stretch", BACK, Level.EASY,
            "Low back pain and stiffness",
            listOf(
                "Lie on your back on a bed or mat with both knees bent.",
                "Hold behind one knee and gently pull it towards your chest.",
                "Hold the stretch, then lower the leg slowly.",
            ),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 20, perSide = true,
            guide = side(Posture.LIE, HOOK, HOOK.copy(rHip = 120f, rKnee = 120f, rArm = 60f)),
        ),
        ex(
            "back_pelvic_tilt", "Pelvic Tilt", BACK, Level.EASY,
            "Low back pain, core control",
            listOf(
                "Lie on your back with knees bent and feet flat.",
                "Tighten your tummy and gently flatten your lower back into the bed.",
                "Hold, breathing normally, then relax.",
            ),
            mode = Mode.HOLD, reps = 10, sets = 1, hold = 5,
            guide = side(Posture.LIE, HOOK, HOOK.copy(trunk = -95f, lHip = 50f, rHip = 50f)),
        ),
        ex(
            "back_bridge", "Bridge", BACK, Level.MEDIUM,
            "Back, hip and buttock strength",
            listOf(
                "Lie on your back with knees bent, feet flat and arms by your sides.",
                "Squeeze your buttocks and lift your hips off the bed.",
                "Hold for a moment, then lower slowly.",
            ),
            listOf("Lift only as high as is comfortable – don't arch your back."),
            reps = 10, sets = 2,
            rule = AngleRule(SHOULDER, HIP, KNEE, start = 145f, target = 163f, side = SideMode.AVG,
                hint = "Lie side-on to the phone, with the phone on the floor 2 metres away."),
            guide = side(
                Posture.LIE, HOOK,
                FigurePose(trunk = -110f, lHip = 0f, rHip = 0f, lKnee = 95f, rKnee = 95f, lArm = -25f, rArm = -25f),
            ),
        ),
        ex(
            "back_press_up", "Prone Press-Up", BACK, Level.MEDIUM,
            "Low back pain that eases when standing or walking",
            listOf(
                "Lie on your front with hands under your shoulders.",
                "Keeping your hips on the bed, gently push your upper body up.",
                "Go only as far as is comfortable, then lower slowly.",
            ),
            listOf("Stop if pain spreads down into your leg."),
            reps = 10, sets = 1,
            guide = side(
                Posture.LIE,
                PRONE.copy(lArm = 10f, rArm = 10f, lElbow = 170f, rElbow = 170f),
                FigurePose(trunk = 72f, lArm = 77f, rArm = 77f, lElbow = 85f, rElbow = 85f, lHip = -11f, rHip = -11f),
            ),
        ),
        ex(
            "back_rotation", "Knee Rolls", BACK, Level.EASY,
            "Gentle low back mobility",
            listOf(
                "Lie on your back with knees bent and together.",
                "Slowly let both knees roll to one side, keeping shoulders flat.",
                "Bring them back to the middle and roll to the other side.",
            ),
            reps = 10, sets = 1,
            guide = side(Posture.LIE, HOOK, HOOK.copy(lHip = 30f, rHip = 30f)),
        ),
        ex(
            "back_superman", "Back Extension (Lying)", BACK, Level.HARD,
            "Strength for the muscles along the spine",
            listOf(
                "Lie on your front with arms stretched forward.",
                "Gently lift your arms, chest and legs a little off the floor.",
                "Hold, then lower slowly.",
            ),
            mode = Mode.HOLD, reps = 6, sets = 1, hold = 5,
            guide = side(
                Posture.LIE,
                PRONE.copy(lArm = 180f, rArm = 180f),
                PRONE.copy(trunk = 82f, lArm = 180f, rArm = 180f, lHip = -14f, rHip = -14f),
            ),
        ),
        ex(
            "back_seated_twist", "Seated Twist", BACK, Level.EASY,
            "Upper and middle back stiffness",
            listOf(
                "Sit tall, feet flat, arms crossed over your chest.",
                "Slowly turn your upper body to one side.",
                "Hold the gentle stretch, then return and turn the other way.",
            ),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 15, perSide = true,
            guide = front(
                Posture.SIT,
                F_SITTING.copy(lArm = 30f, rArm = 30f, lElbow = -140f, rElbow = -140f),
                F_SITTING.copy(head = 10f, lArm = 40f, rArm = 20f, lElbow = -140f, rElbow = -140f),
            ),
        ),
        ex(
            "back_side_bend", "Standing Side Bend", BACK, Level.EASY,
            "Side stiffness, reaching sideways",
            listOf(
                "Stand tall, feet hip-width apart, holding onto a chair if needed.",
                "Slide one hand down the side of your leg as you lean sideways.",
                "Return to upright and repeat on the other side.",
            ),
            reps = 10, sets = 1,
            guide = front(
                Posture.STAND, F_STANDING,
                F_STANDING.copy(trunk = -16f, lHip = 21f, rHip = -11f, rArm = 20f, lArm = 0f),
            ),
        ),
        ex(
            "back_bird_dog", "Bird Dog", BACK, Level.HARD,
            "Back and core control, balance",
            listOf(
                "Start on your hands and knees on a mat.",
                "Slowly stretch one arm forward and the opposite leg back.",
                "Hold steady, then return. Repeat with the other arm and leg.",
            ),
            mode = Mode.HOLD, reps = 6, sets = 1, hold = 5, perSide = true,
            guide = side(Posture.LIE, QUAD, QUAD.copy(rArm = 180f, lHip = 0f, lKnee = 0f)),
        ),

        // ───────────────────────── CORE & POSTURE ─────────────────────────
        ex(
            "core_brace", "Tummy Tightening", CORE, Level.EASY,
            "Core control, back support, bladder control",
            listOf(
                "Lie on your back with knees bent, or sit tall.",
                "Gently draw your belly button in towards your spine.",
                "Hold while breathing normally, then relax.",
            ),
            mode = Mode.HOLD, reps = 10, sets = 1, hold = 10,
            guide = side(Posture.LIE, HOOK),
        ),
        ex(
            "core_dead_bug", "Dead Bug", CORE, Level.MEDIUM,
            "Core strength and coordination",
            listOf(
                "Lie on your back, knees bent up over your hips, arms reaching to the ceiling.",
                "Slowly lower one arm overhead while straightening the opposite leg.",
                "Return and swap sides. Keep your lower back flat.",
            ),
            reps = 10, sets = 2,
            guide = side(
                Posture.LIE,
                FigurePose(trunk = -90f, lHip = 90f, rHip = 90f, lKnee = 90f, rKnee = 90f, lArm = 90f, rArm = 90f),
                FigurePose(trunk = -90f, lHip = 15f, rHip = 90f, lKnee = 0f, rKnee = 90f, lArm = 90f, rArm = 180f),
            ),
        ),
        ex(
            "core_crunch", "Partial Sit-Up", CORE, Level.MEDIUM,
            "Tummy muscle strength",
            listOf(
                "Lie on your back with knees bent.",
                "Reach your hands towards your knees, lifting your head and shoulders slightly.",
                "Lower slowly. Keep your neck relaxed.",
            ),
            reps = 10, sets = 2,
            guide = side(
                Posture.LIE, HOOK,
                FigurePose(trunk = -65f, lHip = 70f, rHip = 70f, lKnee = 100f, rKnee = 100f, lArm = 45f, rArm = 45f, head = 10f),
            ),
        ),
        ex(
            "core_plank", "Forearm Plank", CORE, Level.HARD,
            "Whole-body strength and stability",
            listOf(
                "Lie on your front, resting on your forearms.",
                "Lift your body so you are on forearms and toes (or knees, to make it easier).",
                "Keep your body in a straight line and hold.",
            ),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 20,
            guide = side(
                Posture.LIE,
                FigurePose(trunk = 80f, lArm = 80f, rArm = 80f, lElbow = 90f, rElbow = 90f, lHip = 0f, rHip = 0f),
            ),
        ),
        ex(
            "core_sit_tall", "Sit Tall Posture", CORE, Level.EASY,
            "Slouching, back and neck ache from sitting",
            listOf(
                "Sit towards the front of a chair with feet flat.",
                "Grow tall, as if a string is pulling the top of your head up.",
                "Draw your shoulders gently back and hold.",
            ),
            mode = Mode.HOLD, reps = 5, sets = 1, hold = 10,
            guide = side(Posture.SIT, SITTING.copy(trunk = -10f, head = 28f, lHip = 80f, rHip = 80f), SITTING),
        ),
        ex(
            "core_wall_angel", "Wall Angels", CORE, Level.MEDIUM,
            "Rounded upper back, shoulder posture",
            listOf(
                "Stand with your back against a wall, arms in a 'goalpost' shape.",
                "Slowly slide your arms up the wall as high as is comfortable.",
                "Slide them back down to the start.",
            ),
            reps = 10, sets = 2,
            guide = front(
                Posture.STAND,
                F_STANDING.copy(lArm = 90f, rArm = 90f, lElbow = 90f, rElbow = 90f),
                F_STANDING.copy(lArm = 155f, rArm = 155f, lElbow = 25f, rElbow = 25f),
            ),
        ),

        // ───────────────────────── HIPS ─────────────────────────
        ex(
            "hip_abduction", "Side Leg Raise (Standing)", HIPS, Level.EASY,
            "Hip strength, walking steadily, balance",
            listOf(
                "Stand behind a chair holding the back for support.",
                "Keeping your body upright, lift one leg out to the side.",
                "Lower slowly. Finish one side, then the other.",
            ),
            listOf("Keep your toes pointing forward."),
            reps = 10, sets = 2, perSide = true,
            rule = AngleRule(HIP, HIP, KNEE, start = 104f, target = 116f, aOpposite = true, hint = FACE_ON),
            guide = front(Posture.STAND, F_STANDING, F_STANDING.copy(rHip = 32f)),
        ),
        ex(
            "hip_extension", "Leg Back Kick", HIPS, Level.EASY,
            "Walking stride, buttock strength",
            listOf(
                "Stand behind a chair holding on for support.",
                "Keeping your knee straight, move one leg backwards.",
                "Don't lean forward. Return slowly.",
            ),
            reps = 10, sets = 2, perSide = true,
            guide = side(Posture.STAND, STANDING, STANDING.copy(rHip = -28f, trunk = 4f)),
        ),
        ex(
            "hip_seated_march", "Seated Marching", HIPS, Level.EASY,
            "Hip strength when standing is hard",
            listOf(
                "Sit tall in a chair, holding the sides of the seat.",
                "Lift one knee up, then lower it.",
                "Then lift the other knee, like marching.",
            ),
            reps = 20, sets = 2,
            guide = side(Posture.SIT, SITTING, SITTING.copy(rHip = 118f)),
        ),
        ex(
            "hip_clams", "Clamshells", HIPS, Level.MEDIUM,
            "Hip and buttock strength, hip pain",
            listOf(
                "Lie on your side with knees bent and feet together.",
                "Keeping your feet together, lift your top knee.",
                "Lower slowly. Don't let your hips roll back.",
            ),
            reps = 12, sets = 2, perSide = true,
            guide = front(
                Posture.LIE,
                FigurePose(trunk = -90f, lHip = 30f, rHip = -30f, lKnee = -90f, rKnee = 90f),
                FigurePose(trunk = -90f, lHip = 30f, rHip = 8f, lKnee = -90f, rKnee = 90f),
            ),
        ),
        ex(
            "hip_side_lying_raise", "Side-Lying Leg Lift", HIPS, Level.MEDIUM,
            "Hip strength after hip replacement (when cleared)",
            listOf(
                "Lie on your side, bottom knee bent for support.",
                "Keep the top leg straight and lift it towards the ceiling.",
                "Lower slowly.",
            ),
            reps = 10, sets = 2, perSide = true,
            guide = front(Posture.LIE, SIDE_LYING, SIDE_LYING.copy(rHip = 32f)),
        ),
        ex(
            "hip_slr", "Straight Leg Raise", HIPS, Level.MEDIUM,
            "Thigh strength after knee or hip surgery",
            listOf(
                "Lie on your back. Bend one knee with the foot flat.",
                "Tighten the thigh of the straight leg.",
                "Lift the straight leg to the height of the other knee, then lower slowly.",
            ),
            reps = 10, sets = 2, perSide = true,
            rule = AngleRule(SHOULDER, HIP, ANKLE, start = 165f, target = 145f,
                hint = "Lie side-on to the phone, with the phone on the floor 2 metres away."),
            guide = side(
                Posture.LIE,
                FigurePose(trunk = -90f, lHip = 45f, lKnee = 100f),
                FigurePose(trunk = -90f, lHip = 45f, lKnee = 100f, rHip = 45f),
            ),
        ),
        ex(
            "hip_flexor_stretch", "Hip Front Stretch", HIPS, Level.MEDIUM,
            "Tight hips from sitting, upright walking",
            listOf(
                "Stand in a split stance holding a chair, one foot forward and one back.",
                "Bend the front knee and gently push your hips forward.",
                "Feel the stretch at the front of the back hip. Hold.",
            ),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 20, perSide = true,
            guide = side(
                Posture.STAND,
                STANDING.copy(rHip = 20f, lHip = -15f),
                STANDING.copy(rHip = 40f, rKnee = 45f, lHip = -25f, lKnee = 5f),
            ),
        ),

        // ───────────────────────── KNEES ─────────────────────────
        ex(
            "knee_quad_set", "Thigh Tightening", KNEES, Level.EASY,
            "Knee arthritis, after knee surgery",
            listOf(
                "Sit with your leg straight out on a stool, or lie on your back.",
                "Push the back of your knee down and tighten your thigh muscle.",
                "Hold, then relax.",
            ),
            mode = Mode.HOLD, reps = 10, sets = 1, hold = 5, perSide = true,
            guide = side(Posture.SIT, SIT_LEG_OUT, SIT_LEG_OUT.copy(rAnkle = -15f)),
        ),
        ex(
            "knee_extension", "Seated Knee Straightening", KNEES, Level.EASY,
            "Knee strength, walking and climbing stairs",
            listOf(
                "Sit tall in a chair with feet flat on the floor.",
                "Slowly straighten one knee until the leg is level.",
                "Hold for a moment, then lower slowly.",
            ),
            listOf("Add a light ankle weight when it becomes easy."),
            reps = 10, sets = 2, perSide = true,
            rule = AngleRule(HIP, KNEE, ANKLE, start = 115f, target = 155f,
                hint = "Sit side-on to the phone so your whole body and chair are visible."),
            guide = side(Posture.SIT, SITTING, SITTING.copy(rKnee = 4f)),
        ),
        ex(
            "knee_heel_slide", "Heel Slides", KNEES, Level.EASY,
            "Knee bending after surgery or injury",
            listOf(
                "Lie on your back with legs straight.",
                "Slowly slide one heel towards your bottom, bending the knee.",
                "Slide it back until the leg is straight.",
            ),
            reps = 10, sets = 2, perSide = true,
            rule = AngleRule(HIP, KNEE, ANKLE, start = 160f, target = 110f,
                hint = "Lie side-on to the phone, with the phone on the floor 2 metres away."),
            guide = side(Posture.LIE, SUPINE, SUPINE.copy(rHip = 60f, rKnee = 110f)),
        ),
        ex(
            "knee_mini_squat", "Mini Squats", KNEES, Level.MEDIUM,
            "Leg strength for stairs and standing",
            listOf(
                "Stand holding the back of a chair, feet hip-width apart.",
                "Bend your knees a little, keeping your heels down.",
                "Straighten back up.",
            ),
            listOf("Knees should stay over your toes, not past them."),
            reps = 10, sets = 2,
            rule = AngleRule(HIP, KNEE, ANKLE, start = 160f, target = 135f, side = SideMode.AVG, hint = SIDE_ON),
            guide = side(
                Posture.STAND, STANDING,
                STANDING.copy(trunk = 15f, lHip = 40f, rHip = 40f, lKnee = 50f, rKnee = 50f, lArm = 50f, rArm = 50f),
            ),
        ),
        ex(
            "knee_ham_curl", "Standing Knee Bend", KNEES, Level.EASY,
            "Back-of-thigh strength, walking",
            listOf(
                "Stand holding the back of a chair.",
                "Bend one knee, bringing your heel up towards your bottom.",
                "Lower slowly.",
            ),
            reps = 10, sets = 2, perSide = true,
            rule = AngleRule(HIP, KNEE, ANKLE, start = 150f, target = 105f, hint = SIDE_ON),
            guide = side(Posture.STAND, STANDING, STANDING.copy(rKnee = 90f)),
        ),
        ex(
            "knee_step_up", "Step-Ups", KNEES, Level.MEDIUM,
            "Climbing stairs and kerbs",
            listOf(
                "Stand facing the bottom stair, holding the rail.",
                "Step up with one foot, then bring the other foot up.",
                "Step down one foot at a time. Lead with the stronger leg going up.",
            ),
            reps = 10, sets = 2,
            guide = side(Posture.STAND, STANDING, STANDING.copy(rHip = 45f, rKnee = 50f)),
        ),
        ex(
            "knee_ham_stretch", "Back-of-Thigh Stretch", KNEES, Level.EASY,
            "Tight hamstrings, knee and back stiffness",
            listOf(
                "Sit on the edge of a chair with one leg straight out, heel on the floor.",
                "Sit tall and lean forward from your hips until you feel a stretch.",
                "Hold, then sit back up.",
            ),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 20, perSide = true,
            guide = side(
                Posture.SIT,
                SIT_LEG_OUT.copy(rHip = 70f),
                SIT_LEG_OUT.copy(trunk = 25f, lHip = 115f, rHip = 95f, lArm = 40f, rArm = 40f),
            ),
        ),
        ex(
            "knee_wall_sit", "Wall Sit", KNEES, Level.HARD,
            "Thigh endurance",
            listOf(
                "Stand with your back against a wall, feet a step away from it.",
                "Slide down until your knees are slightly bent.",
                "Hold, then slide back up.",
            ),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 15,
            guide = side(Posture.STAND, STANDING.copy(lHip = 55f, rHip = 55f, lKnee = 55f, rKnee = 55f)),
        ),

        // ───────────────────────── ANKLES & FEET ─────────────────────────
        ex(
            "ank_pumps", "Ankle Pumps", FEET, Level.EASY,
            "Swollen ankles, circulation, after surgery",
            listOf(
                "Sit or lie with your legs out straight.",
                "Point your toes away from you.",
                "Then pull them back towards you. Keep a steady rhythm.",
            ),
            reps = 20, sets = 2,
            guide = side(Posture.SIT, SIT_LEG_OUT.copy(rAnkle = -20f), SIT_LEG_OUT.copy(rAnkle = 35f)),
        ),
        ex(
            "ank_circles", "Ankle Circles", FEET, Level.EASY,
            "Stiff ankles",
            listOf(
                "Sit with one foot lifted slightly off the floor.",
                "Slowly draw circles with your toes.",
                "Do half clockwise and half anticlockwise, then change foot.",
            ),
            reps = 10, sets = 1, perSide = true,
            guide = side(Posture.SIT, SIT_LEG_OUT.copy(rAnkle = -15f), SIT_LEG_OUT.copy(rAnkle = 25f)),
        ),
        ex(
            "ank_heel_raise", "Heel Raises", FEET, Level.EASY,
            "Calf strength, balance, walking",
            listOf(
                "Stand holding the back of a chair.",
                "Rise up onto your toes.",
                "Lower your heels slowly back down.",
            ),
            reps = 12, sets = 2,
            rule = AngleRule(KNEE, ANKLE, FOOT, start = 124f, target = 136f, side = SideMode.AVG, hint = SIDE_ON),
            guide = side(Posture.STAND, STANDING, STANDING.copy(lAnkle = 35f, rAnkle = 35f)),
        ),
        ex(
            "ank_toe_raise", "Toe Raises", FEET, Level.EASY,
            "Shin strength, avoiding trips",
            listOf(
                "Stand holding the back of a chair.",
                "Lift your toes and the front of your feet off the floor.",
                "Lower slowly.",
            ),
            reps = 12, sets = 2,
            guide = side(Posture.STAND, STANDING, STANDING.copy(lAnkle = -22f, rAnkle = -22f, trunk = -3f)),
        ),
        ex(
            "ank_alphabet", "Ankle Alphabet", FEET, Level.EASY,
            "Ankle movement after a sprain",
            listOf(
                "Sit with one leg out straight and the foot off the floor.",
                "Using your big toe as a pen, draw the letters A to Z in the air.",
                "Keep going for the full time, then change feet.",
            ),
            mode = Mode.HOLD, reps = 1, sets = 1, hold = 60, perSide = true,
            guide = side(Posture.SIT, SIT_LEG_OUT.copy(rAnkle = -10f), SIT_LEG_OUT.copy(rAnkle = 30f)),
        ),
        ex(
            "foot_towel_curl", "Towel Scrunches", FEET, Level.EASY,
            "Foot arch strength, plantar fasciitis",
            listOf(
                "Sit with a towel flat under your bare foot.",
                "Curl your toes to scrunch the towel towards you.",
                "Relax and repeat for the full time.",
            ),
            mode = Mode.HOLD, reps = 2, sets = 1, hold = 30, perSide = true,
            guide = side(Posture.SIT, SITTING, SITTING.copy(rAnkle = 12f)),
        ),
        ex(
            "calf_stretch", "Calf Stretch at Wall", FEET, Level.EASY,
            "Tight calves, heel pain, cramps",
            listOf(
                "Stand facing a wall with hands on it.",
                "Step one foot back, keeping the heel down and knee straight.",
                "Bend the front knee and lean in until you feel a stretch in the back calf. Hold.",
            ),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 30, perSide = true,
            guide = side(
                Posture.STAND,
                FigurePose(trunk = 12f, rHip = 25f, rKnee = 20f, lHip = -20f, lArm = 100f, rArm = 100f),
                FigurePose(trunk = 18f, rHip = 35f, rKnee = 40f, lHip = -28f, lArm = 95f, rArm = 95f),
            ),
        ),

        // ───────────────────────── BALANCE & DAILY STRENGTH ─────────────────────────
        ex(
            "bal_sit_to_stand", "Sit to Stand", BALANCE, Level.MEDIUM,
            "Getting up from chairs, the toilet and the car",
            listOf(
                "Sit near the front of a firm chair, feet slightly behind your knees.",
                "Lean forward, then push through your legs to stand up.",
                "Stand tall, then slowly sit back down with control.",
            ),
            listOf("Use your hands on the armrests at first. Cross arms on your chest to make it harder."),
            reps = 8, sets = 2,
            rule = AngleRule(HIP, KNEE, ANKLE, start = 115f, target = 158f, side = SideMode.AVG,
                hint = "Put the chair side-on to the phone so your whole body is visible."),
            guide = side(Posture.SIT, SITTING.copy(trunk = 14f, lHip = 104f, rHip = 104f, lArm = 30f, rArm = 30f), STANDING),
        ),
        ex(
            "bal_march", "Standing Marching", BALANCE, Level.EASY,
            "Balance, hip strength, walking",
            listOf(
                "Stand holding the back of a chair or kitchen counter.",
                "Lift one knee up in front of you, then lower it.",
                "Then lift the other knee. Each lift counts.",
            ),
            reps = 20, sets = 2,
            rule = AngleRule(SHOULDER, HIP, KNEE, start = 155f, target = 125f, hint = SIDE_ON),
            guide = side(Posture.STAND, STANDING, STANDING.copy(rHip = 85f, rKnee = 90f)),
        ),
        ex(
            "bal_single_leg", "Stand on One Leg", BALANCE, Level.MEDIUM,
            "Fall prevention",
            listOf(
                "Stand next to a counter with one hand resting on it.",
                "Lift one foot slightly off the floor.",
                "Hold your balance, then put it down and change legs.",
            ),
            listOf("Use fingertip support only. Try without support once it feels easy."),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 10, perSide = true,
            guide = side(Posture.STAND, STANDING, STANDING.copy(rHip = 30f, rKnee = 60f)),
        ),
        ex(
            "bal_tandem", "Heel-to-Toe Stand", BALANCE, Level.MEDIUM,
            "Steady balance on narrow paths",
            listOf(
                "Stand next to a counter.",
                "Place one foot directly in front of the other, heel touching toes.",
                "Hold your balance, then swap which foot is in front.",
            ),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 15,
            guide = side(Posture.STAND, STANDING.copy(rHip = 10f, lHip = -8f)),
        ),
        ex(
            "bal_heel_toe_walk", "Heel-to-Toe Walk", BALANCE, Level.HARD,
            "Walking balance",
            listOf(
                "Stand beside a wall or counter for support.",
                "Walk forward placing the heel of one foot just in front of the toes of the other.",
                "Count each step. Turn carefully and walk back.",
            ),
            reps = 10, sets = 2,
            guide = side(
                Posture.STAND,
                STANDING.copy(rHip = 15f, lHip = -10f, lArm = 20f),
                STANDING.copy(rHip = -10f, lHip = 15f, rArm = 20f),
            ),
        ),
        ex(
            "bal_side_step", "Side Steps", BALANCE, Level.EASY,
            "Sideways stability, stepping around obstacles",
            listOf(
                "Stand facing a counter with both hands on it.",
                "Take a step to the side, then bring the other foot to meet it.",
                "Step back the other way.",
            ),
            reps = 10, sets = 2,
            guide = front(Posture.STAND, F_STANDING, F_STANDING.copy(rHip = 20f)),
        ),

        // ───────────────────────── BREATHING & RELAXATION ─────────────────────────
        ex(
            "breath_diaphragm", "Belly Breathing", BREATHING, Level.EASY,
            "Breathlessness, anxiety, recovery after illness",
            listOf(
                "Sit comfortably or lie with knees bent. Put one hand on your belly.",
                "Breathe in slowly through your nose so your belly rises.",
                "Breathe out slowly through your mouth. Follow the voice.",
            ),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 60, rest = 15,
            guide = side(Posture.SIT, SITTING.copy(rArm = 15f, rElbow = 80f), SITTING.copy(rArm = 15f, rElbow = 80f, trunk = -4f, shrug = 2f)),
            breath = 10,
        ),
        ex(
            "breath_pursed", "Pursed-Lip Breathing", BREATHING, Level.EASY,
            "COPD, breathlessness when walking",
            listOf(
                "Sit and relax your shoulders.",
                "Breathe in through your nose for about 2 seconds.",
                "Breathe out slowly through pursed lips, as if blowing out a candle, for about 4 seconds.",
            ),
            mode = Mode.HOLD, reps = 3, sets = 1, hold = 60, rest = 15,
            guide = side(Posture.SIT, SITTING, SITTING.copy(shrug = 2f, head = -4f)),
            breath = 6,
        ),
        ex(
            "breath_arm_raise", "Deep Breath with Arm Raise", BREATHING, Level.EASY,
            "Chest expansion, posture, gentle energy",
            listOf(
                "Stand or sit tall with arms by your sides.",
                "Breathe in as you raise both arms out and up.",
                "Breathe out as you lower them slowly.",
            ),
            reps = 8, sets = 1,
            rule = AngleRule(HIP, SHOULDER, ELBOW, start = 40f, target = 130f, side = SideMode.AVG, hint = FACE_ON),
            guide = front(Posture.STAND, F_STANDING, F_STANDING.copy(lArm = 160f, rArm = 160f)),
        ),
        ex(
            "relax_tense_release", "Tense and Relax", BREATHING, Level.EASY,
            "Stress, poor sleep, muscle tension",
            listOf(
                "Sit or lie comfortably and close your eyes.",
                "Breathe in and tense your shoulders and fists.",
                "Breathe out and let everything go loose. Notice the difference.",
            ),
            mode = Mode.HOLD, reps = 2, sets = 1, hold = 60, rest = 10,
            guide = side(Posture.SIT, SITTING, SITTING.copy(shrug = 5f, rElbow = 30f)),
            breath = 8,
        ),
    )

    private val byIdMap = all.associateBy { it.id }

    fun byId(id: String): Exercise? = byIdMap[id]

    fun byCategory(c: Category): List<Exercise> = all.filter { it.category == c }

    fun search(q: String): List<Exercise> {
        val s = q.trim()
        if (s.isEmpty()) return emptyList()
        return all.filter {
            it.name.contains(s, true) || it.goodFor.contains(s, true) || it.category.title.contains(s, true)
        }
    }

    /** A gentle starter plan for first launch. */
    val starterPlan = listOf("bal_sit_to_stand", "bal_march", "knee_extension", "ank_heel_raise", "breath_diaphragm")

    val recommended = listOf("bal_single_leg", "sh_flexion", "arm_wall_pushup", "ank_pumps", "neck_chin_tuck")
}
