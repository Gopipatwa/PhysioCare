package com.physiocare.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.physiocare.app.data.FacePose
import com.physiocare.app.data.FigurePose
import com.physiocare.app.data.Guide
import com.physiocare.app.data.Posture
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Body proportions in "figure units".
private const val TORSO = 58f
private const val NECK = 9f
private const val HEAD_R = 12f
private const val UPPER_ARM = 30f
private const val FOREARM = 28f
private const val THIGH = 42f
private const val SHIN = 40f
private const val FOOT = 13f

/**
 * A moving demonstration of the exercise, drawn from two key poses.
 * Tiny and asset-free: every exercise in the library gets an animation without images or video.
 */
@Composable
fun ExerciseFigure(guide: Guide, modifier: Modifier = Modifier, animate: Boolean = true, durationMs: Int = 1700) {
    val transition = rememberInfiniteTransition(label = "figure")
    val t = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "t",
    )
    val main = MaterialTheme.colorScheme.primary
    val dim = main.copy(alpha = 0.38f)
    val prop = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.secondary
    Canvas(modifier) {
        val k = if (animate) t.value else 1f
        if (guide.posture == Posture.FACE) {
            val fa = guide.faceA ?: FacePose()
            val fb = guide.faceB ?: fa
            drawFace(fa.lerp(fb, k), main, accent, prop)
            return@Canvas
        }
        drawFigure(guide.a.lerp(guide.b, k), guide.posture, guide.front, main, dim, prop, accent)
    }
}

private fun dir(deg: Float): Offset {
    val r = Math.toRadians(deg.toDouble())
    return Offset(sin(r).toFloat(), cos(r).toFloat())
}

