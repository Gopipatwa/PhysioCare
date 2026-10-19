package com.physiocare.app.pose

import android.graphics.PointF
import com.google.mlkit.vision.pose.Pose
import com.physiocare.app.data.AngleRule
import com.physiocare.app.data.SideMode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min

object PoseMath {
    private const val MIN_LIKELIHOOD = 0.5f

    /** Angle in degrees at point b, between a-b-c (0..180). */
    fun angle(a: PointF, b: PointF, c: PointF): Float {
        val r = atan2((c.y - b.y).toDouble(), (c.x - b.x).toDouble()) -
            atan2((a.y - b.y).toDouble(), (a.x - b.x).toDouble())
        var d = abs(Math.toDegrees(r))
        if (d > 180.0) d = 360.0 - d
        return d.toFloat()
    }

    /** Joint angle for the left and right side (null when that side is not clearly visible). */
    fun ruleAngles(pose: Pose, rule: AngleRule): Pair<Float?, Float?> {
        fun one(left: Boolean): Float? {
            val ia = if (left != rule.aOpposite) rule.a.left else rule.a.right
            val ib = if (left) rule.b.left else rule.b.right
            val ic = if (left) rule.c.left else rule.c.right
            val la = pose.getPoseLandmark(ia) ?: return null
            val lb = pose.getPoseLandmark(ib) ?: return null
            val lc = pose.getPoseLandmark(ic) ?: return null
            val conf = min(la.inFrameLikelihood, min(lb.inFrameLikelihood, lc.inFrameLikelihood))
            if (conf < MIN_LIKELIHOOD) return null
            return angle(la.position, lb.position, lc.position)
        }
        return one(true) to one(false)
    }

    fun ruleLandmarks(rule: AngleRule): Set<Int> {
        val s = mutableSetOf(rule.b.left, rule.b.right, rule.c.left, rule.c.right, rule.a.left, rule.a.right)
        return s
    }
}

/**
 * Counts a repetition when the joint angle travels from `start` past `target`
 * and back past `start`. Smoothed to ignore camera jitter.
 */
class RepCounter(private val rule: AngleRule, reach: Float = 1f) {
    /** reach < 1 = "small movements" mode: the target is moved closer to the start position. */
    private val target = rule.start + (rule.target - rule.start) * reach.coerceIn(0.2f, 1f)
    private val increasing = target > rule.start
    private val reachedSide = BooleanArray(2)
    private val smooth = FloatArray(2) { Float.NaN }

    var count = 0
        private set
    var progress = 0f
        private set
    val reached: Boolean get() = reachedSide[0] || reachedSide[1]

    /** Returns true when a new repetition was completed on this frame. */
    fun update(left: Float?, right: Float?): Boolean {
        val inputs: List<Float?> = if (rule.side == SideMode.AVG) {
            val v = listOfNotNull(left, right)
            listOf(if (v.isEmpty()) null else v.average().toFloat())
        } else {
            listOf(left, right)
        }
        var counted = false
        var best = 0f
        inputs.forEachIndexed { i, raw ->
            if (raw == null) return@forEachIndexed
            val s = if (smooth[i].isNaN()) raw else smooth[i] * 0.55f + raw * 0.45f
            smooth[i] = s
            val pastTarget = if (increasing) s >= target else s <= target
            val backAtStart = if (increasing) s <= rule.start else s >= rule.start
            if (pastTarget) {
                reachedSide[i] = true
            } else if (backAtStart && reachedSide[i]) {
                reachedSide[i] = false
                count++
                counted = true
            }
            val p = ((s - rule.start) / (target - rule.start)).coerceIn(0f, 1f)
            if (p > best) best = p
        }
        progress = best
        return counted
    }
}
