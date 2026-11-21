package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.domain.model.ReferenceMethodCode

@Composable
fun ReferenceMethodDiagram(
    methodCode: ReferenceMethodCode,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val border = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val wallColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val wallStroke = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    val copperColor = Color(0xFFD97706)
    val sheathColor = MaterialTheme.colorScheme.primary
    val conduitColor = MaterialTheme.colorScheme.tertiary
    val insulationFill = Color(0xFFFDE68A).copy(alpha = 0.55f)
    val soilColor = Color(0xFF92400E).copy(alpha = 0.22f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f

            // Subtle blueprint grid
            val gridColor = wallStroke.copy(alpha = 0.08f)
            val step = minOf(w, h) / 6f
            var gx = step
            while (gx < w) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                gx += step
            }
            var gy = step
            while (gy < h) {
                drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                gy += step
            }

            when (methodCode) {
                ReferenceMethodCode.A1 -> drawMethodA(
                    isMulticore = false,
                    w = w, h = h, cx = cx, cy = cy,
                    insulationFill = insulationFill,
                    wallStroke = wallStroke,
                    conduitColor = conduitColor,
                    sheathColor = sheathColor,
                    copperColor = copperColor
                )
                ReferenceMethodCode.A2 -> drawMethodA(
                    isMulticore = true,
                    w = w, h = h, cx = cx, cy = cy,
                    insulationFill = insulationFill,
                    wallStroke = wallStroke,
                    conduitColor = conduitColor,
                    sheathColor = sheathColor,
                    copperColor = copperColor
                )
                ReferenceMethodCode.B1 -> drawMethodB(
                    isMulticore = false,
                    w = w, h = h, cx = cx, cy = cy,
                    wallColor = wallColor,
                    wallStroke = wallStroke,
                    conduitColor = conduitColor,
                    sheathColor = sheathColor,
                    copperColor = copperColor
                )
                ReferenceMethodCode.B2 -> drawMethodB(
                    isMulticore = true,
                    w = w, h = h, cx = cx, cy = cy,
                    wallColor = wallColor,
                    wallStroke = wallStroke,
                    conduitColor = conduitColor,
                    sheathColor = sheathColor,
                    copperColor = copperColor
                )
                ReferenceMethodCode.C -> drawMethodC(
                    w = w, h = h, cx = cx, cy = cy,
                    wallColor = wallColor,
                    wallStroke = wallStroke,
                    sheathColor = sheathColor,
                    copperColor = copperColor
                )
                ReferenceMethodCode.D1 -> drawMethodD(
                    inDuct = true,
                    w = w, h = h, cx = cx, cy = cy,
                    soilColor = soilColor,
                    wallStroke = wallStroke,
                    conduitColor = conduitColor,
                    sheathColor = sheathColor,
                    copperColor = copperColor
                )
                ReferenceMethodCode.D2 -> drawMethodD(
                    inDuct = false,
                    w = w, h = h, cx = cx, cy = cy,
                    soilColor = soilColor,
                    wallStroke = wallStroke,
                    conduitColor = conduitColor,
                    sheathColor = sheathColor,
                    copperColor = copperColor
                )
                ReferenceMethodCode.E -> drawMethodE(
                    w = w, h = h, cx = cx, cy = cy,
                    wallColor = wallColor,
                    wallStroke = wallStroke,
                    sheathColor = sheathColor,
                    copperColor = copperColor
                )
                ReferenceMethodCode.F -> drawMethodF(
                    w = w, h = h, cx = cx, cy = cy,
                    wallColor = wallColor,
                    wallStroke = wallStroke,
                    sheathColor = sheathColor,
                    copperColor = copperColor
                )
                ReferenceMethodCode.G -> drawMethodG(
                    w = w, h = h, cx = cx, cy = cy,
                    wallStroke = wallStroke,
                    sheathColor = sheathColor,
                    copperColor = copperColor
                )
            }
        }
    }
}

