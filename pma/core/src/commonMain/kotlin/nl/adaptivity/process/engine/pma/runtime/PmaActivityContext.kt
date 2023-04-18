package nl.adaptivity.process.engine.pma.runtime

import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.processModel.engine.ExecutableActivity
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface PmaActivityContext<out AIC : PmaActivityContext<AIC>> : ActivityInstanceContext {

    val activityInstance: IProcessNodeInstance

    override val processContext: PmaProcessInstanceContext<AIC>

    override val node: ExecutableActivity get() = activityInstance.node as ExecutableActivity

    override val state: NodeInstanceState get() = activityInstance.state

    override val nodeInstanceHandle: PNIHandle
        get() = activityInstance.handle

    override val assignedUser: PrincipalCompat?
        get() = activityInstance.assignedUser

    override val owner: PrincipalCompat
        get() = (activityInstance as SecureObject<*>).owner

}
