import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.pdvrieze.process.compose.common.impl.ViewerPreview

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Model Viewer test",
        state = rememberWindowState(width = 300.dp, height = 200.dp)
    ) {
        ViewerPreview()
    }
}