private fun DrawScope.drawFigure(
    p: FigurePose,
    posture: Posture,
    front: Boolean,
    main: Color,
    dim: Color,
    prop: Color,
    accent: Color,
) {
    val tr = p.trunk
    val up = dir(180f - tr)
    val perp = Offset(-up.y, up.x)
    val shoulderW = if (front) 16f else 0f
    val hipW = if (front) 10f else 0f

    val neck = up * TORSO
    val shoulderMid = neck - up * 3f + up * p.shrug
    val lSh = shoulderMid - perp * shoulderW
    val rSh = shoulderMid + perp * shoulderW
    val head = neck + dir(180f - tr - p.head) * (NECK + HEAD_R)
    val lHp = Offset.Zero - perp * hipW
    val rHp = perp * hipW

    fun sign(left: Boolean) = if (front && left) -1f else 1f
    fun abs(rel: Float, left: Boolean) = sign(left) * rel - tr

    val lEl = lSh + dir(abs(p.lArm, true)) * UPPER_ARM
    val lHa = lEl + dir(abs(p.lArm + p.lElbow, true)) * FOREARM
    val rEl = rSh + dir(abs(p.rArm, false)) * UPPER_ARM
    val rHa = rEl + dir(abs(p.rArm + p.rElbow, false)) * FOREARM

    val thigh = if (front && posture == Posture.SIT) 12f else THIGH
    val lKn = lHp + dir(abs(p.lHip, true)) * thigh
    val lAn = lKn + dir(abs(p.lHip - p.lKnee, true)) * SHIN
    val lTo = lAn + dir(abs(p.lHip - p.lKnee + 90f - p.lAnkle, true)) * FOOT
    val rKn = rHp + dir(abs(p.rHip, false)) * thigh
    val rAn = rKn + dir(abs(p.rHip - p.rKnee, false)) * SHIN
    val rTo = rAn + dir(abs(p.rHip - p.rKnee + 90f - p.rAnkle, false)) * FOOT

    // ---- Place the figure on the floor ----
    val s = min(size.height / 215f, size.width / 235f)
    val floorY = size.height - 8f * s
    val groundY: Float
    val anchorX: Float
    val screenX: Float
    when (posture) {
        Posture.LIE -> {
            groundY = listOf(lSh, rSh, lEl, rEl, lHa, rHa, Offset.Zero, lHp, rHp, lKn, rKn, lAn, rAn)
                .maxOf { it.y }
            anchorX = 0f
            screenX = size.width / 2f + 8f * s
        }
        else -> {
            val leftLow = maxOf(lAn.y, lTo.y)
            val rightLow = maxOf(rAn.y, rTo.y)
            groundY = maxOf(leftLow, rightLow)
            if (front) {
                anchorX = 0f
                screenX = size.width / 2f
            } else {
                anchorX = if (leftLow >= rightLow - 0.5f) lAn.x else rAn.x
                screenX = size.width / 2f + if (posture == Posture.SIT) 24f * s else 0f
            }
        }
    }
    fun m(o: Offset) = Offset(screenX + (o.x - anchorX) * s, floorY - (groundY - o.y) * s)

    val stroke = 7f * s
    val propStroke = 5f * s

    // ---- Props ----
    drawLine(prop, Offset(size.width * 0.04f, floorY + stroke / 2f), Offset(size.width * 0.96f, floorY + stroke / 2f), propStroke, StrokeCap.Round)
    if (posture == Posture.SIT) {
        val seatY = floorY - 34f * s
        if (front) {
            // seen from the front the seat is at hip height and the backrest shows behind the body
            val cx = size.width / 2f
            val y = m(Offset.Zero).y + 5f * s
            drawLine(prop, Offset(cx - 30f * s, y), Offset(cx + 30f * s, y), propStroke, StrokeCap.Round)
            drawLine(prop, Offset(cx - 26f * s, y - 62f * s), Offset(cx - 26f * s, floorY), propStroke, StrokeCap.Round)
            drawLine(prop, Offset(cx + 26f * s, y - 62f * s), Offset(cx + 26f * s, floorY), propStroke, StrokeCap.Round)
        } else {
            val front0 = screenX - 32f * s
            val back0 = screenX - 72f * s
            drawLine(prop, Offset(back0, seatY), Offset(front0, seatY), propStroke, StrokeCap.Round)
            drawLine(prop, Offset(front0, seatY), Offset(front0, floorY), propStroke, StrokeCap.Round)
            drawLine(prop, Offset(back0, seatY - 52f * s), Offset(back0, floorY), propStroke, StrokeCap.Round)
        }
    }
    if (posture == Posture.LIE) {
        drawLine(prop.copy(alpha = 0.6f), Offset(size.width * 0.08f, floorY - 1f * s), Offset(size.width * 0.92f, floorY - 1f * s), 9f * s, StrokeCap.Round)
    }

    // ---- Body ----
    val back = if (front) main else dim
    fun limb(c: Color, pts: List<Offset>) {
        for (i in 0 until pts.size - 1) drawLine(c, m(pts[i]), m(pts[i + 1]), stroke, StrokeCap.Round)
    }
    // far side first (lighter), so the near side reads on top
    limb(back, listOf(lHp, lKn, lAn, lTo))
    limb(back, listOf(lSh, lEl, lHa))
    // trunk
    limb(main, listOf(Offset.Zero, neck))
    if (front) {
        limb(main, listOf(lSh, rSh))
        limb(main, listOf(lHp, rHp))
    }
    limb(main, listOf(neck, head - dir(180f - tr - p.head) * HEAD_R))
    drawCircle(main, HEAD_R * s, m(head))
    // small dot shows which way the person is facing in side views
    if (!front) drawCircle(accent, HEAD_R * s * 0.3f, m(head + dir(90f - tr - p.head) * (HEAD_R * 0.5f)))
    // near side
    limb(main, listOf(rHp, rKn, rAn, rTo))
    limb(main, listOf(rSh, rEl, rHa))
    drawCircle(main, stroke * 0.7f, m(rHa))
    drawCircle(back, stroke * 0.7f, m(lHa))
}

