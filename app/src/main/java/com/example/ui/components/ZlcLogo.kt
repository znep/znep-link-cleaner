package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

private enum class ZGlitchMode {
    SEISMIC_JIGGLE,
    SCANLINE_SLICE,
    CYBER_FLIP,
    VOLTAGE_SURGE,
    QUANTUM_BLINK
}

/**
 * Cyberpunk-inspired "Zlc" brand mark with a Randomized 5-Mode Stunt & Glitch Engine:
 * - Dominant geometric Electric Cyan 'Z' with Neon Purple + Hot Magenta chromatic-aberration ghost layers.
 * - Smaller steady 'lc' subscript anchored in the bottom-right corner.
 * - The 'Z' randomly performs seismic jiggles, horizontal CRT scanline slice tears, kinetic snap-flips,
 *   high-voltage neon spark surges, and quantum double-ghost blinks at random intervals, whenever a link
 *   is cleaned ([triggerKey]), and immediately when tapped by the user.
 */
@Composable
fun ZlcLogo(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    triggerKey: Any? = null
) {
    var manualTapCount by remember { mutableIntStateOf(0) }
    var zOffsetX by remember { mutableFloatStateOf(0f) }
    var zOffsetY by remember { mutableFloatStateOf(0f) }
    var ghostAlpha by remember { mutableFloatStateOf(0.68f) }
    var magentaGhostAlpha by remember { mutableFloatStateOf(0f) }
    var sliceShearX by remember { mutableFloatStateOf(0f) }
    var scanlineY by remember { mutableFloatStateOf(-1f) }
    var zScaleX by remember { mutableFloatStateOf(1f) }
    var zRotation by remember { mutableFloatStateOf(0f) }
    var voltageFlash by remember { mutableFloatStateOf(0f) }
    var sparkRadius by remember { mutableFloatStateOf(0f) }

    fun resetState() {
        zOffsetX = 0f
        zOffsetY = 0f
        ghostAlpha = 0.68f
        magentaGhostAlpha = 0f
        sliceShearX = 0f
        scanlineY = -1f
        zScaleX = 1f
        zRotation = 0f
        voltageFlash = 0f
        sparkRadius = 0f
    }

    suspend fun runStunt(mode: ZGlitchMode) {
        when (mode) {
            ZGlitchMode.SEISMIC_JIGGLE -> {
                val frames = listOf(
                    Triple(-3.6f, 1.9f, 0.98f),
                    Triple(3.8f, -2.1f, 0.96f),
                    Triple(-3.1f, -1.5f, 0.94f),
                    Triple(2.9f, 1.8f, 0.92f),
                    Triple(-2.4f, 1.2f, 0.88f),
                    Triple(2.0f, -1.1f, 0.84f),
                    Triple(-1.3f, 0.7f, 0.78f),
                    Triple(0.8f, -0.4f, 0.72f)
                )
                for ((dx, dy, alpha) in frames) {
                    zOffsetX = dx
                    zOffsetY = dy
                    ghostAlpha = alpha
                    magentaGhostAlpha = (alpha - 0.15f).coerceAtLeast(0f)
                    delay(28L)
                }
            }

            ZGlitchMode.SCANLINE_SLICE -> {
                val steps = listOf(
                    Triple(5.0f, 0.20f, -1.8f),
                    Triple(-5.4f, 0.38f, 2.1f),
                    Triple(4.6f, 0.56f, -1.5f),
                    Triple(-3.8f, 0.74f, 1.4f),
                    Triple(2.5f, 0.88f, -0.9f),
                    Triple(-1.2f, 0.96f, 0.4f)
                )
                for ((shear, scanY, dx) in steps) {
                    sliceShearX = shear
                    scanlineY = scanY
                    zOffsetX = dx
                    ghostAlpha = 0.95f
                    magentaGhostAlpha = 0.85f
                    delay(34L)
                }
            }

            ZGlitchMode.CYBER_FLIP -> {
                val frames = listOf(
                    Triple(0.35f, -14f, 1.8f),
                    Triple(-0.85f, -8f, -2.4f),
                    Triple(-1.0f, 0f, 2.2f),
                    Triple(-0.45f, 12f, -1.6f),
                    Triple(0.55f, 7f, 1.2f),
                    Triple(1.08f, -3f, -0.6f),
                    Triple(1.0f, 0f, 0f)
                )
                for ((scaleX, rot, dx) in frames) {
                    zScaleX = scaleX
                    zRotation = rot
                    zOffsetX = dx
                    ghostAlpha = 0.95f
                    magentaGhostAlpha = 0.78f
                    delay(32L)
                }
            }

            ZGlitchMode.VOLTAGE_SURGE -> {
                val frames = listOf(
                    Triple(1.0f, 0.35f, -2.5f),
                    Triple(0.25f, 0.65f, 2.8f),
                    Triple(1.0f, 0.95f, -2.2f),
                    Triple(0.4f, 1.20f, 1.9f),
                    Triple(0.9f, 0.80f, -1.2f),
                    Triple(0.2f, 0.40f, 0.6f)
                )
                for ((flash, spark, dx) in frames) {
                    voltageFlash = flash
                    sparkRadius = spark
                    zOffsetX = dx
                    zOffsetY = -dx * 0.45f
                    ghostAlpha = 0.98f
                    magentaGhostAlpha = 0.90f
                    delay(34L)
                }
            }

            ZGlitchMode.QUANTUM_BLINK -> {
                val frames = listOf(
                    Triple(-4.8f, -2.2f, 1.0f),
                    Triple(4.8f, 2.2f, 1.0f),
                    Triple(-3.4f, 2.0f, 0.92f),
                    Triple(3.4f, -2.0f, 0.92f),
                    Triple(-1.6f, 0.8f, 0.80f)
                )
                for ((dx, dy, alpha) in frames) {
                    zOffsetX = dx
                    zOffsetY = dy
                    ghostAlpha = alpha
                    magentaGhostAlpha = alpha
                    voltageFlash = 0.55f
                    delay(30L)
                }
            }
        }
        resetState()
    }

    // Trigger an energetic stunt immediately on tap or when a link is cleaned / expanded
    LaunchedEffect(triggerKey, manualTapCount) {
        val modes = ZGlitchMode.entries
        val chosen = if (manualTapCount > 0) {
            modes[manualTapCount % modes.size]
        } else {
            ZGlitchMode.SEISMIC_JIGGLE
        }
        runStunt(chosen)
        // Follow up with a quick secondary micro-jiggle 45% of the time
        if (Random.nextFloat() < 0.45f) {
            delay(90L)
            runStunt(modes.random())
        }
    }

    // Randomized background cyberpunk stunts every 1.3s to 3.1s
    LaunchedEffect(Unit) {
        val modes = ZGlitchMode.entries
        while (true) {
            delay(Random.nextLong(1300L, 3100L))
            runStunt(modes.random())
            // Occasional double-combo glitch burst
            if (Random.nextFloat() < 0.35f) {
                delay(110L)
                runStunt(modes.random())
            }
        }
    }

    val cornerRadius = size * 0.24f
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF060913),
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B)
                    )
                )
            )
            .border(
                width = if (voltageFlash > 0.4f) 1.6.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (voltageFlash > 0.4f) {
                        listOf(Color(0xFFF8FAFC), Color(0xFF38BDF8), Color(0xFFEC4899))
                    } else {
                        listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.75f),
                            Color(0xFFA855F7).copy(alpha = 0.75f)
                        )
                    }
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { manualTapCount++ }
            )
            .semantics {
                contentDescription = "ZnepLinkCleaner Zlc Logo"
                role = Role.Image
            }
            .testTag("zlc_quiver_logo"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            fun sx(v: Float): Float = ((v - 16f) / 76f) * w
            fun sy(v: Float): Float = ((v - 16f) / 76f) * h

            val jx = (zOffsetX / 34f) * w
            val jy = (zOffsetY / 34f) * h
            val shearPx = (sliceShearX / 34f) * w

            fun buildZPath(dx: Float, dy: Float): Path = Path().apply {
                moveTo(sx(24f) + dx, sy(28f) + dy)
                lineTo(sx(63f) + dx, sy(28f) + dy)
                lineTo(sx(63f) + dx, sy(36.5f) + dy)
                lineTo(sx(37.5f) + dx, sy(65.5f) + dy)
                lineTo(sx(63f) + dx, sy(65.5f) + dy)
                lineTo(sx(63f) + dx, sy(74f) + dy)
                lineTo(sx(24f) + dx, sy(74f) + dy)
                lineTo(sx(24f) + dx, sy(65.5f) + dy)
                lineTo(sx(49.5f) + dx, sy(36.5f) + dy)
                lineTo(sx(24f) + dx, sy(36.5f) + dy)
                close()
            }

            val zCenter = Offset(sx(43.5f), sy(51f))

            withTransform({
                if (zRotation != 0f) {
                    rotate(degrees = zRotation, pivot = zCenter)
                }
                if (zScaleX != 1f) {
                    scale(scaleX = zScaleX, scaleY = 1f, pivot = zCenter)
                }
            }) {
                // 1a. Secondary Hot Magenta Chromatic Ghost (during intense stunts)
                if (magentaGhostAlpha > 0.05f) {
                    val magDx = jx * 1.55f + (w * 0.04f)
                    val magDy = -jy * 1.35f - (h * 0.025f)
                    drawPath(
                        path = buildZPath(magDx, magDy),
                        color = Color(0xFFEC4899).copy(alpha = magentaGhostAlpha)
                    )
                }

                // 1b. Primary Neon Purple Chromatic Aberration Ghost 'Z'
                val ghostDx = -jx * 1.45f - (w * 0.035f)
                val ghostDy = -jy * 1.45f + (h * 0.022f)
                drawPath(
                    path = buildZPath(ghostDx, ghostDy),
                    color = Color(0xFFA855F7).copy(alpha = ghostAlpha)
                )

                val mainZColor = if (voltageFlash > 0.5f) {
                    Color(0xFFF8FAFC)
                } else {
                    Color(0xFF38BDF8)
                }

                // 2. Primary Quivering 'Z' (with horizontal slice-tear support)
                if (shearPx != 0f) {
                    val topSplit = sy(41f)
                    val bottomSplit = sy(61f)
                    // Upper slice
                    clipRect(left = 0f, top = 0f, right = w, bottom = topSplit) {
                        drawPath(path = buildZPath(jx - shearPx * 0.5f, jy), color = mainZColor)
                    }
                    // Middle diagonal slice (sheared horizontally)
                    clipRect(left = 0f, top = topSplit, right = w, bottom = bottomSplit) {
                        drawPath(path = buildZPath(jx + shearPx, jy), color = Color(0xFF7DD3FC))
                    }
                    // Lower slice
                    clipRect(left = 0f, top = bottomSplit, right = w, bottom = h) {
                        drawPath(path = buildZPath(jx - shearPx * 0.35f, jy), color = mainZColor)
                    }
                } else {
                    drawPath(
                        path = buildZPath(jx, jy),
                        color = mainZColor
                    )
                }

                // 3. Top Edge Laser Highlight on 'Z'
                drawRect(
                    color = Color(0xFFE0F2FE),
                    topLeft = Offset(sx(24f) + jx, sy(28f) + jy),
                    size = Size(sx(63f) - sx(24f), sy(31f) - sy(28f))
                )
            }

            // 4. Sweeping CRT Laser Scanline (during SCANLINE_SLICE stunt)
            if (scanlineY in 0f..1f) {
                val lineY = h * scanlineY
                drawRect(
                    color = Color(0xFF00F0FF).copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.08f, lineY),
                    size = Size(w * 0.84f, (h * 0.045f).coerceAtLeast(1.5f))
                )
            }

            // 5. High-Voltage Corner Sparks (during VOLTAGE_SURGE stunt)
            if (sparkRadius > 0.05f) {
                val sparkDist = w * 0.09f * sparkRadius
                val sparkSize = (w * 0.055f).coerceAtLeast(2f)
                val corners = listOf(
                    Offset(sx(24f) - sparkDist, sy(28f) - sparkDist),
                    Offset(sx(63f) + sparkDist, sy(28f) - sparkDist),
                    Offset(sx(24f) - sparkDist, sy(74f) + sparkDist),
                    Offset(sx(63f) + sparkDist, sy(74f) + sparkDist)
                )
                for ((idx, pt) in corners.withIndex()) {
                    drawCircle(
                        color = if (idx % 2 == 0) Color(0xFF00F0FF) else Color(0xFFEC4899),
                        radius = sparkSize * 0.5f,
                        center = pt
                    )
                }
            }

            // 6. Smaller Steady 'l' (Bottom-Right Subscript)
            drawRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(sx(67.5f), sy(46f)),
                size = Size(sx(73f) - sx(67.5f), sy(74f) - sy(46f))
            )

            // 7. Smaller Steady 'c' (Bottom-Right Subscript)
            val cPath = Path().apply {
                moveTo(sx(76f), sy(53.5f))
                lineTo(sx(88f), sy(53.5f))
                lineTo(sx(88f), sy(58.5f))
                lineTo(sx(81f), sy(58.5f))
                lineTo(sx(81f), sy(69f))
                lineTo(sx(88f), sy(69f))
                lineTo(sx(88f), sy(74f))
                lineTo(sx(76f), sy(74f))
                close()
            }
            drawPath(
                path = cPath,
                color = Color(0xFF7DD3FC)
            )
        }
    }
}
