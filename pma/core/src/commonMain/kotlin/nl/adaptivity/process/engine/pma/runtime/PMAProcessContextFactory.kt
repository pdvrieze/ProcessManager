package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.pma.models.PMAMessageActivity
import nl.adaptivity.process.engine.pma.models.TaskListService
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal

interface PMAProcessContextFactory<AIC : PMAActivityContext<AIC>>: ProcessContextFactory<AIC> {
    val authServiceClient: AuthServiceClient

    fun getOrCreateTaskListForUser(principal: Principal): TaskListService

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
        is PMAMessageActivity<*> ->
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
}
