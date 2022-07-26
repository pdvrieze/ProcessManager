// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import io.github.pdvrieze.process.compose.common.canvas.JsCanvas
import io.github.pdvrieze.process.compose.common.canvas.JsCanvasStrategy
import io.github.pdvrieze.process.compose.common.canvas.JsCanvasTheme
import kotlinx.dom.clear
import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.process.diagram.DrawableProcessModel
import nl.adaptivity.process.diagram.DrawableProcessNode
import nl.adaptivity.process.diagram.RootDrawableProcessModel
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.configurableModel.ConfigurableProcessModel
import nl.adaptivity.process.processModel.configurableModel.activity
import nl.adaptivity.process.processModel.configurableModel.endNode
import nl.adaptivity.process.processModel.configurableModel.startNode
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal
import org.jetbrains.compose.web.attributes.height
import org.jetbrains.compose.web.attributes.width
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.CanvasRenderingContext2D


private class TestModel(owner: Principal): ConfigurableProcessModel<DrawableProcessNode>(
    name = "PreviewModel",
    owner = owner,
    uuid = UUID("1")
) {
    val start by startNode
    val ac1 by activity(start) { x = 40.0 }
    val end by endNode(ac1) { x = 80.0 }
}

@Composable
fun ViewerPreview() {
    val owner = SYSTEMPRINCIPAL
    val builder: RootProcessModel.Builder = TestModel(owner).configurationBuilder
    val model = RootDrawableProcessModel(builder)

    Viewer(model)
}

@Composable
fun Viewer(model: DrawableProcessModel) {
    Canvas(attrs = {
        width(800)
        height(600)
    }) {
        DisposableEffect(model) {
            val elem = scopeElement
            val ctx = elem.getContext("2d") as CanvasRenderingContext2D
            ctx.fillStyle = "green"
            ctx.fillRect(0.0, 0.0, 300.0, 200.0)

            val b = model.builder()
            val cv = JsCanvas(scopeElement, JsCanvasTheme(JsCanvasStrategy(ctx)))
            b.layout()
            b.draw(cv, Rectangle(0.0, 0.0, 800.0, 600.0))


            onDispose { elem.clear() }

        }
    }
}


fun main() {
    val owner = SYSTEMPRINCIPAL
    val builder: RootProcessModel.Builder = TestModel(owner).configurationBuilder
    val model = RootDrawableProcessModel(builder)

    renderComposable(rootElementId = "root") {
        Div {
            Text("Foo")
        }
        Div({ style { padding(25.px) } }) {

            Viewer(model)
        }

    }
}

/*
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Compose for ${getPlatformName()}",
        state = rememberWindowState(width = 300.dp, height = 200.dp)
    ) {
        App()
    }
}
*/
