package nl.adaptivity.process.engine.pma.dynamic

import nl.adaptivity.process.engine.pma.AuthToken
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.IDynamicPMAActivityContext
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.UIService
import nl.adaptivity.util.multiplatform.PrincipalCompat

open class TaskBuilderContext<AIC : DynamicPMAActivityContext<AIC, BIC>, BIC : TaskBuilderContext.BrowserContext<AIC, BIC>, I>() {

    /*
        open fun ensureTaskList(browser: Browser) : TaskListService {
            val taskUser = browser.user
            if (::taskListService.isInitialized) {
                if (! taskListService.servesFor(taskUser)) {
                    throw UnsupportedOperationException("Attempting to change the user for an activity after it has already been set")
                }
            } else {
                taskListService = processContext.contextFactory.getOrCreateTaskListForUser(taskUser)
            }
            return taskListService
        }
    */

    fun <O> acceptTask(principal: PrincipalCompat, action: BIC.(I) -> O) : AcceptedTask<AIC, BIC, I, O> {
        return AcceptedTask(principal, action)
    }

    class AcceptedTask<AIC : DynamicPMAActivityContext<AIC, BIC>, BIC : BrowserContext<AIC, BIC>, I, O>(
        val principal: PrincipalCompat,
        private val action: BIC.(I) -> O
    ) {
        operator fun invoke(activityContext: AIC, input: I): O {
            val browser = activityContext.resolveBrowser(principal)
            val context = activityContext.browserContext(browser)
            return context.action(input)
        }
    }

    interface BrowserContext<AIC: DynamicPMAActivityContext<AIC, BIC>, BIC: BrowserContext<AIC, BIC>> :
        IDynamicPMAActivityContext<AIC, BIC> {
        fun <S: RunnableUIService, R> uiServiceLogin(service: ServiceName<S>, action: UIServiceInnerContext<S>.() -> R) : R
    }

    interface UIServiceInnerContext<S: UIService> {
        val authToken: AuthToken
        val service: S
    }
}

