package nl.adaptivity.process.engine.pma.dynamic

import nl.adaptivity.process.engine.pma.AuthToken
import nl.adaptivity.process.engine.pma.dynamic.services.TaskList
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaProcessInstanceContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableUiService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.UiService
import nl.adaptivity.util.multiplatform.PrincipalCompat

open class TaskBuilderContext<AIC : AbstractDynamicPmaActivityContext<AIC, BIC>, BIC : TaskBuilderContext.BrowserContext<AIC, BIC>, I>() {

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

    class AcceptedTask<AIC : AbstractDynamicPmaActivityContext<AIC, BIC>, BIC : BrowserContext<AIC, BIC>, I, O>(
        val principal: PrincipalCompat,
        private val action: BIC.(I) -> O
    ) {
        operator fun invoke(activityContext: AIC, input: I): O {
            val browser = activityContext.resolveBrowser(principal)
            val context = activityContext.browserContext(browser)

            val processContext: DynamicPmaProcessInstanceContext<AIC> = activityContext.processContext
            val taskListService = processContext.contextFactory.getOrCreateTaskListForUser(principal) as TaskList

            context.uiServiceLogin(taskListService) {
                service.acceptActivity(authToken, browser.user, emptyList(), context.nodeInstanceHandle)
            }

/*
            val authorizationCode = taskListService.acceptActivity(
                browser.loginToService(taskListService),
                browser.user,
                emptyList(),
                activityContext.nodeInstanceHandle
            )
            browser.addToken(processContext.authService, authorizationCode)
*/

            return context.action(input)
        }
    }

    interface BrowserContext<AIC: AbstractDynamicPmaActivityContext<AIC, BIC>, BIC: BrowserContext<AIC, BIC>> :
        DynamicPmaActivityContext<AIC, BIC> {
        fun <S: RunnableUiService, R> uiServiceLogin(service: ServiceName<S>, action: UIServiceInnerContext<S>.() -> R) : R
        fun <S: RunnableUiService, R> uiServiceLogin(service: S, action: UIServiceInnerContext<S>.() -> R) : R
    }

    interface UIServiceInnerContext<S: UiService> {
        val authToken: AuthToken
        val service: S
    }
}

