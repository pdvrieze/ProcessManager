package io.github.pdvrieze.pma.agfil.contexts

import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat

class AgfilBrowserContext(
    private val delegateContext: AgfilActivityContext,
    override val browser: Browser,
) : TaskBuilderContext.BrowserContext<AgfilActivityContext, AgfilBrowserContext>, AgfilActivityContext {

    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean = delegateContext.canBeAssignedTo(principal)

    override val activityInstance: IProcessNodeInstance get() = delegateContext.activityInstance

    override val processContext: AgfilProcessContext get() = delegateContext.processContext

    override fun resolveBrowser(principal: PrincipalCompat): Browser = delegateContext.resolveBrowser(principal)

    override fun randomEaCallHandler(): PrincipalCompat = delegateContext.randomEaCallHandler()

    override fun randomAccidentDetails(): String = delegateContext.randomAccidentDetails()

    override fun browserContext(browser: Browser): AgfilBrowserContext = when (browser) {
        this.browser -> this
        else -> delegateContext.browserContext(browser)
    }
}
