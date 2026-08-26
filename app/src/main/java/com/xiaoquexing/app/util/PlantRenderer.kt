package com.xiaoquexing.app.util

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.xiaoquexing.app.data.entity.PlantType
import com.xiaoquexing.app.data.entity.PlantStage
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object PlantRenderer {

    // Reusable palette
    private val BROWN_DARK = Color(0xFF5D4037)
    private val BROWN = Color(0xFF795548)
    private val BROWN_LIGHT = Color(0xFF8D6E63)
    private val SOIL = Color(0xFF6D4C41)
    private val SOIL_LIGHT = Color(0xFF8D6E63)
    private val GREEN_DARK = Color(0xFF2E7D32)
    private val GREEN = Color(0xFF4CAF50)
    private val GREEN_LIGHT = Color(0xFF81C784)
    private val GREEN_PALE = Color(0xFFA5D6A7)
    private val YELLOW = Color(0xFFFFEB3B)
    private val YELLOW_DARK = Color(0xFFF9A825)
    private val PINK = Color(0xFFF48FB1)
    private val PINK_DARK = Color(0xFFEC407A)
    private val PINK_LIGHT = Color(0xFFF8BBD0)
    private val WHITE = Color(0xFFFFFFFF)
    private val CREAM = Color(0xFFFFF8E1)
    private val CACTUS_GREEN = Color(0xFF66BB6A)
    private val CACTUS_DARK = Color(0xFF388E3C)
    private val RED = Color(0xFFE53935)
    private val RED_DARK = Color(0xFFB71C1C)
    private val PURPLE = Color(0xFFAB47BC)
    private val BAMBOO_GREEN = Color(0xFF66BB6A)
    private val BAMBOO_LIGHT = Color(0xFFA5D6A7)
    private val MUSHROOM_RED = Color(0xFFE53935)
    private val MUSHROOM_CAP_BROWN = Color(0xFF795548)
    private val MUSHROOM_WHITE = Color(0xFFFFFDE7)
    private val SKY = Color(0xFFE1F5FE)
    private val ORANGE = Color(0xFFFF9800)
    private val GOLD = Color(0xFFFFD700)

    fun drawPlant(
        drawScope: DrawScope,
        plantType: PlantType,
        stage: PlantStage,
        progressInStage: Float,
        showPot: Boolean = true
    ) {
        with(drawScope) {
            val w = size.width
            val h = size.height
            val groundY = h * 0.82f
            val cx = w / 2f

            // Draw sky background (very subtle gradient)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF1F8E9), Color(0xFFE8F5E9)),
                    startY = 0f, endY = groundY
                ),
                size = Size(w, groundY)
            )

            // Draw ground
            drawGround(cx, groundY, w)

            // Draw pot if needed
            if (showPot) drawPot(cx, groundY)

            val plantBaseY = groundY
            val stageIdx = stage.ordinal
            val animP = progressInStage.coerceIn(0f, 1f)

            when (plantType) {
                PlantType.TREE -> drawTree(cx, plantBaseY, stageIdx, animP)
                PlantType.SAKURA -> drawSakura(cx, plantBaseY, stageIdx, animP)
                PlantType.SUNFLOWER -> drawSunflower(cx, plantBaseY, stageIdx, animP)
                PlantType.CACTUS -> drawCactus(cx, plantBaseY, stageIdx, animP)
                PlantType.SUCCULENT -> drawSucculent(cx, plantBaseY, stageIdx, animP)
                PlantType.VINE -> drawVine(cx, plantBaseY, stageIdx, animP)
                PlantType.ROSE -> drawRose(cx, plantBaseY, stageIdx, animP)
                PlantType.BAMBOO -> drawBamboo(cx, plantBaseY, stageIdx, animP)
                PlantType.MUSHROOM -> drawMushroom(cx, plantBaseY, stageIdx, animP)
            }
        }
    }

    private fun DrawScope.drawGround(cx: Float, groundY: Float, w: Float) {
        // Soil mound
        val soilPath = Path().apply {
            moveTo(0f, groundY + 20f)
            quadraticBezierTo(cx, groundY - 15f, w, groundY + 20f)
            lineTo(w, groundY + 60f)
            lineTo(0f, groundY + 60f)
            close()
        }
        drawPath(soilPath, SOIL)
        // Top highlight
        drawLine(
            SOIL_LIGHT,
            start = Offset(w * 0.1f, groundY + 2f),
            end = Offset(w * 0.9f, groundY + 2f),
            strokeWidth = 3f
        )
    }

    private fun DrawScope.drawPot(cx: Float, groundY: Float) {
        val potW = 120f
        val potH = 60f
        val potTop = groundY + 4f
        // Pot body
        drawRoundRect(
            color = Color(0xFFD7CCC8),
            topLeft = Offset(cx - potW / 2, potTop),
            size = Size(potW, potH),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // Pot rim
        drawRoundRect(
            color = Color(0xFFBCAAA4),
            topLeft = Offset(cx - potW / 2 - 6f, potTop - 6f),
            size = Size(potW + 12f, 12f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Pot shadow line
        drawLine(
            Color(0xFFA1887F).copy(alpha = 0.3f),
            start = Offset(cx - potW / 2 + 8f, potTop + potH - 8f),
            end = Offset(cx + potW / 2 - 8f, potTop + potH - 8f),
            strokeWidth = 2f
        )
    }

    // ========== 1. TREE (小确幸之树) ==========
    private fun DrawScope.drawTree(cx: Float, baseY: Float, stage: Int, p: Float) {
        when (stage) {
            0 -> { // Seed
                drawOval(BROWN, topLeft = Offset(cx - 8f, baseY - 14f), size = Size(16f, 12f))
                drawLine(GREEN_DARK, start = Offset(cx, baseY - 14f), end = Offset(cx, baseY - 18f), strokeWidth = 2f)
            }
            1 -> { // Sprout
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx, baseY - 30f * p), strokeWidth = 3f)
                drawOval(GREEN_LIGHT, topLeft = Offset(cx - 6f, baseY - 30f * p - 12f), size = Size(12f, 16f))
                drawOval(GREEN_PALE, topLeft = Offset(cx + 2f, baseY - 22f * p - 6f), size = Size(10f, 14f))
            }
            2 -> { // Seedling
                val trunkH = 70f * p + 20f
                drawRect(BROWN, topLeft = Offset(cx - 5f, baseY - trunkH), size = Size(10f, trunkH))
                drawCircle(GREEN_LIGHT, radius = 28f, center = Offset(cx, baseY - trunkH - 5f))
                drawCircle(GREEN, radius = 22f, center = Offset(cx - 8f, baseY - trunkH - 18f))
                drawCircle(GREEN, radius = 22f, center = Offset(cx + 10f, baseY - trunkH - 12f))
            }
            3 -> { // Growing
                val trunkH = 150f * p + 50f
                drawRect(BROWN, topLeft = Offset(cx - 8f, baseY - trunkH), size = Size(16f, trunkH))
                val top = baseY - trunkH
                drawCircle(GREEN, radius = 45f, center = Offset(cx, top - 20f))
                drawCircle(GREEN_LIGHT, radius = 38f, center = Offset(cx - 30f, top - 5f))
                drawCircle(GREEN_LIGHT, radius = 38f, center = Offset(cx + 30f, top - 10f))
                drawCircle(GREEN_DARK, radius = 30f, center = Offset(cx, top - 35f))
                // branches
                drawLine(BROWN, start = Offset(cx - 8f, top + 20f), end = Offset(cx - 35f, top + 5f), strokeWidth = 5f)
                drawLine(BROWN, start = Offset(cx + 8f, top + 30f), end = Offset(cx + 38f, top + 15f), strokeWidth = 5f)
            }
            4 -> { // Flourishing
                val trunkH = 200f * p + 100f
                drawRect(BROWN_DARK, topLeft = Offset(cx - 12f, baseY - trunkH), size = Size(24f, trunkH))
                val top = baseY - trunkH
                // Massive foliage
                listOf(
                    Offset(cx - 55f, top - 10f) to 55f,
                    Offset(cx + 55f, top - 15f) to 52f,
                    Offset(cx, top - 60f) to 65f,
                    Offset(cx - 30f, top - 40f) to 45f,
                    Offset(cx + 35f, top - 45f) to 48f,
                    Offset(cx, top + 10f) to 50f
                ).forEach { (off, r) ->
                    drawCircle(GREEN, radius = r * p.coerceAtLeast(0.5f), center = off)
                }
                // branches
                drawLine(BROWN_DARK, start = Offset(cx - 12f, top + 30f), end = Offset(cx - 60f, top), strokeWidth = 7f)
                drawLine(BROWN_DARK, start = Offset(cx + 12f, top + 40f), end = Offset(cx + 65f, top + 5f), strokeWidth = 7f)
            }
            5 -> { // Mature - with fruits
                val trunkH = 230f * p + 150f
                drawRect(BROWN_DARK, topLeft = Offset(cx - 15f, baseY - trunkH), size = Size(30f, trunkH))
                val top = baseY - trunkH
                listOf(
                    Offset(cx - 60f, top - 10f) to 62f,
                    Offset(cx + 60f, top - 20f) to 58f,
                    Offset(cx, top - 75f) to 75f,
                    Offset(cx - 35f, top - 50f) to 52f,
                    Offset(cx + 40f, top - 55f) to 55f,
                    Offset(cx, top + 15f) to 58f
                ).forEach { (off, r) -> drawCircle(GREEN_DARK, radius = r, center = off) }
                drawLine(BROWN_DARK, start = Offset(cx - 15f, top + 40f), end = Offset(cx - 70f, top + 5f), strokeWidth = 8f)
                drawLine(BROWN_DARK, start = Offset(cx + 15f, top + 50f), end = Offset(cx + 75f, top + 10f), strokeWidth = 8f)
                // fruits (red apples/oranges)
                val fruitPositions = listOf(
                    Offset(cx - 30f, top - 30f), Offset(cx + 40f, top - 25f),
                    Offset(cx, top - 90f), Offset(cx - 55f, top + 20f),
                    Offset(cx + 50f, top + 5f), Offset(cx + 15f, top - 60f)
                )
                fruitPositions.forEach { pos ->
                    drawCircle(RED, radius = 8f, center = pos)
                    drawCircle(RED_DARK, radius = 3f, center = Offset(pos.x, pos.y - 8f))
                }
            }
            6 -> { // Divine - magical golden tree
                val trunkH = 260f * p + 200f
                drawRect(BROWN_DARK, topLeft = Offset(cx - 18f, baseY - trunkH), size = Size(36f, trunkH))
                val top = baseY - trunkH
                // Golden aura
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(GOLD.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(cx, top - 40f),
                        radius = 180f
                    ),
                    radius = 180f,
                    center = Offset(cx, top - 40f)
                )
                listOf(
                    Offset(cx - 70f, top - 10f) to 72f,
                    Offset(cx + 70f, top - 20f) to 68f,
                    Offset(cx, top - 90f) to 88f,
                    Offset(cx - 40f, top - 60f) to 60f,
                    Offset(cx + 45f, top - 65f) to 63f,
                    Offset(cx, top + 20f) to 68f
                ).forEach { (off, r) -> drawCircle(GREEN_DARK, radius = r, center = off) }
                // Golden fruits
                val fruitPositions = listOf(
                    Offset(cx - 30f, top - 30f), Offset(cx + 40f, top - 25f),
                    Offset(cx, top - 110f), Offset(cx - 55f, top + 20f),
                    Offset(cx + 50f, top + 5f), Offset(cx + 15f, top - 70f),
                    Offset(cx - 60f, top - 30f), Offset(cx - 10f, top + 5f)
                )
                fruitPositions.forEach { pos ->
                    drawCircle(GOLD, radius = 9f, center = pos)
                    drawCircle(YELLOW, radius = 4f, center = pos)
                }
                // Sparkles
                repeat(8) { i ->
                    val angle = (i * 45f) * 0.01745f
                    val sx = cx + kotlin.math.cos(angle.toDouble()).toFloat() * 130f
                    val sy = top - 40f + kotlin.math.sin(angle.toDouble()).toFloat() * 110f
                    drawCircle(YELLOW, radius = 3f, center = Offset(sx, sy))
                }
            }
        }
    }

    // ========== 2. SAKURA ==========
    private fun DrawScope.drawSakura(cx: Float, baseY: Float, stage: Int, p: Float) {
        when (stage) {
            0 -> drawSeed(cx, baseY, PINK)
            1 -> drawSprout(cx, baseY, p, Color(0xFFF8BBD0), BROWN)
            2 -> {
                val trunkH = 60f * p + 25f
                drawRect(BROWN, topLeft = Offset(cx - 5f, baseY - trunkH), size = Size(10f, trunkH))
                val top = baseY - trunkH
                drawCircle(PINK_LIGHT, radius = 26f, center = Offset(cx, top - 10f))
                drawCircle(PINK, radius = 18f, center = Offset(cx - 8f, top - 22f))
                drawCircle(PINK, radius = 18f, center = Offset(cx + 8f, top - 16f))
            }
            3 -> {
                val trunkH = 130f * p + 60f
                drawRect(BROWN, topLeft = Offset(cx - 8f, baseY - trunkH), size = Size(16f, trunkH))
                val top = baseY - trunkH
                listOf(
                    Offset(cx, top - 30f) to 40f,
                    Offset(cx - 28f, top - 10f) to 35f,
                    Offset(cx + 28f, top - 15f) to 35f,
                    Offset(cx - 10f, top - 50f) to 30f,
                    Offset(cx + 12f, top - 45f) to 32f
                ).forEach { (off, r) -> drawCircle(PINK, radius = r * p.coerceAtLeast(0.5f), center = off) }
            }
            4, 5 -> { // Full bloom
                val trunkH = if (stage == 5) 220f * p + 130f else 180f * p + 90f
                drawRect(BROWN_DARK, topLeft = Offset(cx - 12f, baseY - trunkH), size = Size(24f, trunkH))
                val top = baseY - trunkH
                listOf(
                    Offset(cx - 50f, top - 5f) to 55f,
                    Offset(cx + 50f, top - 15f) to 52f,
                    Offset(cx, top - 70f) to 70f,
                    Offset(cx - 30f, top - 45f) to 42f,
                    Offset(cx + 32f, top - 50f) to 44f,
                    Offset(cx, top + 15f) to 50f,
                    Offset(cx - 65f, top - 30f) to 35f,
                    Offset(cx + 65f, top - 35f) to 33f
                ).forEach { (off, r) -> drawCircle(PINK_LIGHT, radius = r, center = off) }
                listOf(
                    Offset(cx - 50f, top - 5f), Offset(cx + 50f, top - 15f),
                    Offset(cx, top - 70f), Offset(cx - 30f, top - 45f),
                    Offset(cx + 32f, top - 50f), Offset(cx, top + 15f)
                ).forEach { off ->
                    drawCircle(PINK, radius = 20f, center = off)
                }
                // Branches
                drawLine(BROWN_DARK, start = Offset(cx - 12f, top + 30f), end = Offset(cx - 60f, top + 5f), strokeWidth = 6f)
                drawLine(BROWN_DARK, start = Offset(cx + 12f, top + 40f), end = Offset(cx + 65f, top + 10f), strokeWidth = 6f)
                // Falling petals
                drawPetals(cx, top - 80f, baseY, 12)
            }
            6 -> { // Divine sakura
                val trunkH = 250f * p + 180f
                drawRect(BROWN_DARK, topLeft = Offset(cx - 15f, baseY - trunkH), size = Size(30f, trunkH))
                val top = baseY - trunkH
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(PINK.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(cx, top - 50f), radius = 180f
                    ), radius = 180f, center = Offset(cx, top - 50f)
                )
                listOf(
                    Offset(cx - 65f, top - 10f) to 65f,
                    Offset(cx + 65f, top - 20f) to 62f,
                    Offset(cx, top - 90f) to 85f,
                    Offset(cx - 38f, top - 60f) to 50f,
                    Offset(cx + 40f, top - 65f) to 52f,
                    Offset(cx, top + 20f) to 60f,
                    Offset(cx - 80f, top - 40f) to 40f,
                    Offset(cx + 80f, top - 45f) to 38f
                ).forEach { (off, r) -> drawCircle(PINK_LIGHT, radius = r, center = off) }
                listOf(
                    Offset(cx, top - 90f), Offset(cx - 65f, top - 10f),
                    Offset(cx + 65f, top - 20f), Offset(cx, top + 20f)
                ).forEach { off -> drawCircle(PINK_DARK, radius = 25f, center = off) }
                drawPetals(cx, top - 100f, baseY, 25)
            }
        }
    }

    private fun DrawScope.drawSeed(cx: Float, baseY: Float, tint: Color) {
        drawOval(BROWN, topLeft = Offset(cx - 8f, baseY - 14f), size = Size(16f, 12f))
        drawCircle(tint, radius = 3f, center = Offset(cx, baseY - 16f))
    }

    private fun DrawScope.drawSprout(cx: Float, baseY: Float, p: Float, leafColor: Color, stemColor: Color) {
        val h = 35f * p
        drawLine(stemColor, start = Offset(cx, baseY), end = Offset(cx, baseY - h), strokeWidth = 3f)
        drawOval(leafColor, topLeft = Offset(cx - 8f, baseY - h - 12f), size = Size(14f, 16f))
        drawOval(leafColor.copy(alpha = 0.8f), topLeft = Offset(cx + 2f, baseY - h * 0.6f - 6f), size = Size(12f, 14f))
    }

    private fun DrawScope.drawPetals(cx: Float, topY: Float, botY: Float, count: Int) {
        val rng = Random(42)
        repeat(count) {
            val px = cx + (rng.nextFloat() - 0.5f) * 280f
            val py = topY + rng.nextFloat() * (botY - topY)
            val s = 3f + rng.nextFloat() * 4f
            drawCircle(PINK, radius = s, center = Offset(px, py))
            drawCircle(PINK_LIGHT, radius = s * 0.6f, center = Offset(px, py))
        }
    }

    // ========== 3. SUNFLOWER ==========
    private fun DrawScope.drawSunflower(cx: Float, baseY: Float, stage: Int, p: Float) {
        when (stage) {
            0 -> drawSeed(cx, baseY, YELLOW_DARK)
            1 -> drawSprout(cx, baseY, p, GREEN_LIGHT, GREEN_DARK)
            2 -> {
                val stemH = 80f * p + 25f
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx, baseY - stemH), strokeWidth = 6f)
                drawOval(GREEN, topLeft = Offset(cx + 5f, baseY - stemH * 0.6f - 8f), size = Size(20f, 12f))
                drawOval(GREEN, topLeft = Offset(cx - 25f, baseY - stemH * 0.4f - 8f), size = Size(20f, 12f))
                drawCircle(YELLOW, radius = 12f, center = Offset(cx, baseY - stemH - 5f))
                drawCircle(BROWN_DARK, radius = 6f, center = Offset(cx, baseY - stemH - 5f))
            }
            3 -> {
                val stemH = 140f * p + 60f
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx, baseY - stemH), strokeWidth = 9f)
                val top = baseY - stemH
                // leaves
                drawLeaf(cx - 5f, baseY - stemH * 0.7f, -35f, GREEN)
                drawLeaf(cx + 5f, baseY - stemH * 0.4f, 35f, GREEN)
                // flower bud
                drawSunflowerHead(cx, top - 20f, 28f)
            }
            4 -> {
                val stemH = 180f * p + 80f
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx, baseY - stemH), strokeWidth = 10f)
                val top = baseY - stemH
                drawLeaf(cx - 5f, baseY - stemH * 0.7f, -45f, GREEN_DARK)
                drawLeaf(cx + 5f, baseY - stemH * 0.5f, 45f, GREEN_DARK)
                drawLeaf(cx - 5f, baseY - stemH * 0.3f, -30f, GREEN)
                // big sunflower head
                drawSunflowerHead(cx, top - 30f, 45f)
            }
            5 -> {
                val stemH = 200f * p + 100f
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx, baseY - stemH), strokeWidth = 11f)
                val top = baseY - stemH
                drawLeaf(cx - 5f, baseY - stemH * 0.7f, -50f, GREEN_DARK)
                drawLeaf(cx + 5f, baseY - stemH * 0.5f, 50f, GREEN_DARK)
                drawLeaf(cx - 5f, baseY - stemH * 0.3f, -40f, GREEN)
                drawLeaf(cx + 5f, baseY - stemH * 0.85f, 38f, GREEN)
                // Multiple sunflower heads
                drawSunflowerHead(cx, top - 35f, 50f)
                drawSunflowerHead(cx - 55f, top + 10f, 30f)
                drawSunflowerHead(cx + 55f, top + 15f, 28f)
            }
            6 -> {
                val stemH = 230f * p + 130f
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx, baseY - stemH), strokeWidth = 13f)
                val top = baseY - stemH
                // Golden glow
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(YELLOW.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(cx, top - 50f), radius = 180f
                    ), radius = 180f, center = Offset(cx, top - 50f)
                )
                drawLeaf(cx - 5f, baseY - stemH * 0.7f, -55f, GREEN_DARK)
                drawLeaf(cx + 5f, baseY - stemH * 0.5f, 55f, GREEN_DARK)
                drawLeaf(cx - 5f, baseY - stemH * 0.3f, -45f, GREEN)
                drawLeaf(cx + 5f, baseY - stemH * 0.85f, 42f, GREEN)
                drawSunflowerHead(cx, top - 40f, 60f)
                drawSunflowerHead(cx - 65f, top, 38f)
                drawSunflowerHead(cx + 65f, top + 5f, 36f)
                drawSunflowerHead(cx - 30f, top - 70f, 28f)
                drawSunflowerHead(cx + 30f, top - 75f, 26f)
            }
        }
    }

    private fun DrawScope.drawSunflowerHead(cx: Float, cy: Float, r: Float) {
        // Petals
        val petalCount = 14
        for (i in 0 until petalCount) {
            val angle = (i * 360f / petalCount) * 0.01745f
            val px = cx + kotlin.math.cos(angle.toDouble()).toFloat() * r * 0.6f
            val py = cy + kotlin.math.sin(angle.toDouble()).toFloat() * r * 0.6f
            val pcx = cx + kotlin.math.cos(angle.toDouble()).toFloat() * r
            val pcy = cy + kotlin.math.sin(angle.toDouble()).toFloat() * r
            drawOval(
                YELLOW,
                topLeft = Offset(minOf(px, pcx) - r * 0.25f, minOf(py, pcy) - r * 0.15f),
                size = Size(r * 0.55f, r * 0.3f)
            )
        }
        // Center disk
        drawCircle(BROWN_DARK, radius = r * 0.5f, center = Offset(cx, cy))
        drawCircle(YELLOW_DARK, radius = r * 0.38f, center = Offset(cx, cy))
        // Seeds pattern
        for (i in 0..5) {
            for (j in 0..8) {
                val sa = j * 45f * 0.01745f + i * 20f * 0.01745f
                val sr = i * r * 0.07f
                val sx = cx + kotlin.math.cos(sa.toDouble()).toFloat() * sr
                val sy = cy + kotlin.math.sin(sa.toDouble()).toFloat() * sr
                drawCircle(BROWN_DARK, radius = 1.5f, center = Offset(sx, sy))
            }
        }
    }

    private fun DrawScope.drawLeaf(x: Float, y: Float, dir: Float, color: Color) {
        val path = Path().apply {
            moveTo(x, y)
            quadraticBezierTo(x + dir, y - 8f, x + dir * 1.2f, y + 12f)
            quadraticBezierTo(x + dir * 0.5f, y + 18f, x, y)
        }
        drawPath(path, color)
    }

    // ========== 4. CACTUS ==========
    private fun DrawScope.drawCactus(cx: Float, baseY: Float, stage: Int, p: Float) {
        when (stage) {
            0 -> drawSeed(cx, baseY, CACTUS_GREEN)
            1 -> { // Sprout - tiny cactus ball
                val h = 30f * p
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx - 8f, baseY - h), size = Size(16f, h), cornerRadius = CornerRadius(8f, 8f))
                drawCactusSpines(cx, baseY - h, 16f, h)
            }
            2 -> {
                val h = 70f * p + 20f
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx - 14f, baseY - h), size = Size(28f, h), cornerRadius = CornerRadius(14f, 14f))
                drawCactusSpines(cx - 14f, baseY - h, 28f, h)
                // Small arm
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx + 10f, baseY - h * 0.6f), size = Size(14f, h * 0.4f), cornerRadius = CornerRadius(7f, 7f))
            }
            3 -> {
                val h = 130f * p + 40f
                drawRoundRect(CACTUS_DARK, topLeft = Offset(cx - 18f, baseY - h), size = Size(36f, h), cornerRadius = CornerRadius(18f, 18f))
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx - 14f, baseY - h), size = Size(28f, h), cornerRadius = CornerRadius(14f, 14f))
                drawCactusSpines(cx - 18f, baseY - h, 36f, h)
                // Arms
                val armH = h * 0.5f
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx + 14f, baseY - h * 0.7f), size = Size(18f, armH), cornerRadius = CornerRadius(9f, 9f))
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx - 32f, baseY - h * 0.5f), size = Size(18f, armH * 0.7f), cornerRadius = CornerRadius(9f, 9f))
            }
            4 -> {
                val h = 170f * p + 70f
                drawRoundRect(CACTUS_DARK, topLeft = Offset(cx - 22f, baseY - h), size = Size(44f, h), cornerRadius = CornerRadius(22f, 22f))
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx - 17f, baseY - h), size = Size(34f, h), cornerRadius = CornerRadius(17f, 17f))
                drawCactusSpines(cx - 22f, baseY - h, 44f, h)
                val armH = h * 0.55f
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx + 18f, baseY - h * 0.75f), size = Size(22f, armH), cornerRadius = CornerRadius(11f, 11f))
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx - 40f, baseY - h * 0.55f), size = Size(22f, armH * 0.7f), cornerRadius = CornerRadius(11f, 11f))
                // Flower on top
                drawCircle(PINK, radius = 12f, center = Offset(cx, baseY - h - 5f))
                drawCircle(WHITE, radius = 5f, center = Offset(cx, baseY - h - 5f))
            }
            5 -> {
                val h = 200f * p + 100f
                drawRoundRect(CACTUS_DARK, topLeft = Offset(cx - 24f, baseY - h), size = Size(48f, h), cornerRadius = CornerRadius(24f, 24f))
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx - 19f, baseY - h), size = Size(38f, h), cornerRadius = CornerRadius(19f, 19f))
                drawCactusSpines(cx - 24f, baseY - h, 48f, h)
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx + 20f, baseY - h * 0.75f), size = Size(26f, h * 0.55f), cornerRadius = CornerRadius(13f, 13f))
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx - 46f, baseY - h * 0.55f), size = Size(26f, h * 0.45f), cornerRadius = CornerRadius(13f, 13f))
                // Multiple colorful flowers
                listOf(
                    Offset(cx, baseY - h - 8f) to PINK,
                    Offset(cx + 26f, baseY - h * 0.75f - 5f) to YELLOW,
                    Offset(cx - 38f, baseY - h * 0.55f - 5f) to RED,
                    Offset(cx - 5f, baseY - h - 5f) to PURPLE
                ).forEach { (off, c) ->
                    drawCircle(c, radius = 11f, center = off)
                    drawCircle(WHITE, radius = 4f, center = off)
                }
            }
            6 -> {
                val h = 240f * p + 130f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(PINK.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(cx, baseY - h / 2f), radius = 200f
                    ), radius = 200f, center = Offset(cx, baseY - h / 2f)
                )
                drawRoundRect(CACTUS_DARK, topLeft = Offset(cx - 28f, baseY - h), size = Size(56f, h), cornerRadius = CornerRadius(28f, 28f))
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx - 22f, baseY - h), size = Size(44f, h), cornerRadius = CornerRadius(22f, 22f))
                drawCactusSpines(cx - 28f, baseY - h, 56f, h)
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx + 24f, baseY - h * 0.7f), size = Size(30f, h * 0.6f), cornerRadius = CornerRadius(15f, 15f))
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx - 54f, baseY - h * 0.55f), size = Size(30f, h * 0.5f), cornerRadius = CornerRadius(15f, 15f))
                drawRoundRect(CACTUS_GREEN, topLeft = Offset(cx - 20f, baseY - h * 0.85f), size = Size(20f, h * 0.35f), cornerRadius = CornerRadius(10f, 10f))
                // Rainbow flowers
                val flowerColors = listOf(PINK, YELLOW, RED, PURPLE, ORANGE, WHITE)
                listOf(
                    Offset(cx, baseY - h - 10f),
                    Offset(cx + 30f, baseY - h * 0.7f - 8f),
                    Offset(cx - 46f, baseY - h * 0.55f - 8f),
                    Offset(cx - 10f, baseY - h - 5f),
                    Offset(cx + 10f, baseY - h - 12f),
                    Offset(cx - 20f, baseY - h * 0.85f - 5f)
                ).forEachIndexed { i, off ->
                    drawCircle(flowerColors[i % flowerColors.size], radius = 12f, center = off)
                    drawCircle(GOLD, radius = 4f, center = off)
                }
            }
        }
    }

    private fun DrawScope.drawCactusSpines(x: Float, y: Float, w: Float, h: Float) {
        val spineColor = WHITE.copy(alpha = 0.9f)
        val rows = (h / 15f).toInt()
        for (r in 0 until rows) {
            val sy = y + 10f + r * 15f
            drawLine(spineColor, Offset(x - 2f, sy), Offset(x - 8f, sy - 4f), strokeWidth = 1.5f)
            drawLine(spineColor, Offset(x + w + 2f, sy), Offset(x + w + 8f, sy - 4f), strokeWidth = 1.5f)
            if (r % 2 == 0) {
                drawLine(spineColor, Offset(x + w / 2f, sy - 3f), Offset(x + w / 2f, sy - 8f), strokeWidth = 1.5f)
            }
        }
    }

    // ========== 5. SUCCULENT ==========
    private fun DrawScope.drawSucculent(cx: Float, baseY: Float, stage: Int, p: Float) {
        when (stage) {
            0 -> drawSeed(cx, baseY, GREEN_LIGHT)
            1 -> { // Tiny rosette
                val r = 15f * p
                drawCircle(GREEN_PALE, radius = r, center = Offset(cx, baseY - r))
                drawCircle(GREEN_LIGHT, radius = r * 0.7f, center = Offset(cx, baseY - r))
            }
            2 -> { // Rosette forming
                val r = 30f * p + 5f
                // Outer leaves
                for (i in 0 until 6) {
                    val a = (i * 60f) * 0.01745f
                    val ox = cx + kotlin.math.cos(a.toDouble()).toFloat() * r * 0.5f
                    val oy = baseY - r + kotlin.math.sin(a.toDouble()).toFloat() * r * 0.3f
                    drawOval(GREEN_PALE, topLeft = Offset(ox - 8f, oy - 14f), size = Size(16f, 22f))
                }
                drawCircle(GREEN_LIGHT, radius = r * 0.5f, center = Offset(cx, baseY - r * 0.8f))
            }
            3 -> { // Full rosette
                val r = 45f * p + 15f
                // Outer leaves
                for (i in 0 until 8) {
                    val a = (i * 45f) * 0.01745f
                    val ox = cx + kotlin.math.cos(a.toDouble()).toFloat() * r * 0.7f
                    val oy = baseY - r * 0.9f + kotlin.math.sin(a.toDouble()).toFloat() * r * 0.4f
                    drawOval(GREEN_PALE, topLeft = Offset(ox - 10f, oy - 20f), size = Size(20f, 30f))
                }
                // Inner leaves
                for (i in 0 until 5) {
                    val a = (i * 72f + 20f) * 0.01745f
                    val ox = cx + kotlin.math.cos(a.toDouble()).toFloat() * r * 0.3f
                    val oy = baseY - r * 0.85f + kotlin.math.sin(a.toDouble()).toFloat() * r * 0.25f
                    drawOval(GREEN_LIGHT, topLeft = Offset(ox - 7f, oy - 15f), size = Size(14f, 24f))
                }
                drawCircle(GREEN, radius = 8f, center = Offset(cx, baseY - r * 0.8f))
            }
            4 -> { // Clustering
                val r = 60f * p + 25f
                // Main rosette
                drawSucculentRosette(cx, baseY - r * 0.7f, r, GREEN_PALE, GREEN_LIGHT, GREEN)
                // Small offset rosettes
                drawSucculentRosette(cx - 40f, baseY - r * 0.4f + 5f, r * 0.55f, Color(0xFFB9F6CA), GREEN_PALE, GREEN_LIGHT)
                drawSucculentRosette(cx + 45f, baseY - r * 0.35f, r * 0.5f, Color(0xFFB9F6CA), GREEN_PALE, GREEN_LIGHT)
            }
            5 -> { // Cluster with flowers
                val r = 70f * p + 40f
                drawSucculentRosette(cx, baseY - r * 0.6f, r, GREEN_PALE, GREEN_LIGHT, GREEN_DARK)
                drawSucculentRosette(cx - 48f, baseY - r * 0.35f + 8f, r * 0.55f, Color(0xFFB9F6CA), GREEN_PALE, GREEN)
                drawSucculentRosette(cx + 50f, baseY - r * 0.3f, r * 0.5f, Color(0xFFB9F6CA), GREEN_PALE, GREEN)
                drawSucculentRosette(cx - 15f, baseY - r * 0.25f + 12f, r * 0.4f, PINK_LIGHT, GREEN_PALE, GREEN_LIGHT)
                drawSucculentRosette(cx + 25f, baseY - r * 0.2f + 10f, r * 0.38f, PINK_LIGHT, GREEN_PALE, GREEN_LIGHT)
                // Small flowers
                drawCircle(PINK, radius = 6f, center = Offset(cx, baseY - r - 10f))
                drawCircle(PINK_DARK, radius = 3f, center = Offset(cx, baseY - r - 10f))
            }
            6 -> { // Divine - magical cluster
                val r = 85f * p + 50f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(PINK_LIGHT.copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(cx, baseY - r * 0.5f), radius = 200f
                    ), radius = 200f, center = Offset(cx, baseY - r * 0.5f)
                )
                drawSucculentRosette(cx, baseY - r * 0.6f, r, Color(0xFFB9F6CA), GREEN_PALE, GREEN_DARK)
                drawSucculentRosette(cx - 55f, baseY - r * 0.35f + 8f, r * 0.55f, PINK_LIGHT, Color(0xFFB9F6CA), GREEN)
                drawSucculentRosette(cx + 58f, baseY - r * 0.3f, r * 0.52f, PINK_LIGHT, Color(0xFFB9F6CA), GREEN)
                drawSucculentRosette(cx - 20f, baseY - r * 0.2f + 15f, r * 0.42f, Color(0xFFE1BEE7), GREEN_PALE, PURPLE)
                drawSucculentRosette(cx + 30f, baseY - r * 0.15f + 12f, r * 0.4f, Color(0xFFB3E5FC), GREEN_PALE, Color(0xFF0288D1))
                drawSucculentRosette(cx - 60f, baseY - r * 0.1f + 5f, r * 0.3f, Color(0xFFFFF9C4), GREEN_PALE, YELLOW_DARK)
                drawSucculentRosette(cx + 62f, baseY - 5f, r * 0.28f, Color(0xFFFFCCBC), GREEN_PALE, ORANGE)
                // Magic sparkles
                repeat(10) { i ->
                    val a = (i * 36f) * 0.01745f
                    val sr = r + 20f
                    drawCircle(PINK, radius = 3f, center = Offset(cx + kotlin.math.cos(a.toDouble()).toFloat() * sr, baseY - r * 0.6f + kotlin.math.sin(a.toDouble()).toFloat() * sr * 0.6f))
                }
            }
        }
    }

    private fun DrawScope.drawSucculentRosette(cx: Float, cy: Float, r: Float, outer: Color, mid: Color, inner: Color) {
        for (i in 0 until 8) {
            val a = (i * 45f) * 0.01745f
            val ox = cx + kotlin.math.cos(a.toDouble()).toFloat() * r * 0.6f
            val oy = cy + kotlin.math.sin(a.toDouble()).toFloat() * r * 0.4f
            drawOval(outer, topLeft = Offset(ox - r * 0.2f, oy - r * 0.38f), size = Size(r * 0.4f, r * 0.55f))
        }
        for (i in 0 until 5) {
            val a = (i * 72f + 20f) * 0.01745f
            val ox = cx + kotlin.math.cos(a.toDouble()).toFloat() * r * 0.28f
            val oy = cy + kotlin.math.sin(a.toDouble()).toFloat() * r * 0.22f
            drawOval(mid, topLeft = Offset(ox - r * 0.15f, oy - r * 0.3f), size = Size(r * 0.3f, r * 0.45f))
        }
        drawCircle(inner, radius = r * 0.15f, center = Offset(cx, cy))
    }

    // ========== 6. VINE ==========
    private fun DrawScope.drawVine(cx: Float, baseY: Float, stage: Int, p: Float) {
        when (stage) {
            0 -> drawSeed(cx, baseY, GREEN_LIGHT)
            1 -> drawSprout(cx, baseY, p, GREEN_LIGHT, GREEN_DARK)
            2 -> {
                val h = 50f * p + 15f
                // Small winding vine
                val path = Path().apply {
                    moveTo(cx, baseY)
                    cubicTo(cx + 15f, baseY - h * 0.3f, cx - 10f, baseY - h * 0.7f, cx, baseY - h)
                }
                drawPath(path, GREEN_DARK, style = Stroke(4f))
                drawOval(GREEN_LIGHT, topLeft = Offset(cx - 5f, baseY - h - 10f), size = Size(12f, 14f))
                drawOval(GREEN, topLeft = Offset(cx - 18f, baseY - h * 0.5f - 5f), size = Size(10f, 12f))
            }
            3 -> {
                val h = 120f * p + 40f
                drawVineString(cx, baseY, h, -1f, 30f)
                drawVineString(cx, baseY, h * 0.9f, 1f, 25f)
            }
            4 -> {
                val h = 160f * p + 80f
                drawVineString(cx - 10f, baseY, h, -1f, 45f)
                drawVineString(cx + 10f, baseY, h * 0.95f, 1f, 40f)
                drawVineString(cx, baseY, h * 1.05f, -0.5f, 30f)
                // Small buds/flowers
                drawCircle(PINK, radius = 5f, center = Offset(cx - 30f, baseY - h * 0.4f))
                drawCircle(PINK, radius = 5f, center = Offset(cx + 25f, baseY - h * 0.6f))
            }
            5 -> {
                val h = 180f * p + 100f
                drawVineString(cx - 20f, baseY, h, -1f, 55f)
                drawVineString(cx + 20f, baseY, h * 0.95f, 1f, 50f)
                drawVineString(cx, baseY, h * 1.02f, -0.5f, 40f)
                drawVineString(cx - 40f, baseY, h * 0.8f, 1f, 35f)
                drawVineString(cx + 40f, baseY, h * 0.85f, -1f, 35f)
                // Flowers scattered
                listOf(
                    Offset(cx - 40f, baseY - h * 0.4f), Offset(cx + 35f, baseY - h * 0.6f),
                    Offset(cx - 20f, baseY - h * 0.8f), Offset(cx + 55f, baseY - h * 0.3f),
                    Offset(cx - 55f, baseY - h * 0.5f), Offset(cx + 20f, baseY - h * 0.85f)
                ).forEach { drawSmallFlower(it, PINK) }
            }
            6 -> {
                val h = 220f * p + 130f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(GREEN_LIGHT.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(cx, baseY - h * 0.4f), radius = 200f
                    ), radius = 200f, center = Offset(cx, baseY - h * 0.4f)
                )
                drawVineString(cx - 30f, baseY, h, -1f, 65f)
                drawVineString(cx + 30f, baseY, h * 0.95f, 1f, 60f)
                drawVineString(cx, baseY, h * 1.02f, -0.5f, 50f)
                drawVineString(cx - 55f, baseY, h * 0.85f, 1f, 45f)
                drawVineString(cx + 55f, baseY, h * 0.88f, -1f, 45f)
                drawVineString(cx - 15f, baseY, h * 0.75f, 0.7f, 30f)
                // Colorful flowers
                val flowerColors = listOf(PINK, PINK_DARK, WHITE, PURPLE, YELLOW)
                listOf(
                    Offset(cx - 50f, baseY - h * 0.4f), Offset(cx + 45f, baseY - h * 0.6f),
                    Offset(cx - 25f, baseY - h * 0.8f), Offset(cx + 65f, baseY - h * 0.3f),
                    Offset(cx - 65f, baseY - h * 0.5f), Offset(cx + 25f, baseY - h * 0.85f),
                    Offset(cx - 10f, baseY - h * 0.95f), Offset(cx + 10f, baseY - h * 0.7f)
                ).forEachIndexed { i, off -> drawSmallFlower(off, flowerColors[i % flowerColors.size]) }
            }
        }
    }

    private fun DrawScope.drawVineString(sx: Float, sy: Float, len: Float, dir: Float, amp: Float) {
        val path = Path().apply {
            moveTo(sx, sy)
            val segments = 8
            for (i in 1..segments) {
                val t = i / segments.toFloat()
                val wave = kotlin.math.sin(t * 3.14159f * 2f) * amp * dir
                val px = sx + wave * t
                val py = sy - len * t
                if (i == 1) quadraticBezierTo(sx + wave * 0.5f * dir, sy - len * 0.5f * t, px, py)
                else lineTo(px, py)
            }
        }
        drawPath(path, GREEN_DARK, style = Stroke(3.5f))
        // Leaves along vine
        val segments = 6
        for (i in 1..segments) {
            val t = i / segments.toFloat()
            val wave = kotlin.math.sin(t * 3.14159f * 2f) * amp * dir
            val px = sx + wave * t
            val py = sy - len * t
            drawOval(GREEN_LIGHT, topLeft = Offset(px + dir * 3f, py - 6f), size = Size(14f, 10f))
            drawOval(GREEN, topLeft = Offset(px - dir * 13f, py - 4f), size = Size(12f, 8f))
        }
    }

    private fun DrawScope.drawSmallFlower(pos: Offset, color: Color) {
        for (i in 0 until 5) {
            val a = (i * 72f) * 0.01745f
            drawCircle(
                color,
                radius = 5f,
                center = Offset(pos.x + kotlin.math.cos(a.toDouble()).toFloat() * 5f, pos.y + kotlin.math.sin(a.toDouble()).toFloat() * 5f)
            )
        }
        drawCircle(YELLOW, radius = 3f, center = pos)
    }

    // ========== 7. ROSE ==========
    private fun DrawScope.drawRose(cx: Float, baseY: Float, stage: Int, p: Float) {
        when (stage) {
            0 -> drawSeed(cx, baseY, RED)
            1 -> drawSprout(cx, baseY, p, RED.copy(red = 0.6f, green = 0.4f, blue = 0.4f), GREEN_DARK)
            2 -> {
                val h = 50f * p + 25f
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx, baseY - h), strokeWidth = 4f)
                drawLeaf(cx + 2f, baseY - h * 0.4f, 20f, GREEN_DARK)
                drawRoseBud(cx, baseY - h - 8f, 12f, RED)
            }
            3 -> {
                val h = 110f * p + 40f
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx, baseY - h), strokeWidth = 5f)
                drawLeaf(cx + 3f, baseY - h * 0.6f, 30f, GREEN_DARK)
                drawLeaf(cx - 3f, baseY - h * 0.3f, -25f, GREEN_DARK)
                // Rose thorn
                drawLine(Color(0xFF4E342E), start = Offset(cx, baseY - h * 0.5f), end = Offset(cx + 6f, baseY - h * 0.5f + 6f), strokeWidth = 2f)
                drawRoseBud(cx, baseY - h - 12f, 20f, RED)
                drawRoseBud(cx + 25f, baseY - h * 0.7f, 14f, PINK_DARK)
            }
            4 -> { // Bush forming
                val h = 140f * p + 70f
                // Multiple stems
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx - 10f, baseY - h), strokeWidth = 5f)
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx + 12f, baseY - h * 0.9f), strokeWidth = 5f)
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx, baseY - h * 0.85f), strokeWidth = 4f)
                // Leaves
                drawLeaf(cx + 3f, baseY - h * 0.6f, 35f, GREEN_DARK)
                drawLeaf(cx - 3f, baseY - h * 0.4f, -30f, GREEN_DARK)
                drawLeaf(cx - 10f, baseY - h * 0.75f, -25f, GREEN)
                drawLeaf(cx + 12f, baseY - h * 0.55f, 28f, GREEN)
                // Roses
                drawRoseBud(cx - 10f, baseY - h - 15f, 25f, RED)
                drawRoseBud(cx + 15f, baseY - h * 0.9f - 12f, 20f, PINK_DARK)
                drawRoseBud(cx, baseY - h * 0.85f - 10f, 18f, RED_DARK)
            }
            5 -> { // Full rose bush
                val h = 170f * p + 90f
                drawLine(GREEN_DARK, start = Offset(cx - 20f, baseY), end = Offset(cx - 25f, baseY - h), strokeWidth = 5f)
                drawLine(GREEN_DARK, start = Offset(cx + 20f, baseY), end = Offset(cx + 28f, baseY - h * 0.92f), strokeWidth = 5f)
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx - 5f, baseY - h * 1.02f), strokeWidth = 5f)
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx + 8f, baseY - h * 0.85f), strokeWidth = 4f)
                // Foliage
                listOf(
                    Offset(cx - 30f, baseY - h * 0.7f) to 35f,
                    Offset(cx + 30f, baseY - h * 0.6f) to 33f,
                    Offset(cx - 10f, baseY - h * 0.5f) to 38f,
                    Offset(cx, baseY - h * 0.3f) to 40f
                ).forEach { (off, r) -> drawCircle(GREEN, radius = r, center = off) }
                // Roses
                drawRoseBud(cx - 25f, baseY - h - 18f, 28f, RED)
                drawRoseBud(cx + 30f, baseY - h * 0.92f - 15f, 25f, PINK_DARK)
                drawRoseBud(cx - 5f, baseY - h * 1.02f - 20f, 30f, RED_DARK)
                drawRoseBud(cx + 8f, baseY - h * 0.85f - 12f, 22f, PINK)
                drawRoseBud(cx - 40f, baseY - h * 0.6f - 8f, 18f, RED)
                drawRoseBud(cx + 40f, baseY - h * 0.5f - 8f, 18f, PINK_DARK)
            }
            6 -> { // Divine rose garden
                val h = 200f * p + 110f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(RED.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(cx, baseY - h * 0.5f), radius = 200f
                    ), radius = 200f, center = Offset(cx, baseY - h * 0.5f)
                )
                drawLine(GREEN_DARK, start = Offset(cx - 30f, baseY), end = Offset(cx - 35f, baseY - h), strokeWidth = 6f)
                drawLine(GREEN_DARK, start = Offset(cx + 30f, baseY), end = Offset(cx + 38f, baseY - h * 0.92f), strokeWidth = 6f)
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx - 8f, baseY - h * 1.05f), strokeWidth = 6f)
                drawLine(GREEN_DARK, start = Offset(cx, baseY), end = Offset(cx + 10f, baseY - h * 0.88f), strokeWidth = 5f)
                listOf(
                    Offset(cx - 40f, baseY - h * 0.7f) to 45f,
                    Offset(cx + 42f, baseY - h * 0.65f) to 42f,
                    Offset(cx - 15f, baseY - h * 0.5f) to 50f,
                    Offset(cx + 15f, baseY - h * 0.4f) to 48f,
                    Offset(cx, baseY - h * 0.25f) to 50f
                ).forEach { (off, r) -> drawCircle(GREEN_DARK, radius = r, center = off) }
                val roseColors = listOf(RED, PINK_DARK, RED_DARK, PINK, GOLD)
                listOf(
                    Triple(cx - 35f, baseY - h - 20f, 32f),
                    Triple(cx + 40f, baseY - h * 0.92f - 18f, 28f),
                    Triple(cx - 8f, baseY - h * 1.05f - 25f, 35f),
                    Triple(cx + 10f, baseY - h * 0.88f - 15f, 26f),
                    Triple(cx - 50f, baseY - h * 0.6f - 10f, 22f),
                    Triple(cx + 52f, baseY - h * 0.55f - 10f, 22f),
                    Triple(cx, baseY - h * 0.25f - 12f, 24f),
                    Triple(cx - 25f, baseY - h * 0.4f - 10f, 20f)
                ).forEachIndexed { i, (x, y, r) -> drawRoseBud(x, y, r, roseColors[i % roseColors.size]) }
            }
        }
    }

    private fun DrawScope.drawRoseBud(cx: Float, cy: Float, r: Float, color: Color) {
        // Outer petals
        for (i in 0 until 6) {
            val a = (i * 60f) * 0.01745f
            drawCircle(
                color.copy(alpha = 0.9f),
                radius = r * 0.5f,
                center = Offset(cx + kotlin.math.cos(a.toDouble()).toFloat() * r * 0.4f, cy + kotlin.math.sin(a.toDouble()).toFloat() * r * 0.4f)
            )
        }
        drawCircle(color, radius = r * 0.55f, center = Offset(cx, cy))
        drawCircle(color.copy(alpha = 0.7f), radius = r * 0.4f, center = Offset(cx, cy - r * 0.1f))
        // Inner swirl
        drawCircle(WHITE.copy(alpha = 0.3f), radius = r * 0.15f, center = Offset(cx - r * 0.1f, cy - r * 0.1f))
    }

    // ========== 8. BAMBOO ==========
    private fun DrawScope.drawBamboo(cx: Float, baseY: Float, stage: Int, p: Float) {
        when (stage) {
            0 -> drawSeed(cx, baseY, BAMBOO_GREEN)
            1 -> drawSprout(cx, baseY, p, BAMBOO_LIGHT, BAMBOO_GREEN)
            2 -> {
                val h = 80f * p + 20f
                drawBambooStalk(cx - 5f, baseY, h, 8f)
                drawBambooLeaves(cx - 5f, baseY - h, 15f)
            }
            3 -> {
                val h = 140f * p + 50f
                drawBambooStalk(cx - 12f, baseY, h, 11f)
                drawBambooStalk(cx + 10f, baseY, h * 0.85f, 9f)
                drawBambooLeaves(cx - 12f, baseY - h, 25f)
                drawBambooLeaves(cx + 10f, baseY - h * 0.85f, 20f)
            }
            4 -> {
                val h = 180f * p + 80f
                drawBambooStalk(cx - 25f, baseY, h, 12f)
                drawBambooStalk(cx, baseY, h * 1.05f, 14f)
                drawBambooStalk(cx + 25f, baseY, h * 0.9f, 11f)
                drawBambooLeaves(cx - 25f, baseY - h, 30f)
                drawBambooLeaves(cx, baseY - h * 1.05f, 35f)
                drawBambooLeaves(cx + 25f, baseY - h * 0.9f, 28f)
            }
            5 -> {
                val h = 210f * p + 100f
                drawBambooStalk(cx - 35f, baseY, h, 13f)
                drawBambooStalk(cx - 10f, baseY, h * 1.05f, 15f)
                drawBambooStalk(cx + 18f, baseY, h * 0.95f, 13f)
                drawBambooStalk(cx + 42f, baseY, h * 0.8f, 10f)
                drawBambooLeaves(cx - 35f, baseY - h, 35f)
                drawBambooLeaves(cx - 10f, baseY - h * 1.05f, 40f)
                drawBambooLeaves(cx + 18f, baseY - h * 0.95f, 35f)
                drawBambooLeaves(cx + 42f, baseY - h * 0.8f, 28f)
                // Falling leaves
                drawBambooFallingLeaves(cx, baseY - h, baseY, 8)
            }
            6 -> {
                val h = 250f * p + 130f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(BAMBOO_LIGHT.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(cx, baseY - h * 0.5f), radius = 200f
                    ), radius = 200f, center = Offset(cx, baseY - h * 0.5f)
                )
                drawBambooStalk(cx - 45f, baseY, h, 14f)
                drawBambooStalk(cx - 18f, baseY, h * 1.08f, 17f)
                drawBambooStalk(cx + 12f, baseY, h * 1.02f, 15f)
                drawBambooStalk(cx + 38f, baseY, h * 0.9f, 13f)
                drawBambooStalk(cx + 60f, baseY, h * 0.75f, 10f)
                drawBambooLeaves(cx - 45f, baseY - h, 40f)
                drawBambooLeaves(cx - 18f, baseY - h * 1.08f, 48f)
                drawBambooLeaves(cx + 12f, baseY - h * 1.02f, 42f)
                drawBambooLeaves(cx + 38f, baseY - h * 0.9f, 36f)
                drawBambooLeaves(cx + 60f, baseY - h * 0.75f, 28f)
                drawBambooFallingLeaves(cx, baseY - h, baseY, 18)
            }
        }
    }

    private fun DrawScope.drawBambooStalk(cx: Float, baseY: Float, h: Float, w: Float) {
        val segments = (h / 35f).toInt().coerceAtLeast(2)
        val segH = h / segments
        for (i in 0 until segments) {
            val y = baseY - (i + 1) * segH
            drawRoundRect(
                BAMBOO_GREEN,
                topLeft = Offset(cx - w / 2, y),
                size = Size(w, segH + 1f),
                cornerRadius = CornerRadius(w / 3f, w / 3f)
            )
            // Node ring
            drawRect(
                BAMBOO_GREEN.copy(alpha = 0.5f),
                topLeft = Offset(cx - w / 2 - 1f, y - 2f),
                size = Size(w + 2f, 3f)
            )
        }
        // Highlight
        drawRect(
            BAMBOO_LIGHT.copy(alpha = 0.5f),
            topLeft = Offset(cx - w / 2 + 2f, baseY - h),
            size = Size(w / 3f, h)
        )
    }

    private fun DrawScope.drawBambooLeaves(cx: Float, topY: Float, size: Float) {
        for (i in 0 until 6) {
            val a = (i * 60f - 60f) * 0.01745f
            val lx = cx + kotlin.math.cos(a.toDouble()).toFloat() * size * 0.5f
            val ly = topY + kotlin.math.sin(a.toDouble()).toFloat() * size * 0.3f
            val leafPath = Path().apply {
                moveTo(cx, topY - 2f)
                quadraticBezierTo(lx + size * 0.3f, ly - size * 0.2f, lx + size * 0.8f, ly)
                quadraticBezierTo(lx + size * 0.3f, ly + size * 0.1f, cx, topY + 2f)
            }
            drawPath(leafPath, GREEN_DARK)
        }
    }

    private fun DrawScope.drawBambooFallingLeaves(cx: Float, topY: Float, botY: Float, count: Int) {
        val rng = Random(123)
        repeat(count) {
            val lx = cx + (rng.nextFloat() - 0.5f) * 260f
            val ly = topY + rng.nextFloat() * (botY - topY)
            val s = 6f + rng.nextFloat() * 4f
            val leafPath = Path().apply {
                moveTo(lx, ly)
                quadraticBezierTo(lx + s, ly - s / 2f, lx + s * 1.5f, ly)
                quadraticBezierTo(lx + s, ly + s / 3f, lx, ly)
            }
            drawPath(leafPath, BAMBOO_GREEN)
        }
    }

    // ========== 9. MUSHROOM ==========
    private fun DrawScope.drawMushroom(cx: Float, baseY: Float, stage: Int, p: Float) {
        when (stage) {
            0 -> drawSeed(cx, baseY, MUSHROOM_RED)
            1 -> {
                val h = 25f * p
                drawRoundRect(MUSHROOM_WHITE, topLeft = Offset(cx - 5f, baseY - h), size = Size(10f, h), cornerRadius = CornerRadius(3f, 3f))
                drawOval(MUSHROOM_RED, topLeft = Offset(cx - 12f, baseY - h - 8f), size = Size(24f, 14f))
            }
            2 -> {
                val h = 40f * p + 15f
                // Stem
                drawRoundRect(MUSHROOM_WHITE, topLeft = Offset(cx - 8f, baseY - h), size = Size(16f, h), cornerRadius = CornerRadius(4f, 4f))
                // Cap
                drawMushroomCap(cx, baseY - h, 28f, 18f, MUSHROOM_RED)
                // White spots
                drawCircle(WHITE, radius = 4f, center = Offset(cx - 8f, baseY - h - 5f))
                drawCircle(WHITE, radius = 3f, center = Offset(cx + 6f, baseY - h - 8f))
            }
            3 -> {
                val h = 60f * p + 25f
                drawRoundRect(MUSHROOM_WHITE, topLeft = Offset(cx - 10f, baseY - h), size = Size(20f, h), cornerRadius = CornerRadius(5f, 5f))
                drawMushroomCap(cx, baseY - h, 40f, 25f, MUSHROOM_RED)
                // Smaller mushrooms
                val h2 = h * 0.55f
                drawRoundRect(MUSHROOM_WHITE, topLeft = Offset(cx - 35f, baseY - h2), size = Size(12f, h2), cornerRadius = CornerRadius(4f, 4f))
                drawMushroomCap(cx - 29f, baseY - h2, 20f, 12f, MUSHROOM_CAP_BROWN)
            }
            4 -> {
                val h = 80f * p + 45f
                drawRoundRect(MUSHROOM_WHITE, topLeft = Offset(cx - 12f, baseY - h), size = Size(24f, h), cornerRadius = CornerRadius(6f, 6f))
                drawMushroomCap(cx, baseY - h, 55f, 35f, MUSHROOM_RED)
                // Ring of mushrooms
                listOf(
                    Triple(-45f, h * 0.7f, PURPLE),
                    Triple(42f, h * 0.65f, MUSHROOM_CAP_BROWN),
                    Triple(-25f, h * 0.4f, ORANGE),
                    Triple(30f, h * 0.45f, YELLOW_DARK)
                ).forEach { (dx, mh, capColor) ->
                    val x = cx + dx
                    drawRoundRect(MUSHROOM_WHITE, topLeft = Offset(x - 7f, baseY - mh), size = Size(14f, mh), cornerRadius = CornerRadius(4f, 4f))
                    drawMushroomCap(x, baseY - mh, 18f, 12f, capColor)
                }
            }
            5 -> {
                val h = 90f * p + 65f
                drawRoundRect(MUSHROOM_WHITE, topLeft = Offset(cx - 14f, baseY - h), size = Size(28f, h), cornerRadius = CornerRadius(7f, 7f))
                drawMushroomCap(cx, baseY - h, 70f, 45f, RED)
                // Fairy ring of mushrooms
                val ringR = 75f
                val mushroomCount = 8
                val capColors = listOf(PURPLE, ORANGE, YELLOW_DARK, PINK, MUSHROOM_CAP_BROWN, RED, Color(0xFF4FC3F7), Color(0xFFAB47BC))
                for (i in 0 until mushroomCount) {
                    val a = (i * 360f / mushroomCount) * 0.01745f
                    val mx = cx + kotlin.math.cos(a.toDouble()).toFloat() * ringR
                    val my = baseY - h * 0.4f - kotlin.math.sin(a.toDouble()).toFloat().coerceAtLeast(-0.3f) * 15f
                    val mh = h * 0.35f + kotlin.math.sin(a.toDouble()).toFloat().coerceAtLeast(0f) * 10f
                    drawRoundRect(MUSHROOM_WHITE, topLeft = Offset(mx - 7f, my - mh), size = Size(14f, mh), cornerRadius = CornerRadius(4f, 4f))
                    drawMushroomCap(mx, my - mh, 20f, 14f, capColors[i])
                }
            }
            6 -> {
                val h = 110f * p + 85f
                // Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(PINK.copy(alpha = 0.5f), PURPLE.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(cx, baseY - h * 0.5f), radius = 220f
                    ), radius = 220f, center = Offset(cx, baseY - h * 0.5f)
                )
                drawRoundRect(MUSHROOM_WHITE, topLeft = Offset(cx - 18f, baseY - h), size = Size(36f, h), cornerRadius = CornerRadius(8f, 8f))
                drawMushroomCap(cx, baseY - h, 85f, 55f, RED)
                // Golden spots on big cap
                drawCircle(GOLD, radius = 5f, center = Offset(cx - 20f, baseY - h - 12f))
                drawCircle(GOLD, radius = 4f, center = Offset(cx + 15f, baseY - h - 25f))
                drawCircle(GOLD, radius = 6f, center = Offset(cx, baseY - h - 30f))
                drawCircle(WHITE, radius = 5f, center = Offset(cx - 10f, baseY - h - 20f))
                drawCircle(WHITE, radius = 4f, center = Offset(cx + 25f, baseY - h - 10f))
                // Fairy ring with magical mushrooms
                val ringR = 90f
                val capColors = listOf(PURPLE, ORANGE, YELLOW_DARK, PINK, MUSHROOM_CAP_BROWN, RED, Color(0xFF4FC3F7), Color(0xFFAB47BC), GOLD, CREAM)
                for (i in 0 until 10) {
                    val a = (i * 36f) * 0.01745f
                    val mx = cx + kotlin.math.cos(a.toDouble()).toFloat() * ringR
                    val my = baseY - h * 0.45f + kotlin.math.sin(a.toDouble()).toFloat().coerceAtLeast(-0.2f) * 20f
                    val mh = h * 0.38f
                    drawRoundRect(CREAM, topLeft = Offset(mx - 8f, my - mh), size = Size(16f, mh), cornerRadius = CornerRadius(4f, 4f))
                    drawMushroomCap(mx, my - mh, 22f, 15f, capColors[i])
                    // Glow on each mushroom
                    drawCircle(
                        capColors[i].copy(alpha = 0.3f),
                        radius = 18f,
                        center = Offset(mx, my - mh - 5f)
                    )
                }
                // Magic sparkles
                repeat(12) { i ->
                    val a = (i * 30f) * 0.01745f
                    val sr = ringR + 30f
                    drawCircle(
                        GOLD, radius = 2.5f,
                        center = Offset(cx + kotlin.math.cos(a.toDouble()).toFloat() * sr, baseY - h * 0.5f + kotlin.math.sin(a.toDouble()).toFloat() * sr * 0.5f)
                    )
                }
            }
        }
    }

    private fun DrawScope.drawMushroomCap(cx: Float, topOfStem: Float, w: Float, h: Float, color: Color) {
        val capPath = Path().apply {
            moveTo(cx - w / 2, topOfStem + 2f)
            quadraticBezierTo(cx - w / 2, topOfStem - h, cx, topOfStem - h - 2f)
            quadraticBezierTo(cx + w / 2, topOfStem - h, cx + w / 2, topOfStem + 2f)
            close()
        }
        drawPath(capPath, color)
        // Cap underside (gills)
        drawArc(
            Color(0xFFEFEBE9).copy(alpha = 0.5f),
            startAngle = 0f, sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - w / 2 + 2f, topOfStem - 3f),
            size = Size(w - 4f, 8f),
            style = Stroke(1f)
        )
    }
}
