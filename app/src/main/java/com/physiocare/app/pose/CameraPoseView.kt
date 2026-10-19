package com.physiocare.app.pose

import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.max

/** Runs ML Kit pose detection on each camera frame (latest frame only, so it never lags). */
private class PoseAnalyzer(
    private val onResult: (Pose?, Int, Int) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    @Volatile
    var closed = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val media = imageProxy.image
        if (media == null || closed) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val upright = rotation % 180 == 0
        val w = if (upright) imageProxy.width else imageProxy.height
        val h = if (upright) imageProxy.height else imageProxy.width
        val input = InputImage.fromMediaImage(media, rotation)
        detector.process(input)
            .addOnSuccessListener { pose -> if (!closed) onResult(pose, w, h) }
            .addOnFailureListener { if (!closed) onResult(null, w, h) }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        closed = true
        detector.close()
    }
}

private val CONNECTIONS = listOf(
    11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,
    11 to 23, 12 to 24, 23 to 24,
    23 to 25, 25 to 27, 24 to 26, 26 to 28,
    27 to 29, 29 to 31, 27 to 31, 28 to 30, 30 to 32, 28 to 32,
)

private val TRACKED = Color(0xFFF5B700)

@Composable
fun CameraPoseView(
    frontCamera: Boolean,
    highlight: Set<Int>,
    onPose: (Pose?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnPose by rememberUpdatedState(onPose)
    var pose by remember { mutableStateOf<Pose?>(null) }
    var imageSize by remember { mutableStateOf(IntSize(0, 0)) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(frontCamera, lifecycleOwner) {
        var disposed = false
        val analyzer = PoseAnalyzer { p, w, h ->
            pose = p
            imageSize = IntSize(w, h)
            currentOnPose(p)
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (disposed) return@addListener
            val provider = future.get()
            val ratio = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
            val preview = Preview.Builder()
                .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(ratio).build())
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(ratio)
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(640, 480),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, analyzer) }
            val wanted = if (frontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            val fallback = if (frontCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                provider.unbindAll()
                val selector = if (provider.hasCamera(wanted)) wanted else fallback
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (e: Exception) {
                Log.e("PhysioCare", "Camera start failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            analyzer.close()
            if (future.isDone) {
                try {
                    future.get().unbindAll()
                } catch (_: Exception) {
                }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val p = pose ?: return@Canvas
            val iw = imageSize.width.toFloat()
            val ih = imageSize.height.toFloat()
            if (iw <= 0f || ih <= 0f) return@Canvas
            val scale = max(size.width / iw, size.height / ih)
            val dx = (size.width - iw * scale) / 2f
            val dy = (size.height - ih * scale) / 2f
            fun map(lm: PoseLandmark): Offset {
                var x = lm.position.x * scale + dx
                if (frontCamera) x = size.width - x
                return Offset(x, lm.position.y * scale + dy)
            }
            val stroke = size.minDimension / 70f
            for ((a, b) in CONNECTIONS) {
                val la = p.getPoseLandmark(a) ?: continue
                val lb = p.getPoseLandmark(b) ?: continue
                if (la.inFrameLikelihood < 0.5f || lb.inFrameLikelihood < 0.5f) continue
                val hot = a in highlight && b in highlight
                drawLine(
                    color = if (hot) TRACKED else Color.White.copy(alpha = 0.85f),
                    start = map(la), end = map(lb),
                    strokeWidth = if (hot) stroke * 1.8f else stroke,
                    cap = StrokeCap.Round,
                )
            }
            for (lm in p.allPoseLandmarks) {
                if (lm.landmarkType < 11 || lm.inFrameLikelihood < 0.5f) continue
                val hot = lm.landmarkType in highlight
                drawCircle(if (hot) TRACKED else Color.White, radius = if (hot) stroke * 1.6f else stroke, center = map(lm))
            }
        }
    }
}
