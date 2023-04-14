@file:OptIn(ExperimentalContracts::class)

package nl.adaptivity.process.engine.pma.dynamic

import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaProcessInstanceContext
import nl.adaptivity.process.engine.pma.dynamic.services.RunnableUiService
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.engine.pma.models.UiService
import nl.adaptivity.util.multiplatform.PrincipalCompat
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class TaskBuilderContext<AIC : DynamicPmaActivityContext<AIC, BIC>, BIC : TaskBuilderContext.BrowserContext<AIC, BIC>, I>() {
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
        return acceptTask({principal}, action)
    }

    fun <O> acceptTask(principalProvider: AIC.() -> PrincipalCompat, action: BIC.(I) -> O) : AcceptedTask<AIC, BIC, I, O> {
        return AcceptedTask(principalProvider, action)
    }

    class AcceptedTask<AIC : DynamicPmaActivityContext<AIC, BIC>, BIC : BrowserContext<AIC, BIC>, in I, out O>(
        val principalProvider: AIC.() -> PrincipalCompat,
        private val action: BIC.(I) -> O
    ) {
        operator fun invoke(activityContext: AIC, input: I): O {
            val principal = (activityContext as AIC).principalProvider()
            val browser = activityContext.resolveBrowser(principal)
            val context: BIC = activityContext.browserContext(browser)

            val processContext: DynamicPmaProcessInstanceContext<*> = activityContext.processContext
            val taskListService = processContext.contextFactory.getOrCreateTaskListForUser(principal)

            context.uiServiceLogin(taskListService) {
                service.acceptActivity(authToken, browser.user, emptyList(), context.nodeInstanceHandle)
            }

            return context.action(input)
        }
    }

    interface BrowserContext<out AIC: DynamicPmaActivityContext<AIC, BIC>, out BIC: BrowserContext<AIC, BIC>> :
        DynamicPmaActivityContext<AIC, BIC> {
        val browser: Browser

    }

    interface UIServiceInnerContext<S: UiService> {
        val authToken: PmaAuthToken
        val service: S
    }
}

fun <S : RunnableUiService, R> TaskBuilderContext.BrowserContext<*, *>.uiServiceLogin(service: S, action: TaskBuilderContext.UIServiceInnerContext<S>.() -> R) : R {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    val authToken: PmaAuthToken = browser.loginToService(service)

    return DefaultUIServiceInnerContext(authToken, service).action()
}

fun <AIC : DynamicPmaActivityContext<AIC, BIC>, BIC : TaskBuilderContext.BrowserContext<AIC, BIC>, S : RunnableUiService, R> TaskBuilderContext.BrowserContext<AIC, BIC>.uiServiceLogin(
    service: ServiceName<S>,
    action: TaskBuilderContext.UIServiceInnerContext<S>.() -> R
): R {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    val serviceInst: S = processContext.contextFactory.serviceResolver.resolveService(service)
    val authToken: PmaAuthToken = browser.loginToService(serviceInst)

    return DefaultUIServiceInnerContext(authToken, serviceInst).action()
}

private class DefaultUIServiceInnerContext<S: UiService>(
    override val authToken: PmaAuthToken,
    override val service: S
) : TaskBuilderContext.UIServiceInnerContext<S>

