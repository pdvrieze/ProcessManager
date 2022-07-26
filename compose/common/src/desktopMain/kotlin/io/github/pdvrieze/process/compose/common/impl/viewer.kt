package io.github.pdvrieze.process.compose.common.impl

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import io.github.pdvrieze.process.compose.common.Viewer
import nl.adaptivity.process.diagram.DrawableProcessNode
import nl.adaptivity.process.processModel.configurableModel.ConfigurableProcessModel
import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.process.diagram.RootDrawableProcessModel
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.configurableModel.activity
import nl.adaptivity.process.processModel.configurableModel.endNode
import nl.adaptivity.process.processModel.configurableModel.startNode
import java.security.Principal
import nl.adaptivity.util.multiplatform.UUID


private class TestModel(owner: Principal): ConfigurableProcessModel<DrawableProcessNode>(
    name = "PreviewModel",
    owner = owner,
    uuid = UUID.randomUUID()
) {
    val start by startNode
    val ac1 by activity(start) { x = 40.0 }
    val end by endNode(ac1) { x = 80.0 }
}

@Preview
@Composable
fun ViewerPreview() {
    val owner = SYSTEMPRINCIPAL
    val builder: RootProcessModel.Builder = TestModel(owner).configurationBuilder
    val model = RootDrawableProcessModel(builder)

    Viewer(model)
}
