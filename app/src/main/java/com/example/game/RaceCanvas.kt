package com.example.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.CarBodyStyle
import com.example.model.CarModel
import com.example.model.CityTrack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class ProjectedSeg(
    val segIndex: Int,
    val seg: RoadSegment,
    val x1: Float,
    val y1: Float,
    val w1: Float,
    val scale1: Float,
    val x2: Float,
    val y2: Float,
    val w2: Float,
    val scale2: Float,
    val segZ: Float
)

@Composable
fun RaceCanvas(
    simulation: RacingSimulation,
    snapshot: RaceSnapshot,
    carColor: Color,
    carModel: CarModel,
    track: CityTrack,
    frameTick: Long,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Consume frameTick to trigger deterministic redraw every frame
        @Suppress("UNUSED_EXPRESSION")
        frameTick

        val w = size.width
        val h = size.height
        val shakeOffsetX = if (snapshot.crashShake > 0f) {
            sin(frameTick * 1.9f) * snapshot.crashShake * 18f
        } else 0f
        val shakeOffsetY = if (snapshot.crashShake > 0f) {
            cos(frameTick * 2.3f) * snapshot.crashShake * 12f
        } else 0f

        val horizonY = h * 0.42f + shakeOffsetY
        val centerX = w * 0.5f + shakeOffsetX

        // 1. Sky & City Dusk Gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(track.skyTopColor), Color(track.skyBottomColor)),
                startY = 0f,
                endY = horizonY + 24f
            ),
            size = Size(w, horizonY + 24f)
        )

        // Distant metropolis skyline silhouette with glowing windows
        drawDistantCitySkyline(
            width = w,
            horizonY = horizonY,
            accentColor = Color(track.buildingAccentColor),
            parallaxOffset = snapshot.playerX * 42f
        )

        // Ground asphalt/horizon base
        drawRect(
            color = Color(0xFF0A0F1A),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h - horizonY)
        )

        // 2. Project 3D road segments ahead
        val baseIndex = simulation.segmentIndexAt(snapshot.playerZ)
        val baseSeg = simulation.segments[baseIndex]
        val segPercent = ((snapshot.playerZ % RacingSimulation.SEGMENT_LENGTH) / RacingSimulation.SEGMENT_LENGTH)
        val camHeight = 2.75f + baseSeg.elevation
        val camDepth = if (snapshot.isNitroActive) 0.74f else 0.84f

        val drawDistance = 90
        var curveDx = -(baseSeg.curve * segPercent)
        var curveX = 0f
        var maxY = h

        val visibleSegs = ArrayList<ProjectedSeg>(drawDistance)
        val segCount = simulation.segments.size

        for (n in 1..drawDistance) {
            val idx = (baseIndex + n) % segCount
            val seg = simulation.segments[idx]

            val z1 = (n - segPercent) * RacingSimulation.SEGMENT_LENGTH
            val z2 = (n + 1 - segPercent) * RacingSimulation.SEGMENT_LENGTH
            if (z1 <= 0.5f) continue

            val x1World = curveX - snapshot.playerX * 2.3f
            curveX += curveDx
            curveDx += seg.curve * 0.35f
            val x2World = curveX - snapshot.playerX * 2.3f

            val y1World = seg.elevation
            val nextSeg = simulation.segments[(idx + 1) % segCount]
            val y2World = nextSeg.elevation

            val scale1 = (camDepth / (z1 * 0.08f)).coerceAtMost(6f)
            val scale2 = (camDepth / (z2 * 0.08f)).coerceAtMost(6f)

            val sy1 = horizonY + (camHeight - y1World) * scale1 * (h * 0.14f)
            val sy2 = horizonY + (camHeight - y2World) * scale2 * (h * 0.14f)

            if (sy2 >= sy1 || sy2 >= maxY) {
                continue
            }

            val sx1 = centerX + x1World * scale1 * (w * 0.23f)
            val sx2 = centerX + x2World * scale2 * (w * 0.23f)
            val sw1 = 2.5f * scale1 * (w * 0.23f)
            val sw2 = 2.5f * scale2 * (w * 0.23f)

            visibleSegs.add(
                ProjectedSeg(
                    segIndex = idx,
                    seg = seg,
                    x1 = sx1, y1 = sy1.coerceAtMost(maxY), w1 = sw1, scale1 = scale1,
                    x2 = sx2, y2 = sy2, w2 = sw2, scale2 = scale2,
                    segZ = idx * RacingSimulation.SEGMENT_LENGTH
                )
            )
            maxY = sy2
        }

        // 3. Draw far-to-near (Painter's Algorithm)
        val accentColor = Color(track.buildingAccentColor)
        val roadBaseColor = Color(track.roadTintColor)

        for (i in visibleSegs.indices.reversed()) {
            val ps = visibleSegs[i]
            val alt = (ps.segIndex / 2) % 2 == 0

            // Sidewalk / Shoulder ground strip
            drawQuad(
                x1 = 0f, y1 = ps.y1, w1 = w * 2f,
                x2 = 0f, y2 = ps.y2, w2 = w * 2f,
                color = if (alt) Color(0xFF0E1523) else Color(0xFF121B2D)
            )

            // Raised Red/White or Amber/Slate Curbs
            val curbColor = if (alt) Color(0xFFE11D48) else Color(0xFFE2E8F0)
            drawQuad(ps.x1, ps.y1, ps.w1 * 1.07f, ps.x2, ps.y2, ps.w2 * 1.07f, curbColor)

            // Main Asphalt Surface
            val asphalt = if (alt) roadBaseColor else roadBaseColor.copy(
                red = (roadBaseColor.red * 0.9f),
                green = (roadBaseColor.green * 0.9f),
                blue = (roadBaseColor.blue * 0.9f)
            )
            drawQuad(ps.x1, ps.y1, ps.w1, ps.x2, ps.y2, ps.w2, asphalt)

            // 4-Lane Highway Markings
            if (alt) {
                // Left and right lane dividers
                drawQuad(
                    ps.x1 - ps.w1 * 0.50f, ps.y1, ps.w1 * 0.018f,
                    ps.x2 - ps.w2 * 0.50f, ps.y2, ps.w2 * 0.018f,
                    Color(0xFFE2E8F0).copy(alpha = 0.68f)
                )
                drawQuad(
                    ps.x1 + ps.w1 * 0.50f, ps.y1, ps.w1 * 0.018f,
                    ps.x2 + ps.w2 * 0.50f, ps.y2, ps.w2 * 0.018f,
                    Color(0xFFE2E8F0).copy(alpha = 0.68f)
                )
            }
            // Double amber center divider
            drawQuad(
                ps.x1 - ps.w1 * 0.018f, ps.y1, ps.w1 * 0.012f,
                ps.x2 - ps.w2 * 0.018f, ps.y2, ps.w2 * 0.012f,
                Color(0xFFF59E0B).copy(alpha = 0.85f)
            )
            drawQuad(
                ps.x1 + ps.w1 * 0.018f, ps.y1, ps.w1 * 0.012f,
                ps.x2 + ps.w2 * 0.018f, ps.y2, ps.w2 * 0.012f,
                Color(0xFFF59E0B).copy(alpha = 0.85f)
            )

            // Start / Finish Checkered Line
            if (ps.seg.isFinishLine) {
                val squares = 10
                val sqW1 = (ps.w1 * 2f) / squares
                val sqW2 = (ps.w2 * 2f) / squares
                for (sq in 0 until squares) {
                    val isWhite = (sq + ps.segIndex) % 2 == 0
                    val sx1 = (ps.x1 - ps.w1) + sqW1 * (sq + 0.5f)
                    val sx2 = (ps.x2 - ps.w2) + sqW2 * (sq + 0.5f)
                    drawQuad(
                        sx1, ps.y1, sqW1 * 0.5f,
                        sx2, ps.y2, sqW2 * 0.5f,
                        if (isWhite) Color.White else Color(0xFF0F172A)
                    )
                }
            }

            // 3D City Skyscrapers along Left & Right sidewalks
            if (ps.seg.buildingLeftHeight > 0f && ps.w1 > 12f) {
                val bW = ps.w1 * 0.65f
                val bH = ps.seg.buildingLeftHeight * ps.scale1 * (h * 0.013f)
                val bLeft = ps.x1 - ps.w1 * 1.15f - bW
                drawCityBuilding(
                    left = bLeft,
                    bottomY = ps.y1,
                    width = bW,
                    height = bH,
                    baseColor = ps.seg.buildingLeftColor,
                    windowColor = accentColor
                )
            }
            if (ps.seg.buildingRightHeight > 0f && ps.w1 > 12f) {
                val bW = ps.w1 * 0.65f
                val bH = ps.seg.buildingRightHeight * ps.scale1 * (h * 0.013f)
                val bLeft = ps.x1 + ps.w1 * 1.15f
                drawCityBuilding(
                    left = bLeft,
                    bottomY = ps.y1,
                    width = bW,
                    height = bH,
                    baseColor = ps.seg.buildingRightColor,
                    windowColor = Color(0xFFFBBF24)
                )
            }

            // Roadside Streetlamps
            if (ps.seg.hasStreetlight && ps.w1 > 16f) {
                val poleH = ps.w1 * 0.42f
                val lampR = (ps.w1 * 0.025f).coerceIn(2f, 9f)
                // Left lamp
                val lx = ps.x1 - ps.w1 * 1.08f
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(lx, ps.y1),
                    end = Offset(lx, ps.y1 - poleH),
                    strokeWidth = (ps.w1 * 0.015f).coerceIn(1.5f, 5f)
                )
                drawCircle(
                    color = Color(0xFFFDE68A),
                    radius = lampR,
                    center = Offset(lx + lampR * 1.2f, ps.y1 - poleH)
                )
                // Right lamp
                val rx = ps.x1 + ps.w1 * 1.08f
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(rx, ps.y1),
                    end = Offset(rx, ps.y1 - poleH),
                    strokeWidth = (ps.w1 * 0.015f).coerceIn(1.5f, 5f)
                )
                drawCircle(
                    color = Color(0xFFFDE68A),
                    radius = lampR,
                    center = Offset(rx - lampR * 1.2f, ps.y1 - poleH)
                )
            }

            // Overhead Highway Gantry Arch
            if (ps.seg.hasOverpassArch && ps.w1 > 22f) {
                val archH = ps.w1 * 0.56f
                val beamH = (ps.w1 * 0.065f).coerceIn(4f, 22f)
                val postW = (ps.w1 * 0.03f).coerceIn(2f, 10f)
                drawRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(ps.x1 - ps.w1 * 1.12f, ps.y1 - archH),
                    size = Size(postW, archH)
                )
                drawRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(ps.x1 + ps.w1 * 1.12f - postW, ps.y1 - archH),
                    size = Size(postW, archH)
                )
                drawRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(ps.x1 - ps.w1 * 1.14f, ps.y1 - archH),
                    size = Size(ps.w1 * 2.28f, beamH)
                )
                // Glowing emerald highway sign panel
                drawRect(
                    color = Color(0xFF065F46),
                    topLeft = Offset(ps.x1 - ps.w1 * 0.48f, ps.y1 - archH + beamH * 0.15f),
                    size = Size(ps.w1 * 0.96f, beamH * 0.72f)
                )
            }

            // Traffic vehicles belonging to this segment
            for (veh in simulation.traffic) {
                if (simulation.segmentIndexAt(veh.z) == ps.segIndex && ps.w1 > 10f) {
                    val vx = ps.x1 + veh.laneX * ps.w1
                    val vw = ps.w1 * veh.type.widthNorm
                    val vh = vw * 0.62f * veh.type.heightScale
                    drawTrafficVehicle(
                        centerX = vx,
                        bottomY = ps.y1,
                        width = vw,
                        height = vh,
                        type = veh.type,
                        bodyColor = veh.bodyColor
                    )
                }
            }

            // AI Rival Supercars belonging to this segment
            if (simulation.mode == com.example.model.RaceMode.CITY_SPRINT) {
                for (rival in simulation.rivals) {
                    if (simulation.segmentIndexAt(rival.z) == ps.segIndex && ps.w1 > 10f) {
                        val rx = ps.x1 + rival.laneX * ps.w1
                        val rw = ps.w1 * 0.27f
                        drawRealisticRearSupercar(
                            centerX = rx,
                            bottomY = ps.y1,
                            carWidth = rw,
                            bodyColor = rival.bodyColor,
                            hasWing = rival.hasWing,
                            isBraking = false,
                            isNitro = false,
                            leanAngle = 0f,
                            showDriverSilhouette = true,
                            frameTick = frameTick
                        )
                    }
                }
            }
        }

        // 4. Drift Smoke Particles behind Player Car
        val playerScreenX = centerX
        val playerScreenY = h * 0.87f + shakeOffsetY * 0.5f
        val playerCarWidth = (w * 0.29f).coerceIn(115f, 210f)

        for (p in simulation.smokeParticles) {
            drawCircle(
                color = Color(0xFFCBD5E1).copy(alpha = p.alpha.coerceIn(0f, 0.6f)),
                radius = p.size,
                center = Offset(
                    playerScreenX + (p.xNorm - snapshot.playerX) * playerCarWidth * 0.8f,
                    playerScreenY - 8f + p.yOffset
                )
            )
        }

        // 5. Player's Realistic Supercar + Visible Human Driver Cockpit
        drawRealisticRearSupercar(
            centerX = playerScreenX,
            bottomY = playerScreenY,
            carWidth = playerCarWidth,
            bodyColor = carColor,
            hasWing = carModel.hasGtWing || carModel.bodyStyle != CarBodyStyle.GT_COUPE,
            isBraking = snapshot.isBraking || snapshot.isDrifting,
            isNitro = snapshot.isNitroActive,
            leanAngle = snapshot.steerLean * 5.5f,
            showDriverSilhouette = true,
            frameTick = frameTick
        )

        // 6. Nitro Speed Lines along screen edges
        if (snapshot.isNitroActive || snapshot.speedKmh > 270) {
            val lineAlpha = if (snapshot.isNitroActive) 0.42f else 0.16f
            for (k in 0..11) {
                val angle = (k / 12f) * 2f * PI.toFloat() + (frameTick % 7) * 0.12f
                val rStart = w * 0.34f
                val rEnd = w * 0.65f
                drawLine(
                    color = if (snapshot.isNitroActive) Color(0xFF22D3EE).copy(alpha = lineAlpha)
                    else Color.White.copy(alpha = lineAlpha),
                    start = Offset(centerX + cos(angle) * rStart, h * 0.5f + sin(angle) * rStart),
                    end = Offset(centerX + cos(angle) * rEnd, h * 0.5f + sin(angle) * rEnd),
                    strokeWidth = 3f
                )
            }
        }
    }
}

