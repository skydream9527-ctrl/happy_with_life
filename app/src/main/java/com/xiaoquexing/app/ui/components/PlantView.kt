package com.xiaoquexing.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaoquexing.app.data.entity.PlantStage
import com.xiaoquexing.app.data.entity.PlantType
import com.xiaoquexing.app.util.PlantRenderer

@Composable
fun PlantView(
    plantType: PlantType,
    stage: PlantStage,
    gp: Int,
    modifier: Modifier = Modifier,
    showPot: Boolean = true
) {
    val progress = PlantStage.progressInStage(gp)
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            PlantRenderer.drawPlant(
                drawScope = this,
                plantType = plantType,
                stage = stage,
                progressInStage = progress,
                showPot = showPot
            )
        }
    }
}

@Composable
fun PlantViewSmall(
    plantType: PlantType,
    stage: PlantStage,
    gp: Int,
    size: Dp = 80.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        PlantRenderer.drawPlant(
            drawScope = this,
            plantType = plantType,
            stage = stage,
            progressInStage = PlantStage.progressInStage(gp),
            showPot = true
        )
    }
}