private fun DrawScope.drawMethodA(
    isMulticore: Boolean,
    w: Float, h: Float, cx: Float, cy: Float,
    insulationFill: Color, wallStroke: Color, conduitColor: Color,
    sheathColor: Color, copperColor: Color
) {
    // Thermally insulated wall cross-section
    val wallLeft = w * 0.14f
    val wallRight = w * 0.86f
    val wallTop = h * 0.12f
    val wallBot = h * 0.88f
    drawRect(insulationFill, Offset(wallLeft, wallTop), Size(wallRight - wallLeft, wallBot - wallTop))
    drawRect(wallStroke, Offset(wallLeft, wallTop), Size(wallRight - wallLeft, wallBot - wallTop), style = Stroke(2.2f))

    // Insulation wave hatching on left and right
    val hatchColor = wallStroke.copy(alpha = 0.45f)
    for (i in 0..3) {
        val y = wallTop + (wallBot - wallTop) * (0.2f + i * 0.2f)
        drawLine(hatchColor, Offset(wallLeft + 4f, y), Offset(cx - minOf(w, h) * 0.25f, y - 6f), 1.5f)
        drawLine(hatchColor, Offset(cx + minOf(w, h) * 0.25f, y - 6f), Offset(wallRight - 4f, y), 1.5f)
    }

    // Conduit inside insulated wall
    val condR = minOf(w, h) * 0.22f
    drawCircle(Color.White.copy(alpha = 0.9f), condR, Offset(cx, cy))
    drawCircle(conduitColor, condR, Offset(cx, cy), style = Stroke(3.2f))

    if (isMulticore) {
        val cableR = condR * 0.68f
        drawMulticoreCrossSection(Offset(cx, cy), cableR, sheathColor, copperColor)
    } else {
        val coreR = condR * 0.28f
        drawSingleCore(Offset(cx - condR * 0.36f, cy + condR * 0.22f), coreR, sheathColor, copperColor)
        drawSingleCore(Offset(cx + condR * 0.36f, cy + condR * 0.22f), coreR, sheathColor, copperColor)
        drawSingleCore(Offset(cx, cy - condR * 0.36f), coreR, sheathColor, copperColor)
    }
}

private fun DrawScope.drawMethodB(
    isMulticore: Boolean,
    w: Float, h: Float, cx: Float, cy: Float,
    wallColor: Color, wallStroke: Color, conduitColor: Color,
    sheathColor: Color, copperColor: Color
) {
    // Wall on bottom
    val wallTop = h * 0.72f
    drawRect(wallColor, Offset(w * 0.08f, wallTop), Size(w * 0.84f, h * 0.18f))
    drawLine(wallStroke, Offset(w * 0.08f, wallTop), Offset(w * 0.92f, wallTop), 2.5f)

    // Surface trunking / conduit resting on wall
    val boxSize = minOf(w, h) * 0.46f
    val left = cx - boxSize * 0.5f
    val top = wallTop - boxSize
    drawRoundRect(
        color = Color.White.copy(alpha = 0.9f),
        topLeft = Offset(left, top),
        size = Size(boxSize, boxSize),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = conduitColor,
        topLeft = Offset(left, top),
        size = Size(boxSize, boxSize),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(3.2f)
    )

    val boxCenter = Offset(cx, top + boxSize * 0.5f)
    if (isMulticore) {
        drawMulticoreCrossSection(boxCenter, boxSize * 0.32f, sheathColor, copperColor)
    } else {
        val r = boxSize * 0.14f
        drawSingleCore(Offset(boxCenter.x - r * 1.3f, boxCenter.y + r * 0.8f), r, sheathColor, copperColor)
        drawSingleCore(Offset(boxCenter.x + r * 1.3f, boxCenter.y + r * 0.8f), r, sheathColor, copperColor)
        drawSingleCore(Offset(boxCenter.x, boxCenter.y - r * 1.1f), r, sheathColor, copperColor)
    }
}

