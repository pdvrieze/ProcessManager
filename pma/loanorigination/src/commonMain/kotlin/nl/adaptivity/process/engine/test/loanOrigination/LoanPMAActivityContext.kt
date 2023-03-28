package nl.adaptivity.process.engine.test.loanOrigination

import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableActivity
import nl.adaptivity.process.engine.ProcessEnginePermissions
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAActivityContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat

class LoanPMAActivityContext(
    override val processContext: LoanPmaProcessContext,
    processNode: IProcessNodeInstance
) : DynamicPMAActivityContext<Any, Any, LoanPMAActivityContext>(processNode) {

    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean {

        val restrictions = (node as? RunnableActivity<*, *, *>)
            ?.accessRestrictions ?: return true
        return principal != null && restrictions.hasAccess(this, principal,
            ProcessEnginePermissions.ASSIGNED_TO_ACTIVITY
        )
    }

}
