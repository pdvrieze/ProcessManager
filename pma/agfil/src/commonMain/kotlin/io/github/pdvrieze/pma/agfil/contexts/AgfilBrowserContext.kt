package io.github.pdvrieze.pma.agfil.contexts

import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext

class AgfilBrowserContext(
    private val delegateContext: AgfilActivityContext,
    override val browser: Browser,
) : TaskBuilderContext.BrowserContext<AgfilActivityContext, AgfilBrowserContext>, AgfilActivityContext by delegateContext {

    override fun browserContext(browser: Browser): AgfilBrowserContext = when (browser) {
        this.browser -> this
        else -> delegateContext.browserContext(browser)
    }
}
