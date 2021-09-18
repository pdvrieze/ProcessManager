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

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.serialization.XML


/**
 * Activity version that is used for process execution.
 */
class ExecutableMessageActivity : MessageActivityBase, ExecutableProcessNode {

    constructor(
        builder: MessageActivity.Builder,
        newOwner: ProcessModel<*>,
        otherNodes: Iterable<ProcessNode.Builder>
    ) : super(builder, newOwner, otherNodes) {
        this._condition = builder.condition?.toExecutableCondition()
    }

    private var _condition: ExecutableCondition?

    override val ownerModel: ExecutableModelCommon
        get() = super.ownerModel as ExecutableModelCommon

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    override var condition: Condition?
        get() = _condition
        set(value) {
            _condition = value?.toExecutableCondition()
        }

    /**
     * Determine whether the process can start.
     */
    override fun evalCondition(
        engineData: ProcessEngineDataAccess,
        predecessor: IProcessNodeInstance,
        instance: IProcessNodeInstance
    ): ConditionResult {
        return _condition.evalCondition(engineData, predecessor, instance)
    }

    override fun createOrReuseInstance(
        data: MutableProcessEngineDataAccess,
        processInstanceBuilder: ProcessInstance.Builder,
        predecessor: IProcessNodeInstance,
        entryNo: Int,
        allowFinalInstance: Boolean
    ): ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>> {
        return super.createOrReuseInstance(data, processInstanceBuilder, predecessor, entryNo, allowFinalInstance)
    }

    override fun provideTask(
        engineData: ProcessEngineDataAccess,
        instanceBuilder: ProcessNodeInstance.Builder<*, *>
                            ) = false

    /**
     * Take the task. Tasks are either process aware or finished when a reply is
     * received. In either case they should not be automatically taken.
     *
     * @return `false`
     */
    override fun takeTask(instance: ProcessNodeInstance.Builder<*, *>) = false

    /**
     * Start the task. Tasks are either process aware or finished when a reply is
     * received. In either case they should not be automatically started.
     *
     * @return `false`
     */
    override fun startTask(instance: ProcessNodeInstance.Builder<*, *>): Boolean = false

    @Throws(XmlException::class)
    fun serializeCondition(out: XmlWriter) {
        condition?.let {
            XML.Companion.encodeToWriter(out, XmlCondition(it.condition))
        }
    }

}
