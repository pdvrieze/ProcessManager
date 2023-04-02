package nl.adaptivity.process.engine.pma.dynamic

import nl.adaptivity.process.engine.pma.AuthToken
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAActivityContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAProcessInstanceContext
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.runtime.PMAActivityContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.util.multiplatform.PrincipalCompat

open class ServiceActivityContext<AIC : DynamicPMAActivityContext<AIC, *>, S : AutomatedService>(
    protected val activityContext: AIC,
    val service: S,
    val authToken: AuthToken
) : PMAActivityContext<AIC> {
    override fun canBeAssignedTo(principal: PrincipalCompat?): Boolean = activityContext.canBeAssignedTo(principal)

    override val processNode: IProcessNodeInstance get() = activityContext.processNode

    override val processContext: DynamicPMAProcessInstanceContext<AIC> get() = activityContext.processContext


}