private fun DrawScope.drawMethodC(
    w: Float, h: Float, cx: Float, cy: Float,
    wallColor: Color, wallStroke: Color,
    sheathColor: Color, copperColor: Color
) {
    val wallTop = h * 0.68f
    drawRect(wallColor, Offset(w * 0.08f, wallTop), Size(w * 0.84f, h * 0.22f))
    drawLine(wallStroke, Offset(w * 0.08f, wallTop), Offset(w * 0.92f, wallTop), 2.5f)

    val cableR = minOf(w, h) * 0.17f
    val center = Offset(cx, wallTop - cableR)
    drawMulticoreCrossSection(center, cableR, sheathColor, copperColor)

    // Saddle clip hugging cable onto wall
    val clipPath = Path().apply {
        moveTo(cx - cableR * 1.65f, wallTop)
        lineTo(cx - cableR * 1.15f, wallTop)
        lineTo(cx - cableR * 1.15f, center.y)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                center.x - cableR * 1.15f,
                center.y - cableR * 1.15f,
                center.x + cableR * 1.15f,
                center.y + cableR * 1.15f
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        lineTo(cx + cableR * 1.15f, wallTop)
        lineTo(cx + cableR * 1.65f, wallTop)
    }
    drawPath(clipPath, wallStroke, style = Stroke(3.0f))
}

private fun DrawScope.drawMethodD(
    inDuct: Boolean,
    w: Float, h: Float, cx: Float, cy: Float,
    soilColor: Color, wallStroke: Color, conduitColor: Color,
    sheathColor: Color, copperColor: Color
) {
    // Ground line and soil fill
    val groundY = h * 0.28f
    drawRect(soilColor, Offset(w * 0.06f, groundY), Size(w * 0.88f, h * 0.64f))
    drawLine(wallStroke, Offset(w * 0.06f, groundY), Offset(w * 0.94f, groundY), 2.5f)

    // Grass/surface ticks above ground line
    for (i in 0..5) {
        val x = w * (0.14f + i * 0.14f)
        drawLine(wallStroke.copy(alpha = 0.6f), Offset(x, groundY), Offset(x + 4f, groundY - 7f), 1.8f)
    }

    val center = Offset(cx, h * 0.64f)
    val r = minOf(w, h) * 0.20f

    if (inDuct) {
        // Underground duct pipe
        drawCircle(Color.White.copy(alpha = 0.85f), r, center)
        drawCircle(conduitColor, r, center, style = Stroke(3.5f))
        drawMulticoreCrossSection(center, r * 0.65f, sheathColor, copperColor)
    } else {
        // Cover tile above direct-buried cable
        drawRect(
            color = wallStroke.copy(alpha = 0.55f),
            topLeft = Offset(cx - r * 1.4f, center.y - r * 1.35f),
            size = Size(r * 2.8f, 6f)
        )
        drawMulticoreCrossSection(center, r * 0.78f, sheathColor, copperColor)
    }
}

private fun DrawScope.drawMethodE(
    w: Float, h: Float, cx: Float, cy: Float,
    wallColor: Color, wallStroke: Color,
    sheathColor: Color, copperColor: Color
) {
    // Perforated tray suspended in free air (with wall clearance)
    val trayY = h * 0.66f
    val trayLeft = w * 0.16f
    val trayRight = w * 0.84f
    val lipH = h * 0.10f

    // Tray channel
    val trayPath = Path().apply {
        moveTo(trayLeft, trayY - lipH)
        lineTo(trayLeft, trayY)
        lineTo(trayRight, trayY)
        lineTo(trayRight, trayY - lipH)
    }
    drawPath(
        trayPath,
        wallStroke,
        style = Stroke(
            width = 3.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 5f), 0f)
        )
    )
    // Bracket support
    drawLine(wallStroke.copy(alpha = 0.5f), Offset(cx, trayY), Offset(cx, h * 0.86f), 2.5f)
    drawLine(wallStroke.copy(alpha = 0.5f), Offset(cx - 16f, h * 0.86f), Offset(cx + 16f, h * 0.86f), 2.5f)

    // Multicore cable resting on perforated tray in free air
    val cableR = minOf(w, h) * 0.18f
    drawMulticoreCrossSection(Offset(cx, trayY - cableR - 2f), cableR, sheathColor, copperColor)
}

