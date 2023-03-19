package nl.adaptivity.process.engine.pma.runtime

import net.devrieze.util.Handle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.ProcessContextFactory
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.pma.models.PMAMessageActivity
import nl.adaptivity.process.engine.pma.models.TaskListService
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.multiplatform.PrincipalCompat
import java.security.Principal

abstract class PMAProcessContextFactory<AIC : PMAActivityContext<AIC>>: ProcessContextFactory<AIC> {
    abstract val authServiceClient: AuthServiceClient

    abstract fun getOrCreateTaskListForUser(principal: Principal): TaskListService

    override fun createNodeInstance(
        node: ExecutableProcessNode,
        predecessors: List<Handle<SecureObject<ProcessNodeInstance<*, AIC>>>>,
        processInstanceBuilder: ProcessInstance.Builder<AIC>,
        owner: PrincipalCompat,
        entryNo: Int,
        assignedUser: PrincipalCompat?,
        handle: Handle<SecureObject<ProcessNodeInstance<*, AIC>>>,
        state: NodeInstanceState
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, *, AIC> = when (node){
        is PMAMessageActivity<*> ->
            @Suppress("UNCHECKED_CAST")
            PMAActivityInstance.BaseBuilder(
                node = node as PMAMessageActivity<AIC>,
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
