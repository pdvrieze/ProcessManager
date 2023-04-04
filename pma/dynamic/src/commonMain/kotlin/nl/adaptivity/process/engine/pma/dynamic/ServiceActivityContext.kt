package nl.adaptivity.process.engine.pma.dynamic

import nl.adaptivity.process.engine.pma.AuthToken
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaProcessInstanceContext
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.runtime.PmaActivityContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat

open class ServiceActivityContext<AIC : AbstractDynamicPmaActivityContext<AIC, *>, S : AutomatedService>(
    protected val activityContext: AIC,
    val service: S,
    val authToken: AuthToken
) : PmaActivityContext<AIC> {
    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean = activityContext.canBeAssignedTo(principal)

    override val processNode: IProcessNodeInstance get() = activityContext.processNode

    override val processContext: DynamicPmaProcessInstanceContext<AIC> get() = activityContext.processContext


}
