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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Cyberpunk-inspired "Zlc" brand mark:
 * - Dominant geometric Electric Cyan 'Z' with a Neon Purple chromatic-aberration ghost layer.
 * - Smaller steady 'lc' subscript anchored in the bottom-right corner.
 * - The 'Z' quivers slightly in periodic glitch bursts, whenever a link is cleaned ([triggerKey]),
 *   and immediately when tapped by the user.
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
    var ghostAlpha by remember { mutableFloatStateOf(0.65f) }

    suspend fun runQuiverBurst() {
        val frames = listOf(
            Triple(-1.8f, 0.9f, 0.95f),
            Triple(1.9f, -1.0f, 0.90f),
            Triple(-1.3f, -0.6f, 0.85f),
            Triple(1.1f, 0.7f, 0.80f),
            Triple(-0.6f, 0.3f, 0.75f),
            Triple(0f, 0f, 0.65f)
        )
        for ((dx, dy, alpha) in frames) {
            zOffsetX = dx
            zOffsetY = dy
            ghostAlpha = alpha
            delay(32L)
        }
    }

    // Quiver immediately on tap or when a link is cleaned / expanded
    LaunchedEffect(triggerKey, manualTapCount) {
        runQuiverBurst()
    }

    // Periodic subtle cyberpunk quiver burst every ~3.8 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3800L)
            runQuiverBurst()
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
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.7f),
                        Color(0xFFA855F7).copy(alpha = 0.7f)
                    )
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
            // Normalize coordinates against a 108x108 virtual grid ( zoomed into the [16..92] glyph box )
            fun sx(v: Float): Float = ((v - 16f) / 76f) * w
            fun sy(v: Float): Float = ((v - 16f) / 76f) * h

            val jx = (zOffsetX / 34f) * w
            val jy = (zOffsetY / 34f) * h

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

            // 1. Chromatic Aberration / Quiver Ghost 'Z' (Neon Purple)
            val ghostDx = -jx * 1.4f - (w * 0.035f)
            val ghostDy = -jy * 1.4f + (h * 0.022f)
            drawPath(
                path = buildZPath(ghostDx, ghostDy),
                color = Color(0xFFA855F7).copy(alpha = ghostAlpha)
            )

            // 2. Primary Quivering 'Z' (Electric Cyan)
            drawPath(
                path = buildZPath(jx, jy),
                color = Color(0xFF38BDF8)
            )

            // 3. Top Edge Laser Highlight on 'Z'
            drawRect(
                color = Color(0xFFE0F2FE),
                topLeft = Offset(sx(24f) + jx, sy(28f) + jy),
                size = Size(sx(63f) - sx(24f), sy(31f) - sy(28f))
            )

            // 4. Smaller Steady 'l' (Bottom-Right Subscript)
            drawRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(sx(67.5f), sy(46f)),
                size = Size(sx(73f) - sx(67.5f), sy(74f) - sy(46f))
            )

            // 5. Smaller Steady 'c' (Bottom-Right Subscript)
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
