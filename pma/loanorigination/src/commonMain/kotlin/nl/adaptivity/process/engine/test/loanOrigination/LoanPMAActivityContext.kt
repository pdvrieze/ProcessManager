package nl.adaptivity.process.engine.test.loanOrigination

import nl.adaptivity.process.engine.ProcessEnginePermissions
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAActivityContext
import nl.adaptivity.process.engine.pma.runtime.PMAActivityInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat

class LoanPMAActivityContext(
    override val processContext: LoanPmaProcessContext,
    processNode: PMAActivityInstance<*>
) : DynamicPMAActivityContext<Any, Any, LoanPMAActivityContext, LoanBrowserContext>(processNode) {
    @Suppress("UNCHECKED_CAST")
    override val processNode: PMAActivityInstance<LoanPMAActivityContext>
        get() = super.processNode as PMAActivityInstance<LoanPMAActivityContext>

    override fun browserContext(): LoanBrowserContext {
        return LoanBrowserContext(this)
    }

    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean {
        val restrictions = node.accessRestrictions ?: return true

        return principal != null &&
            restrictions.hasAccess(this, principal, ProcessEnginePermissions.ASSIGNED_TO_ACTIVITY)
    }

}
