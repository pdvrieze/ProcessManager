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

import net.devrieze.util.*
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.impl.generateXmlString
import nl.adaptivity.process.processModel.engine.ExecutableCompositeActivity
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialize
import nl.adaptivity.xmlutil.smartStartTag
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

/**
 * Class representing a node instance that wraps a composite activity.
 */
class CompositeInstance<C : ActivityInstanceContext>(builder: Builder<C>) : ProcessNodeInstance<CompositeInstance<C>,C>(builder) {

    interface Builder<C : ActivityInstanceContext> : ProcessNodeInstance.Builder<ExecutableCompositeActivity, CompositeInstance<C>, C> {
        var hChildInstance: Handle<SecureObject<ProcessInstance<*>>>

        override fun doProvideTask(engineData: MutableProcessEngineDataAccess<C>): Boolean {
            val shouldProgress = node.canProvideTaskAutoProgress(engineData, this)

            val childHandle = engineData.instances.put(ProcessInstance(engineData, node.childModel, handle) {})
            hChildInstance = childHandle

            store(engineData)
            engineData.commit()
            return shouldProgress
        }

        override fun canTakeTaskAutomatically(): Boolean = true

        @OptIn(ProcessInstanceStorage::class)
        override fun doStartTask(engineData: MutableProcessEngineDataAccess<C>): Boolean {
            val shouldProgress = tryCreateTask { node.canStartTaskAutoProgress(this) }

            assert(hChildInstance.isValid) { "The task can only be started if the child instance already exists" }
            tryCreateTask {
                val instanceContext = engineData.processContextFactory.newActivityInstanceContext(engineData, this)
                engineData.updateInstance(hChildInstance) {
                    start(engineData, with(build()) { instanceContext.getPayload(processInstanceBuilder) })
                }
            }
            engineData.queueTickle(hChildInstance)
            return shouldProgress
        }

        override fun doFinishTask(
            engineData: MutableProcessEngineDataAccess<C>,
            resultPayload: ICompactFragment?
        ) {
            val childInstance = engineData.instance(hChildInstance).withPermission()
            if (childInstance.state != ProcessInstance.State.FINISHED) {
                throw ProcessException("A Composite task cannot be finished until its child process is. The child state is: ${childInstance.state}")
            }
            return super.doFinishTask(engineData, childInstance.getOutputPayload())
        }

        override fun doTakeTask(
            engineData: MutableProcessEngineDataAccess<C>,
            assignedUser: PrincipalCompat?
        ): Boolean {
            return true
        }
    }

    class BaseBuilder<C: ActivityInstanceContext>(
        node: ExecutableCompositeActivity,
        predecessor: Handle<SecureObject<ProcessNodeInstance<*, C>>>?,
        processInstanceBuilder: ProcessInstance.Builder<C>,
        override var hChildInstance: Handle<SecureObject<ProcessInstance<*>>>,
        owner: PrincipalCompat,
        entryNo: Int,
        handle: Handle<SecureObject<ProcessNodeInstance<*, C>>> = Handle.invalid(),
        state: NodeInstanceState = NodeInstanceState.Pending
    ) : ProcessNodeInstance.BaseBuilder<ExecutableCompositeActivity, CompositeInstance<C>, C>(
        node, listOfNotNull(predecessor), processInstanceBuilder, owner,
        entryNo, handle, state
    ), Builder<C> {

        override fun invalidateBuilder(engineData: ProcessEngineDataAccess<C>) {
            engineData.nodeInstances[handle]?.withPermission()?.let { n ->
                val newBase = n as CompositeInstance
                node = newBase.node
                predecessors.replaceBy(newBase.predecessors)
                state = newBase.state
                hChildInstance = newBase.hChildInstance
            }
        }

        override fun build(): CompositeInstance<C> {
            return CompositeInstance(this)
        }
    }

    class ExtBuilder<C: ActivityInstanceContext>(base: CompositeInstance<C>, processInstanceBuilder: ProcessInstance.Builder<C>) :
        ProcessNodeInstance.ExtBuilder<ExecutableCompositeActivity, CompositeInstance<C>, C>(base, processInstanceBuilder),
        Builder<C> {

        override var node: ExecutableCompositeActivity by overlay { base.node }

        override var hChildInstance: Handle<SecureObject<ProcessInstance<*>>> by overlay(
            observer()
        ) { base.hChildInstance }

        override fun build(): CompositeInstance<C> {
            return if (changed) CompositeInstance(this).also { invalidateBuilder(it) } else base
        }
    }

    val hChildInstance: Handle<SecureObject<ProcessInstance<*>>> =
        builder.hChildInstance.apply {
            if (! (builder.state==NodeInstanceState.Pending || isValid))
                throw ProcessException("Child process instance handles must be valid if the state isn't pending")
        }

    override val node: ExecutableCompositeActivity get() = super.node as ExecutableCompositeActivity

    override fun builder(processInstanceBuilder: ProcessInstance.Builder<C>) = ExtBuilder(this, processInstanceBuilder)

    fun <C : ActivityInstanceContext> C.getPayload(nodeInstanceSource: IProcessInstance<C>): CompactFragment? {
        val defines = getDefines(nodeInstanceSource)
        if (defines.isEmpty()) return null

        val content = buildString {
            for (data in defines) {
                append(generateXmlString(true) { writer ->
                    writer.smartStartTag(QName(data.name!!)) {
                        writer.serialize(data.contentStream)
                    }
                })
                append("\n")
            }
        }
        return CompactFragment(content)
    }
}
