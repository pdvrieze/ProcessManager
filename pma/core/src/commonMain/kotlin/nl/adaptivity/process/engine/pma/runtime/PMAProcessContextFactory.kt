package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.IPMAMessageActivity
import nl.adaptivity.process.engine.pma.models.ResolvedInvokableMethod
import nl.adaptivity.process.engine.pma.models.TaskListService
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.processModel.AccessRestriction
import nl.adaptivity.process.processModel.AuthorizationInfo
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal

interface PMAProcessContextFactory<out AIC : PmaActivityContext<AIC>>: ProcessContextFactory<AIC> {
    val adminAuthServiceClient: AuthServiceClient<*, *, *>

    fun getOrCreateTaskListForUser(principal: Principal): TaskListService

    fun getOrCreateTaskListForRestrictions(accessRestrictions: AccessRestriction?): List<TaskListService>

    override fun createNodeInstance(
        node: ExecutableProcessNode,
        predecessors: List<PNIHandle>,
        processInstanceBuilder: ProcessInstance.Builder,
        owner: PrincipalCompat,
        entryNo: Int,
        assignedUser: PrincipalCompat?,
        handle: PNIHandle,
        state: NodeInstanceState
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, *> = when (node){
        is IPMAMessageActivity<*> ->
            PMAActivityInstance.BaseBuilder<AIC>(
                node = node,
                predecessor = predecessors.single(),
                processInstanceBuilder = processInstanceBuilder,
                owner = owner,
                entryNo = entryNo,
                assignedUser = assignedUser,
                handle = handle,
                state = state
            )

        else -> super.createNodeInstance(
            node = node,
            predecessors = predecessors,
            processInstanceBuilder = processInstanceBuilder,
            owner = owner,
            entryNo = entryNo,
            assignedUser = assignedUser,
            handle = handle,
            state = state
        )
    }

    fun resolveService(targetService: InvokableMethod): ResolvedInvokableMethod?
    fun createAuthTokenForEngineToInvokeService(
        targetService: InvokableMethod,
        authorizations: List<AuthScope>,
        pniHandle: PNIHandle
    ): AuthorizationInfo.Token {
        val resolvedService = requireNotNull(resolveService(targetService)) { "Service $targetService could not be resolved" }
        return adminAuthServiceClient.requestAuthToken(resolvedService, authorizations, pniHandle)
    }
}
