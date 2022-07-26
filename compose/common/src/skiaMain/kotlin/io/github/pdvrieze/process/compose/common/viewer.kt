package io.github.pdvrieze.process.compose.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import io.github.pdvrieze.process.compose.common.canvas.ComposeCanvas
import io.github.pdvrieze.process.compose.common.canvas.ComposeStrategy
import io.github.pdvrieze.process.compose.common.canvas.ComposeTheme
import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.process.diagram.DrawableProcessModel

@Composable
fun Viewer(model: DrawableProcessModel) {
    Box(Modifier.fillMaxSize()
            .background(color = Color.Gray)
            .padding(10.dp)) {
        Text("Hello World!")
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(SolidColor(Color.Red), size = size)
            val b = model.builder()
            val cv = ComposeCanvas(this, ComposeTheme(ComposeStrategy.INSTANCE))
            b.layout()
            b.draw(cv, Rectangle(0.0, 0.0, size.width.toDouble(), size.height.toDouble()))
        }
    }
}
