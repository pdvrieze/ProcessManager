package nl.adaptivity.process.engine.test.loanOrigination

import nl.adaptivity.process.engine.pma.AuthToken
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.RunnableUIService
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.UIService
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData
import nl.adaptivity.util.multiplatform.PrincipalCompat

class LoanBrowserContext(private val delegateContext: LoanPMAActivityContext, private val browser: Browser):
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

    override val processNode: IProcessNodeInstance
        get() = delegateContext.processNode

    override fun <S : RunnableUIService, R> uiServiceLogin(
        serviceId: ServiceName<S>,
        action: TaskBuilderContext.UIServiceInnerContext<S>.() -> R
    ): R {
        val serviceInst: S = processContext.contextFactory.resolveService(serviceId)
        val authToken : AuthToken = browser.loginToService(serviceInst)

        val context = LoanUIServiceContext(authToken, serviceInst)
        return context.action()
    }

    interface Data {
        val customerData: CustomerData
    }

    class LoanUIServiceContext<S: UIService>(override val authToken: AuthToken, override val service: S):
        TaskBuilderContext.UIServiceInnerContext<S>
}
