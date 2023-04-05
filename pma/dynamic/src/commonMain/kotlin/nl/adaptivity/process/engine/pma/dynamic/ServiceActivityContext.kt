package nl.adaptivity.process.engine.pma.dynamic

import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaProcessInstanceContext
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.runtime.PmaActivityContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat

open class ServiceActivityContext<AIC : DynamicPmaActivityContext<AIC, *>, S : AutomatedService>(
    protected val activityContext: AIC,
    val service: S,
    val authToken: PmaAuthToken
) : PmaActivityContext<AIC> {
    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean = activityContext.canBeAssignedTo(principal)

    override val activityInstance: IProcessNodeInstance get() = activityContext.activityInstance

    override val processContext: DynamicPmaProcessInstanceContext<AIC> get() = activityContext.processContext


}