private fun DrawScope.drawMethodF(
    w: Float, h: Float, cx: Float, cy: Float,
    wallColor: Color, wallStroke: Color,
    sheathColor: Color, copperColor: Color
) {
    // Perforated tray with 3 single-core cables touching in trefoil
    val trayY = h * 0.72f
    val trayLeft = w * 0.16f
    val trayRight = w * 0.84f
    val lipH = h * 0.09f
    val trayPath = Path().apply {
        moveTo(trayLeft, trayY - lipH)
        lineTo(trayLeft, trayY)
        lineTo(trayRight, trayY)
        lineTo(trayRight, trayY - lipH)
    }
    drawPath(
        trayPath,
        wallStroke,
        style = Stroke(3.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 5f), 0f))
    )

    val r = minOf(w, h) * 0.11f
    val bottomY = trayY - r - 2f
    val c1 = Offset(cx - r, bottomY)
    val c2 = Offset(cx + r, bottomY)
    val c3 = Offset(cx, bottomY - r * 1.732f)
    drawSingleCore(c1, r, sheathColor, copperColor)
    drawSingleCore(c2, r, sheathColor, copperColor)
    drawSingleCore(c3, r, sheathColor, copperColor)
}

private fun DrawScope.drawMethodG(
    w: Float, h: Float, cx: Float, cy: Float,
    wallStroke: Color, sheathColor: Color, copperColor: Color
) {
    // Horizontal support rail with 3 spaced single-core cables (>= 1 De gap)
    val railY = h * 0.65f
    drawLine(wallStroke, Offset(w * 0.10f, railY), Offset(w * 0.90f, railY), 3.2f)

    val r = minOf(w, h) * 0.095f
    val spacing = r * 4.2f // clear gap > 2r = De
    val cableY = railY - r - 2f
    drawSingleCore(Offset(cx - spacing, cableY), r, sheathColor, copperColor)
    drawSingleCore(Offset(cx, cableY), r, sheathColor, copperColor)
    drawSingleCore(Offset(cx + spacing, cableY), r, sheathColor, copperColor)

    // Dimension arrow indicating >= 1 De gap
    val arrowY = cableY - r * 1.7f
    drawLine(copperColor, Offset(cx - spacing + r, arrowY), Offset(cx - r, arrowY), 1.8f)
    drawLine(copperColor, Offset(cx + r, arrowY), Offset(cx + spacing - r, arrowY), 1.8f)
}

private fun DrawScope.drawSingleCore(
    center: Offset,
    radius: Float,
    sheathColor: Color,
    copperColor: Color
) {
    drawCircle(sheathColor, radius, center)
    drawCircle(Color.White, radius * 0.72f, center)
    drawCircle(copperColor, radius * 0.50f, center)
}

private fun DrawScope.drawMulticoreCrossSection(
    center: Offset,
    outerRadius: Float,
    sheathColor: Color,
    copperColor: Color
) {
    drawCircle(sheathColor, outerRadius, center)
    drawCircle(Color.White.copy(alpha = 0.92f), outerRadius * 0.80f, center)
    val coreR = outerRadius * 0.27f
    val offset = outerRadius * 0.39f
    drawCircle(copperColor, coreR, Offset(center.x - offset * 0.85f, center.y - offset * 0.5f))
    drawCircle(copperColor, coreR, Offset(center.x + offset * 0.85f, center.y - offset * 0.5f))
    drawCircle(copperColor, coreR, Offset(center.x - offset * 0.85f, center.y + offset * 0.6f))
    drawCircle(copperColor, coreR, Offset(center.x + offset * 0.85f, center.y + offset * 0.6f))
}
