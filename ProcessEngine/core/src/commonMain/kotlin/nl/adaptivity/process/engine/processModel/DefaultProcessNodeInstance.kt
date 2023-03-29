/*
 * Copyright (c) 2019.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.Handle
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SecurityProvider
import net.devrieze.util.security.SecurityProvider.IntermediatePermissionDecision
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.MessageSendingResult
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.impl.getClass
import nl.adaptivity.process.processModel.MessageActivity
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.net.devrieze.util.security.ActiveSecureObject
import nl.adaptivity.xmlutil.XmlDeserializerFactory
import nl.adaptivity.xmlutil.XmlReader

/**
 * Class to represent the instanciation of a node. Subclasses may add behaviour.
 */
class DefaultProcessNodeInstance :
    ProcessNodeInstance<DefaultProcessNodeInstance>,
    ActiveSecureObject<DefaultProcessNodeInstance> {

    /**
     * @param node The node that this is an instance of.
     * @param predecessors The node instances that are direct predecessors of this one
     * @param hProcessInstance The handle to the owning process instance.
     * @param owner The owner of the node (generally the owner of the instance)
     * @param handle The handle for this instance (or invalid if not registered yet)
     * @param state The current state of the instance
     * @param results A list of the results associated with this node. This would imply a state of [NodeInstanceState.Complete]
     * @param entryNo The sequence number of this instance. Normally this will be 1, but for nodes that allow reentry,
     *                   this may be a higher number. Values below 1 are invalid.
     * @param failureCause For a failure, the cause of the failure
     */
    constructor(
        node: ExecutableProcessNode,
        predecessors: Collection<PNIHandle>,
        processInstanceBuilder: ProcessInstance.Builder,
        hProcessInstance: PIHandle,
        owner: PrincipalCompat,
        entryNo: Int,
        assignedUser: PrincipalCompat?,
        handle: PNIHandle = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending,
        results: Iterable<ProcessData> = emptyList(),
        failureCause: Throwable? = null
    ) : super(
        node,
        predecessors,
        processInstanceBuilder,
        hProcessInstance,
        owner,
        entryNo,
        handle,
        state,
        results,
        failureCause
    ) {
        this.assignedUser = assignedUser
    }

    constructor(
        node: ExecutableProcessNode,
        predecessor: PNIHandle,
        processInstance: ProcessInstance,
        entryNo: Int,
        assignedUser: PrincipalCompat?
    ) : this(
        node,
        if (predecessor.isValid) listOf(predecessor) else emptyList(),
        processInstance.builder(),
        processInstance.handle,
        processInstance.owner,
        entryNo = entryNo,
        assignedUser = assignedUser
    )

    constructor(builder: Builder) : super(builder) {
        assignedUser = builder.assignedUser
    }

    override val assignedUser: PrincipalCompat?

    override fun withPermission() = this

    override fun hasPermission(
        subject: PrincipalCompat,
        permission: SecurityProvider.Permission
    ): IntermediatePermissionDecision {
        if (node !is MessageActivity) return IntermediatePermissionDecision.PASS
        val ar = node.accessRestrictions ?: return IntermediatePermissionDecision.PASS
        return when {
            ar.hasAccess(this, subject, permission) ->
                IntermediatePermissionDecision.ALLOW

            else ->
                IntermediatePermissionDecision.DENY
        }
    }

    override fun builder(processInstanceBuilder: ProcessInstance.Builder):
        ExtBuilder<ExecutableProcessNode, DefaultProcessNodeInstance> {

        return ExtBuilderImpl(this, processInstanceBuilder)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.getClass() != getClass()) return false

        other as DefaultProcessNodeInstance

        if (hProcessInstance != other.hProcessInstance) return false
        if (state != other.state) return false
        if (failureCause != other.failureCause) return false
        if (node != other.node) return false
        if (handle != other.handle) return false
        if (results != other.results) return false
        if (predecessors != other.predecessors) return false
        if (owner != other.owner) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hProcessInstance.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + (failureCause?.hashCode() ?: 0)
        result = 31 * result + node.hashCode()
        result = 31 * result + handle.hashCode()
        result = 31 * result + results.hashCode()
        result = 31 * result + predecessors.hashCode()
        result = 31 * result + owner.hashCode()
        return result
    }

    interface Builder :
        ProcessNodeInstance.Builder<ExecutableProcessNode, DefaultProcessNodeInstance> {

        override var assignedUser: PrincipalCompat?

        override fun doProvideTask(
            engineData: MutableProcessEngineDataAccess<*>,
            messageService: IMessageService<*>
        ): Boolean {
            // Create a local copy to prevent races - and shut up Kotlin about the possibilities as it should be immutable
            val node = this.node

            val shouldProgress = tryCreateTask { node.canProvideTaskAutoProgress(engineData, this) }

            return node.visit(object : ProcessNode.Visitor<Boolean> {
                override fun visitGeneralNode(node: ProcessNode): Boolean {
                    return shouldProgress
                }

                /**
                 * Helper function that captures the type of the message service.
                 */
                private fun <MSG_T> createAndSendMessage(
                    messageService: IMessageService<MSG_T>,
                    messageActivity: MessageActivity
                ): MessageSendingResult {
                    val preparedMessage = messageService.createMessage(messageActivity.message ?: XmlMessage())
                    return tryCreateTask {
                        val aic = createActivityContext(engineData)
                        messageService.sendMessage(engineData, preparedMessage, aic)// TODO remove cast
                    }
                }

                override fun visitActivity(messageActivity: MessageActivity): Boolean {
                    val sendingResult: MessageSendingResult = createAndSendMessage(messageService, messageActivity)
                    when (sendingResult) {
                        MessageSendingResult.SENT -> state = NodeInstanceState.Sent
                        MessageSendingResult.ACKNOWLEDGED -> state = NodeInstanceState.Acknowledged
                        MessageSendingResult.FAILED -> failTaskCreation(ProcessException("Failure to send message"))
                    }
                    store(engineData)
                    return false
                }
            })
        }

        override fun canTakeTaskAutomatically(): Boolean {
            val n = node
            return n !is MessageActivity || n.accessRestrictions == null
        }

        override fun doTakeTask(
            engineData: MutableProcessEngineDataAccess<*>,
            assignedUser: PrincipalCompat?
        ): Boolean {
            return node.canTakeTaskAutoProgress(createActivityContext(engineData), this, assignedUser)
        }

        override fun doStartTask(engineData: MutableProcessEngineDataAccess<*>): Boolean {
            return node.canStartTaskAutoProgress(this)
        }


    }

    private class ExtBuilderImpl(
        base: DefaultProcessNodeInstance,
        processInstanceBuilder: ProcessInstance.Builder
    ) : ExtBuilder<ExecutableProcessNode, DefaultProcessNodeInstance>(base, processInstanceBuilder), Builder {
        override var node: ExecutableProcessNode by overlay { base.node }

        override fun build() = when {
            changed -> DefaultProcessNodeInstance(this).also { invalidateBuilder(it) }
            else -> base
        }

        override var assignedUser: PrincipalCompat? = base.assignedUser
    }

    class BaseBuilder(
        node: ExecutableProcessNode,
        predecessors: Iterable<PNIHandle>,
        processInstanceBuilder: ProcessInstance.Builder,
        owner: PrincipalCompat,
        entryNo: Int,
        override var assignedUser: PrincipalCompat? = null,
        handle: PNIHandle = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : ProcessNodeInstance.BaseBuilder<ExecutableProcessNode, DefaultProcessNodeInstance>(
        node,
        predecessors,
        processInstanceBuilder,
        owner,
        entryNo,
        handle,
        state
    ), Builder {

        override fun build(): DefaultProcessNodeInstance = DefaultProcessNodeInstance(this)
    }

    class Factory : XmlDeserializerFactory<XmlProcessNodeInstance> {

        override fun deserialize(reader: XmlReader): XmlProcessNodeInstance {
            return XmlProcessNodeInstance.deserialize(reader)
        }
    }

    companion object {

        fun build(
            node: ExecutableProcessNode,
            predecessors: Set<PNIHandle>,
            processInstanceBuilder: ProcessInstance.Builder,
            handle: Handle<SecureObject<DefaultProcessNodeInstance>> = Handle.invalid(),
            state: NodeInstanceState = NodeInstanceState.Pending,
            entryNo: Int,
            assignedUser: PrincipalCompat?,
            body: Builder.() -> Unit
        ): DefaultProcessNodeInstance {
            return DefaultProcessNodeInstance(
                BaseBuilder(
                    node, predecessors, processInstanceBuilder, processInstanceBuilder.owner,
                    entryNo, assignedUser, handle, state
                ).apply(body)
            )
        }


        fun build(
            node: ExecutableProcessNode,
            predecessors: Set<PNIHandle>,
            processInstance: ProcessInstance,
            handle: Handle<SecureObject<DefaultProcessNodeInstance>> = Handle.invalid(),
            state: NodeInstanceState = NodeInstanceState.Pending,
            entryNo: Int,
            assignedUser: PrincipalCompat?,
            body: Builder.() -> Unit
        ): DefaultProcessNodeInstance {
            return build(
                node = node,
                predecessors = predecessors,
                processInstanceBuilder = processInstance.builder(),
                handle = handle,
                state = state,
                entryNo = entryNo,
                assignedUser = assignedUser,
                body = body
            )
        }


    }

}
