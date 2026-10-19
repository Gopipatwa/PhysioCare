package com.physiocare.app.data

import com.physiocare.app.data.Ability.LITTLE
import com.physiocare.app.data.Ability.NONE
import com.physiocare.app.data.Ability.WEAK

/**
 * Chooses exercises for a paralysis type and how much the person can move.
 * Lists are in a sensible order to do them in.
 */
object Recovery {

    private val strokeNone = listOf(
        "rec_pa_arm_raise", "rec_pa_elbow", "rec_pa_wrist", "rec_pa_knee", "rec_pa_ankle",
        "rec_pa_hip_out", "rec_rolling", "breath_diaphragm",
    )
    private val strokeLittle = listOf(
        "rec_clasp_raise", "rec_table_slide", "rec_finger_lift", "rec_mirror", "rec_pa_wrist",
        "rec_weight_shift", "rec_forward_reach", "knee_heel_slide", "back_rotation", "back_bridge",
        "ank_pumps", "hand_squeeze",
    )
    private val strokeWeak = listOf(
        "bal_sit_to_stand", "knee_extension", "hip_seated_march", "rec_supported_stand",
        "rec_weight_shift_stand", "rec_step_weak", "sh_flexion", "arm_bicep_curl", "hand_fist",
        "hand_thumb_touch", "wrist_turns", "ank_heel_raise", "bal_march", "bal_side_step",
    )

    // Paraplegia: arms are working, so upper-body strength and pressure care are always included.
    private val sciLegsAlways = listOf(
        "sci_pressure_relief", "arm_chair_pushup", "sh_overhead_press", "sh_abduction",
        "arm_bicep_curl", "sh_scap_squeeze", "rec_forward_reach", "sci_huff", "breath_diaphragm",
    )
    private val sciLegsNone = listOf("rec_pa_knee", "rec_pa_ankle", "rec_pa_hip_out", "rec_ham_helper")
    private val sciLegsLittle = listOf("knee_quad_set", "knee_heel_slide", "ank_pumps", "back_bridge", "rec_weight_shift")
    private val sciLegsWeak = listOf("knee_extension", "bal_sit_to_stand", "rec_supported_stand", "rec_weight_shift_stand", "rec_step_weak")

    // Tetraplegia: breathing, pressure care and leg range of motion are always included.
    private val sciAllAlways = listOf(
        "sci_huff", "breath_diaphragm", "sci_pressure_relief", "neck_chin_tuck", "sh_shrugs",
        "rec_pa_knee", "rec_pa_ankle", "rec_pa_hip_out", "rec_ham_helper",
    )
    private val sciAllNone = listOf("rec_pa_arm_raise", "rec_pa_elbow", "rec_pa_wrist")
    private val sciAllLittle = listOf("sh_scap_squeeze", "rec_table_slide", "rec_finger_lift", "rec_pa_wrist", "rec_forward_reach", "rec_weight_shift")
    private val sciAllWeak = listOf("sh_flexion", "sh_abduction", "arm_bicep_curl", "hand_squeeze", "hand_fist", "wrist_flex_ext", "arm_chair_pushup")

    private val bellsNone = listOf("face_massage", "face_eyes", "face_brow", "face_smile")
    private val bellsLittle = listOf("face_massage", "face_brow", "face_eyes", "face_smile", "face_pucker", "face_nose")
    private val bellsWeak = listOf("face_brow", "face_eyes", "face_smile", "face_pucker", "face_puff", "face_nose", "face_vowels")

    private val nerveNone = listOf("rec_pa_arm_raise", "rec_pa_elbow", "rec_pa_wrist", "rec_pa_knee", "rec_pa_ankle", "breath_diaphragm", "sci_huff")
    private val nerveLittle = listOf(
        "ank_pumps", "knee_quad_set", "knee_heel_slide", "hand_squeeze", "rec_table_slide",
        "back_bridge", "rec_forward_reach", "breath_diaphragm",
    )
    private val nerveWeak = listOf(
        "bal_sit_to_stand", "knee_extension", "hip_seated_march", "sh_flexion", "arm_bicep_curl",
        "ank_heel_raise", "bal_march", "rec_supported_stand",
    )

    fun exercisesFor(c: Condition, a: Ability): List<String> {
        val ids = when (c) {
            Condition.STROKE -> when (a) { NONE -> strokeNone; LITTLE -> strokeLittle; WEAK -> strokeWeak }
            Condition.SCI_LEGS -> (when (a) { NONE -> sciLegsNone; LITTLE -> sciLegsLittle; WEAK -> sciLegsWeak }) + sciLegsAlways
            Condition.SCI_ALL -> sciAllAlways.take(2) + (when (a) { NONE -> sciAllNone; LITTLE -> sciAllLittle; WEAK -> sciAllWeak }) + sciAllAlways.drop(2)
            Condition.BELLS -> when (a) { NONE -> bellsNone; LITTLE -> bellsLittle; WEAK -> bellsWeak }
            Condition.NERVE -> when (a) { NONE -> nerveNone; LITTLE -> nerveLittle; WEAK -> nerveWeak }
        }
        return ids.distinct().filter { ExerciseLibrary.byId(it) != null }
    }

    /** The exercises to move on to when the person gets stronger. */
    fun nextStep(c: Condition, a: Ability): List<String> {
        val next = when (a) { NONE -> LITTLE; LITTLE -> WEAK; WEAK -> return emptyList() }
        val now = exercisesFor(c, a).toSet()
        return exercisesFor(c, next).filter { it !in now }
    }

    fun safety(c: Condition): List<String> = when (c) {
        Condition.STROKE -> listOf(
            "Support the weak arm at the elbow and wrist. Never pull on the hand or lift under the armpit.",
            "Raise the weak arm only to shoulder height unless your physiotherapist says otherwise.",
            "Have someone beside you for all standing exercises.",
            "Call emergency services at once for new face drooping, arm weakness or slurred speech.",
        )
        Condition.SCI_LEGS, Condition.SCI_ALL -> listOf(
            "If your injury is at chest level or higher: a sudden pounding headache, flushed or sweaty skin above the injury, or a blocked nose can be autonomic dysreflexia. Sit upright, loosen tight clothing, check the bladder or catheter, and get urgent medical help.",
            "Check your skin every day for red or dark marks, especially over the bottom, hips and heels.",
            "Move paralysed limbs gently and slowly – they can't tell you when they are overstretched.",
        )
        Condition.BELLS -> listOf(
            "Protect the eye on the weak side: use eye drops, wear glasses outdoors, and ask your doctor about taping it closed at night.",
            "Do small, gentle movements in front of a mirror. Forcing big or strong movements can cause unwanted linked movements later.",
            "See a doctor quickly if weakness spreads beyond the face, or you have other new symptoms.",
        )
        Condition.NERVE -> listOf(
            "Stop before you get tired. Overdoing it can slow recovery.",
            "If you feel much weaker the next day, do less next time.",
            "Contact your doctor straight away about new breathing difficulty or weakness getting worse.",
        )
    }

    /** Camera counting needs less movement when the person can only move a little. */
    fun smallMoves(profile: RecoveryProfile?): Boolean = profile != null && profile.ability != WEAK
}
