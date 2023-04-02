package nl.adaptivity.process.engine.test.loanOrigination

import nl.adaptivity.process.engine.ProcessEnginePermissions
import nl.adaptivity.process.engine.pma.Browser
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAActivityContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.processModel.MessageActivity
import nl.adaptivity.util.multiplatform.PrincipalCompat

class LoanPMAActivityContext(
    override val processContext: LoanPmaProcessContext,
    processNode: IProcessNodeInstance
) : DynamicPMAActivityContext<LoanPMAActivityContext, LoanBrowserContext>(processNode) {
    /*
    @Suppress("UNCHECKED_CAST")
    override val processNode: PMAActivityInstance<LoanPMAActivityContext>
        get() = super.processNode as PMAActivityInstance<LoanPMAActivityContext>
*/
    override fun browserContext(browser: Browser): LoanBrowserContext {
        return LoanBrowserContext(this, browser)
    }

    override fun resolveBrowser(principal: PrincipalCompat): Browser {
        return processContext.resolveBrowser(principal)
    }

    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean {
        val restrictions = (node as? MessageActivity)?.accessRestrictions ?: return true

        return principal != null &&
            restrictions.hasAccess(this, principal, ProcessEnginePermissions.ASSIGNED_TO_ACTIVITY)
    }

}