private fun DrawScope.drawDistantCitySkyline(
    width: Float,
    horizonY: Float,
    accentColor: Color,
    parallaxOffset: Float
) {
    val towerCount = 16
    val towerW = width / towerCount
    for (i in 0 until towerCount) {
        val hFactor = ((sin(i * 1.7f) + 1.3f) * 0.5f)
        val bHeight = 28f + hFactor * 74f
        val x = (i * towerW - (parallaxOffset % towerW))
        drawRect(
            color = Color(0xFF0B1120),
            topLeft = Offset(x, horizonY - bHeight),
            size = Size(towerW + 2f, bHeight + 4f)
        )
        // Subtle lit roofline / spire accent
        if (i % 3 == 0) {
            drawRect(
                color = accentColor.copy(alpha = 0.45f),
                topLeft = Offset(x + 3f, horizonY - bHeight),
                size = Size(towerW - 6f, 2.5f)
            )
        }
    }
}

private fun DrawScope.drawQuad(
    x1: Float, y1: Float, w1: Float,
    x2: Float, y2: Float, w2: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(x1 - w1, y1)
        lineTo(x1 + w1, y1)
        lineTo(x2 + w2, y2)
        lineTo(x2 - w2, y2)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawCityBuilding(
    left: Float,
    bottomY: Float,
    width: Float,
    height: Float,
    baseColor: Color,
    windowColor: Color
) {
    val top = bottomY - height
    drawRect(
        color = baseColor,
        topLeft = Offset(left, top),
        size = Size(width, height)
    )
    if (width > 24f && height > 30f) {
        val cols = 3
        val rows = 5
        val winW = width * 0.16f
        val winH = height * 0.08f
        val gapX = (width - cols * winW) / (cols + 1)
        val gapY = (height - rows * winH) / (rows + 1)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if ((r + c) % 2 == 0) {
                    drawRect(
                        color = windowColor.copy(alpha = 0.35f),
                        topLeft = Offset(left + gapX + c * (winW + gapX), top + gapY + r * (winH + gapY)),
                        size = Size(winW, winH)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawTrafficVehicle(
    centerX: Float,
    bottomY: Float,
    width: Float,
    height: Float,
    type: TrafficType,
    bodyColor: Color
) {
    val left = centerX - width * 0.5f
    val top = bottomY - height

    // Shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.55f),
        topLeft = Offset(left - width * 0.05f, bottomY - height * 0.12f),
        size = Size(width * 1.1f, height * 0.22f)
    )

    // Tires
    val tireW = width * 0.16f
    val tireH = height * 0.20f
    drawRoundRect(
        color = Color(0xFF09090B),
        topLeft = Offset(left + width * 0.04f, bottomY - tireH),
        size = Size(tireW, tireH),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color(0xFF09090B),
        topLeft = Offset(left + width - tireW - width * 0.04f, bottomY - tireH),
        size = Size(tireW, tireH),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Body
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(left, top + height * 0.25f),
        size = Size(width, height * 0.65f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Cabin / Cargo Box
    if (type == TrafficType.BOX_TRUCK) {
        drawRoundRect(
            color = Color(0xFFF1F5F9),
            topLeft = Offset(left + width * 0.04f, top),
            size = Size(width * 0.92f, height * 0.66f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    } else {
        drawRoundRect(
            color = bodyColor.copy(
                red = (bodyColor.red * 0.85f),
                green = (bodyColor.green * 0.85f),
                blue = (bodyColor.blue * 0.85f)
            ),
            topLeft = Offset(left + width * 0.14f, top),
            size = Size(width * 0.72f, height * 0.42f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Rear Glass
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(left + width * 0.20f, top + height * 0.07f),
            size = Size(width * 0.60f, height * 0.26f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Taxi Roof Sign
        if (type == TrafficType.CITY_CAB) {
            drawRect(
                color = Color(0xFFFEF08A),
                topLeft = Offset(centerX - width * 0.12f, top - height * 0.08f),
                size = Size(width * 0.24f, height * 0.09f)
            )
        }
    }

    // Taillights
    val tailW = width * 0.16f
    val tailH = height * 0.13f
    drawRect(
        color = Color(0xFFEF4444),
        topLeft = Offset(left + width * 0.08f, top + height * 0.48f),
        size = Size(tailW, tailH)
    )
    drawRect(
        color = Color(0xFFEF4444),
        topLeft = Offset(left + width - tailW - width * 0.08f, top + height * 0.48f),
        size = Size(tailW, tailH)
    )
}

private fun DrawScope.drawRealisticRearSupercar(
    centerX: Float,
    bottomY: Float,
    carWidth: Float,
    bodyColor: Color,
    hasWing: Boolean,
    isBraking: Boolean,
    isNitro: Boolean,
    leanAngle: Float,
    showDriverSilhouette: Boolean,
    frameTick: Long
) {
    val carHeight = carWidth * 0.44f
    val left = centerX - carWidth * 0.5f
    val top = bottomY - carHeight

    rotate(degrees = leanAngle, pivot = Offset(centerX, bottomY - carHeight * 0.3f)) {
        // 1. Contact Ground Shadow + Underbody Glow
        drawOval(
            color = Color.Black.copy(alpha = 0.7f),
            topLeft = Offset(left - carWidth * 0.08f, bottomY - carHeight * 0.14f),
            size = Size(carWidth * 1.16f, carHeight * 0.28f)
        )
        if (isNitro) {
            drawOval(
                color = Color(0xFF06B6D4).copy(alpha = 0.32f),
                topLeft = Offset(left, bottomY - carHeight * 0.10f),
                size = Size(carWidth, carHeight * 0.20f)
            )
        }

        // 2. Wide Performance Rear Tires
        val tireW = carWidth * 0.18f
        val tireH = carHeight * 0.34f
        drawRoundRect(
            color = Color(0xFF09090B),
            topLeft = Offset(left + carWidth * 0.02f, bottomY - tireH),
            size = Size(tireW, tireH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = Color(0xFF09090B),
            topLeft = Offset(left + carWidth - tireW - carWidth * 0.02f, bottomY - tireH),
            size = Size(tireW, tireH),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // 3. Lower Carbon Diffuser
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(left + carWidth * 0.08f, bottomY - carHeight * 0.30f),
            size = Size(carWidth * 0.84f, carHeight * 0.22f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // 4. Sculpted Metallic Rear Bodywork
        val bodyPath = Path().apply {
            moveTo(left + carWidth * 0.03f, bottomY - carHeight * 0.22f)
            lineTo(left + carWidth * 0.06f, top + carHeight * 0.36f)
            lineTo(left + carWidth * 0.94f, top + carHeight * 0.36f)
            lineTo(left + carWidth * 0.97f, bottomY - carHeight * 0.22f)
            close()
        }
        drawPath(
            path = bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    bodyColor,
                    bodyColor.copy(
                        red = bodyColor.red * 0.72f,
                        green = bodyColor.green * 0.72f,
                        blue = bodyColor.blue * 0.72f
                    )
                ),
                startY = top + carHeight * 0.35f,
                endY = bottomY
            )
        )

        // 5. Cabin & Sloped Fastback Roof
        val cabinPath = Path().apply {
            moveTo(left + carWidth * 0.16f, top + carHeight * 0.38f)
            lineTo(left + carWidth * 0.26f, top)
            lineTo(left + carWidth * 0.74f, top)
            lineTo(left + carWidth * 0.84f, top + carHeight * 0.38f)
            close()
        }
        drawPath(path = cabinPath, color = bodyColor)

        // Wing Mirrors
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(left + carWidth * 0.05f, top + carHeight * 0.26f),
            size = Size(carWidth * 0.10f, carHeight * 0.09f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(left + carWidth * 0.85f, top + carHeight * 0.26f),
            size = Size(carWidth * 0.10f, carHeight * 0.09f),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // 6. Tinted Rear Windshield + Visible Real Human Driver Helmet/Shoulders Inside
        val glassPath = Path().apply {
            moveTo(left + carWidth * 0.21f, top + carHeight * 0.35f)
            lineTo(left + carWidth * 0.29f, top + carHeight * 0.07f)
            lineTo(left + carWidth * 0.71f, top + carHeight * 0.07f)
            lineTo(left + carWidth * 0.79f, top + carHeight * 0.35f)
            close()
        }
        drawPath(
            path = glassPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF090D16)),
                startY = top,
                endY = top + carHeight * 0.35f
            )
        )

        if (showDriverSilhouette) {
            // Driver on left-hand drive seat visible through rear glass
            val driverX = left + carWidth * 0.39f
            val driverTop = top + carHeight * 0.11f
            // Racing suit shoulders
            drawOval(
                color = Color(0xFF334155),
                topLeft = Offset(driverX - carWidth * 0.06f, driverTop + carHeight * 0.12f),
                size = Size(carWidth * 0.14f, carHeight * 0.12f)
            )
            // Helmet with visor reflection
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = carWidth * 0.042f,
                center = Offset(driverX + carWidth * 0.01f, driverTop + carHeight * 0.09f)
            )
        }

        // 7. Aerodynamic Carbon GT Wing
        if (hasWing) {
            drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(left + carWidth * 0.25f, top + carHeight * 0.14f),
                size = Size(carWidth * 0.04f, carHeight * 0.24f)
            )
            drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(left + carWidth * 0.71f, top + carHeight * 0.14f),
                size = Size(carWidth * 0.04f, carHeight * 0.24f)
            )
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(left + carWidth * 0.07f, top + carHeight * 0.08f),
                size = Size(carWidth * 0.86f, carHeight * 0.09f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // 8. LED Taillights + Braking Flare
        val tailColor = if (isBraking) Color(0xFFFF1E42) else Color(0xFFDC2626)
        val tailAlpha = if (isBraking) 1f else 0.85f
        val tailW = carWidth * 0.21f
        val tailH = carHeight * 0.12f
        drawRoundRect(
            color = tailColor.copy(alpha = tailAlpha),
            topLeft = Offset(left + carWidth * 0.09f, top + carHeight * 0.47f),
            size = Size(tailW, tailH),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = tailColor.copy(alpha = tailAlpha),
            topLeft = Offset(left + carWidth - tailW - carWidth * 0.09f, top + carHeight * 0.47f),
            size = Size(tailW, tailH),
            cornerRadius = CornerRadius(4f, 4f)
        )
        if (isBraking) {
            drawCircle(
                color = Color(0xFFFF1E42).copy(alpha = 0.35f),
                radius = carWidth * 0.14f,
                center = Offset(left + carWidth * 0.19f, top + carHeight * 0.53f)
            )
            drawCircle(
                color = Color(0xFFFF1E42).copy(alpha = 0.35f),
                radius = carWidth * 0.14f,
                center = Offset(left + carWidth * 0.81f, top + carHeight * 0.53f)
            )
        }

        // 9. Twin Exhaust Pipes + Animated Nitro Flame Jets
        val ex1 = left + carWidth * 0.26f
        val ex2 = left + carWidth * 0.74f
        val exY = bottomY - carHeight * 0.17f
        val exR = (carWidth * 0.032f).coerceAtLeast(3f)
        drawCircle(color = Color(0xFF64748B), radius = exR, center = Offset(ex1, exY))
        drawCircle(color = Color(0xFF64748B), radius = exR, center = Offset(ex2, exY))

        if (isNitro) {
            val flicker = 0.85f + ((frameTick % 4) * 0.12f)
            val flameLen = carHeight * 0.45f * flicker
            for (ex in listOf(ex1, ex2)) {
                drawOval(
                    color = Color(0xFF06B6D4).copy(alpha = 0.85f),
                    topLeft = Offset(ex - exR * 1.4f, exY - exR),
                    size = Size(exR * 2.8f, flameLen)
                )
                drawOval(
                    color = Color(0xFFE0F2FE),
                    topLeft = Offset(ex - exR * 0.7f, exY - exR * 0.5f),
                    size = Size(exR * 1.4f, flameLen * 0.65f)
                )
            }
        }
    }
}
