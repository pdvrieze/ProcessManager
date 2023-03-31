package nl.adaptivity.process.engine.pma.dynamic

import nl.adaptivity.process.engine.pma.AuthToken
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.UIService
import nl.adaptivity.process.engine.pma.runtime.PMAActivityContext
import nl.adaptivity.process.engine.pma.runtime.PMAProcessInstanceContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat

abstract class BrowserActivityContext<AIC: PMAActivityContext<AIC>>(protected val activityContext: AIC): PMAActivityContext<AIC>() {
    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean = activityContext.canBeAssignedTo(principal)

    override val processNode: IProcessNodeInstance get() = activityContext.processNode

    override val processContext: PMAProcessInstanceContext<AIC> get() = activityContext.processContext

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

    abstract fun <R> acceptTask(browser: Browser, action: BrowserInnerContext.() -> R): R

    interface BrowserInnerContext {
        fun <S: UIService, R> uiServiceLogin(service: ServiceId<S>, action: UIServiceInnerContext<S>.() -> R) : R
    }

    interface UIServiceInnerContext<S: UIService> {
        val authToken: AuthToken
        val service: S
    }
}

