package nl.adaptivity.process.engine.test.loanOrigination

import nl.adaptivity.process.engine.pma.AuthToken
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.BrowserActivityContext
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.UIService
import nl.adaptivity.process.engine.test.loanOrigination.datatypes.CustomerData

class LoanBrowserContext(activityContext: LoanPMAActivityContext) :
    BrowserActivityContext<LoanPMAActivityContext>(activityContext) {
    override val processContext: LoanPmaProcessContext
        get() = activityContext.processContext

    val data = object : Data {
        override val customerData get() = this@LoanBrowserContext.activityContext.processContext.customerData

    }

    override fun <R> acceptTask(browser: Browser, action: BrowserInnerContext.() -> R): R {
        val taskListService = processContext.taskListFor(browser.user)

        val taskListToken = taskListService.acceptActivity(
            browser.loginToService(taskListService),
            browser.user,
            ArrayDeque(),
            nodeInstanceHandle
        )
        browser.addToken(processContext.authService, taskListToken)
        val context: BrowserInnerContext = InnerContext()
        return context.action()
    }

    interface Data {
        val customerData: CustomerData
    }

    class InnerContext: BrowserInnerContext {
        override fun <S : UIService, R> uiServiceLogin(
            service: ServiceId<S>,
            action: UIServiceInnerContext<S>.() -> R
        ): R {
            TODO("not implemented")
        }
    }

    class LoanUIServiceContext<S: UIService>(override val authToken: AuthToken, override val service: S): UIServiceInnerContext<S>
}