/** Simple front-facing face for Bell's palsy exercises. */
private fun DrawScope.drawFace(f: FacePose, main: Color, accent: Color, prop: Color) {
    val r = min(size.width, size.height) * 0.42f
    val c = Offset(size.width / 2f, size.height / 2f)
    val line = r * 0.07f
    val skin = main.copy(alpha = 0.12f)

    // cheeks puff out beyond the face outline
    val puffR = r * 0.30f * f.puff
    if (f.puff > 0.02f) {
        drawCircle(skin, puffR + r * 0.05f, c + Offset(-r * 0.62f, r * 0.30f))
        drawCircle(skin, puffR + r * 0.05f, c + Offset(r * 0.62f, r * 0.30f))
    }
    drawOval(skin, topLeft = Offset(c.x - r * 0.82f, c.y - r), size = Size(r * 1.64f, r * 2f))
    drawOval(main, topLeft = Offset(c.x - r * 0.82f, c.y - r), size = Size(r * 1.64f, r * 2f), style = Stroke(line))

    // eyebrows
    val browY = c.y - r * 0.42f - f.brow * r * 0.16f
    for (sx in listOf(-1f, 1f)) {
        drawLine(main, Offset(c.x + sx * r * 0.18f, browY), Offset(c.x + sx * r * 0.52f, browY - r * 0.04f), line, StrokeCap.Round)
        if (f.brow > 0.5f) {
            // forehead lines when raising the brows
            drawLine(prop, Offset(c.x - r * 0.3f, browY - r * 0.16f), Offset(c.x + r * 0.3f, browY - r * 0.16f), line * 0.6f, StrokeCap.Round)
        }
    }

    // eyes: an oval that closes to a line
    val eyeY = c.y - r * 0.22f
    val eyeH = r * 0.17f * (1f - f.eyeClose).coerceIn(0f, 1f)
    for (sx in listOf(-1f, 1f)) {
        val ex = c.x + sx * r * 0.35f
        if (eyeH > r * 0.03f) {
            drawOval(Color.White, topLeft = Offset(ex - r * 0.15f, eyeY - eyeH / 2f), size = Size(r * 0.3f, eyeH))
            drawOval(main, topLeft = Offset(ex - r * 0.15f, eyeY - eyeH / 2f), size = Size(r * 0.3f, eyeH), style = Stroke(line * 0.7f))
            drawCircle(main, min(r * 0.06f, eyeH / 2f), Offset(ex, eyeY))
        } else {
            drawLine(main, Offset(ex - r * 0.15f, eyeY), Offset(ex + r * 0.15f, eyeY), line, StrokeCap.Round)
        }
    }

    // nose, with wrinkles
    val noseTop = c.y - r * 0.12f
    drawLine(main, Offset(c.x, noseTop), Offset(c.x - r * 0.06f, c.y + r * 0.18f), line * 0.8f, StrokeCap.Round)
    drawLine(main, Offset(c.x - r * 0.06f, c.y + r * 0.18f), Offset(c.x + r * 0.08f, c.y + r * 0.18f), line * 0.8f, StrokeCap.Round)
    if (f.nose > 0.1f) {
        for (i in 0..1) {
            val y = noseTop + r * (0.02f + i * 0.07f)
            drawLine(prop, Offset(c.x - r * 0.12f * f.nose, y), Offset(c.x + r * 0.12f * f.nose, y), line * 0.5f, StrokeCap.Round)
        }
    }

    // mouth
    val mouthY = c.y + r * 0.48f
    val half = r * 0.32f * (1f - 0.65f * f.pucker) * (1f + 0.15f * f.smile)
    if (f.pucker > 0.5f) {
        val w = r * 0.18f
        drawOval(accent, topLeft = Offset(c.x - w / 2f, mouthY - w * 0.45f), size = Size(w, w * 0.9f))
        drawOval(main, topLeft = Offset(c.x - w / 2f, mouthY - w * 0.45f), size = Size(w, w * 0.9f), style = Stroke(line * 0.8f))
    } else if (f.mouthOpen > 0.05f) {
        val h = r * 0.3f * f.mouthOpen
        drawOval(main.copy(alpha = 0.8f), topLeft = Offset(c.x - half * 0.8f, mouthY - h / 2f), size = Size(half * 1.6f, h))
    } else {
        val corner = mouthY - f.smile * r * 0.12f
        val path = Path().apply {
            moveTo(c.x - half, corner)
            quadraticBezierTo(c.x, mouthY + f.smile * r * 0.16f + (if (f.puff > 0.2f) -r * 0.02f else 0f), c.x + half, corner)
        }
        drawPath(path, main, style = Stroke(line, cap = StrokeCap.Round))
    }
}
