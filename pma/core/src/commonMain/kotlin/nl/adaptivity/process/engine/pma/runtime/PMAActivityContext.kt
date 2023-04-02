package nl.adaptivity.process.engine.pma.runtime

import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.processModel.TokenServiceAuthData
import nl.adaptivity.process.processModel.engine.ExecutableActivity
import nl.adaptivity.util.multiplatform.PrincipalCompat

interface PMAActivityContext<AIC : PMAActivityContext<AIC>> : ActivityInstanceContext {

    val processNode: IProcessNodeInstance

    override val processContext: PMAProcessInstanceContext<AIC>

    override val node: ExecutableActivity get() = processNode.node as ExecutableActivity

    override val state: NodeInstanceState get() = processNode.state

    override val nodeInstanceHandle: PNIHandle
        get() = processNode.handle

    override val assignedUser: PrincipalCompat?
        get() = processNode.assignedUser

    override val owner: PrincipalCompat
        get() = (processNode as SecureObject<*>).owner

    fun <MSG_T> requestAuthData(
        messageService: IMessageService<MSG_T>,
        targetService: InvokableMethod,
        authorizations: List<AuthScope>
    ): TokenServiceAuthData {
        return processContext.requestAuthData(messageService, targetService, authorizations)
    }

}
