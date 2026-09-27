package com.vitalis.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Topographic contour lines: the brand's decorative motif (design canvas). Decorative only. */
@Composable
fun TopoLines(color: Color, modifier: Modifier = Modifier, centerX: Float = .5f, centerY: Float = .5f, rings: Int = 12) = Canvas(modifier) {
    val cx = size.width * centerX
    val cy = size.height * centerY
    val stroke = Stroke(1.dp.toPx())
    repeat(rings) { i ->
        val r = (26 + i * 30).dp.toPx()
        val path = Path()
        for (k in 0..48) {
            val a = k / 48f * 2 * PI.toFloat()
            val wob = 1 + .16f * sin(3 * a + i * .35f) + .08f * sin(5 * a + i * .2f)
            val x = cx + r * 1.25f * wob * cos(a)
            val y = cy + r * wob * sin(a)
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = stroke)
    }
}
