/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | جلوه تعاملی سه‌بعدی (3D Tilt Effect)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  چرخش پرسپکتیوی کارت‌ها بر اساس موقعیت لمس + برگشت فنری هنگام رها شدن
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * افکت 3D Tilt — حداکثر چرخش [maxTilt] درجه حول محورهای X و Y،
 * به‌صورت زنده با حرکت انگشت و بازگشت فنری پس از رها شدن.
 */
fun Modifier.tilt3D(maxTilt: Float = 10f, enabled: Boolean = true): Modifier = composed {
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()

    this
        .onSizeChanged { newSize -> viewSize = newSize }
        .graphicsLayer {
            rotationX = tiltX
            rotationY = tiltY
            cameraDistance = 12f * density
            val active = tiltX != 0f || tiltY != 0f
            scaleX = if (active) 1.04f else 1f
            scaleY = if (active) 1.04f else 1f
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                // ردگیری زنده حرکت انگشت تا لحظه بلند شدن
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val pointer = event.changes.firstOrNull() ?: break
                    if (!pointer.pressed) break
                    val relX = (pointer.position.x / max(viewSize.width.toFloat(), 1f)).coerceIn(0f, 1f)
                    val relY = (pointer.position.y / max(viewSize.height.toFloat(), 1f)).coerceIn(0f, 1f)
                    tiltY = (relX - 0.5f) * 2f * maxTilt
                    tiltX = -(relY - 0.5f) * 2f * maxTilt
                }
                // بازگشت فنری به حالت تخت
                val startX = tiltX
                val startY = tiltY
                scope.launch {
                    animate(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) { value, _ ->
                        tiltX = startX * value
                        tiltY = startY * value
                    }
                    tiltX = 0f
                    tiltY = 0f
                }
            }
        }
}
