package nl.adaptivity.process.engine.test.loanOrigination

import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.util.multiplatform.PrincipalCompat

class LoanBrowserContext(private val delegateContext: LoanPMAActivityContext, override val browser: Browser):
    TaskBuilderContext.BrowserContext<LoanPMAActivityContext, LoanBrowserContext> {

    override val processContext: LoanPmaProcessContext
        get() = delegateContext.processContext

    val data = object : Data {
        override val customerData get() = processContext.customerData
    }

    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean = delegateContext.canBeAssignedTo(principal)

    override fun resolveBrowser(principal: PrincipalCompat): Browser {
        return delegateContext.resolveBrowser(principal)
    }

    override fun browserContext(browser: Browser): LoanBrowserContext = when (browser) {
        this.browser -> this
        else -> delegateContext.browserContext(browser)
    }

    override val activityInstance: IProcessNodeInstance
        get() = delegateContext.activityInstance

    interface Data {
        val customerData: CustomerData
    }
}
