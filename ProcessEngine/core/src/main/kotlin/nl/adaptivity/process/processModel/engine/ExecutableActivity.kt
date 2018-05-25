/*
 * Copyright (c) 2018.
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

import net.devrieze.util.collection.replaceBy
import net.devrieze.util.getInvalidHandle
import nl.adaptivity.process.engine.MutableProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.ProcessInstance
import nl.adaptivity.process.engine.processModel.CompositeInstance
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xml.XmlException
import nl.adaptivity.xml.XmlWriter
import nl.adaptivity.xml.writeChild


/**
 * Activity version that is used for process execution.
 */
class ExecutableActivity : ActivityBase<ExecutableProcessNode, ExecutableModelCommon>, ExecutableProcessNode {

    constructor(builder: Activity.Builder<*, *>,
                buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>) : super(builder,
                                                                                                             buildHelper) {
        this._condition = builder.condition?.let(::ExecutableCondition)
    }

    constructor(builder: Activity.ChildModelBuilder<*, *>,
                buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>) : super(builder,
                                                                                                             buildHelper) {
        this._condition = builder.condition?.let(::ExecutableCondition)
    }

    class Builder : ActivityBase.Builder<ExecutableProcessNode, ExecutableModelCommon>, ExecutableProcessNode.Builder {

        constructor(id: String? = null,
                    predecessor: Identified? = null,
                    successor: Identified? = null,
                    label: String? = null,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    message: XmlMessage? = null,
                    condition: String? = null,
                    name: String? = null,
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    multiInstance: Boolean = false) : super(id, predecessor, successor, label, defines, results,
                                                            message, condition, name, x, y, multiInstance)

        constructor(node: Activity<*, *>) : super(node)

        override fun build(buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>) = ExecutableActivity(
            this, buildHelper)
    }


    class ChildModelBuilder(override val rootBuilder: ExecutableProcessModel.Builder,
                            override var id: String? = null,
                            childId: String? = null,
                            nodes: Collection<ExecutableProcessNode.Builder> = emptyList(),
                            override var predecessor: Identifiable? = null,
                            override var condition: String? = null,
                            override var successor: Identifiable? = null,
                            override var label: String? = null,
                            imports: Collection<IXmlResultType> = emptyList(),
                            defines: Collection<IXmlDefineType> = emptyList(),
                            exports: Collection<IXmlDefineType> = emptyList(),
                            results: Collection<IXmlResultType> = emptyList(),
                            override var x: Double = Double.NaN,
                            override var y: Double = Double.NaN,
                            override var isMultiInstance: Boolean = false) :
        ExecutableChildModel.Builder(rootBuilder, childId, nodes, imports, exports),
        Activity.ChildModelBuilder<ExecutableProcessNode, ExecutableModelCommon>,
        ExecutableModelCommon.Builder,
        ExecutableProcessNode.Builder {

        override var defines: MutableCollection<IXmlDefineType> = java.util.ArrayList(defines)
            set(value) {
                field.replaceBy(value)
            }

        override var results: MutableCollection<IXmlResultType> = java.util.ArrayList(results)
            set(value) {
                field.replaceBy(value)
            }

        override fun buildModel(buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>) =
            ExecutableChildModel(this, buildHelper)

        override fun buildActivity(buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>): Activity<ExecutableProcessNode, ExecutableModelCommon> {
            return ExecutableActivity(this, buildHelper)
        }

        override fun build(buildHelper: ProcessModel.BuildHelper<ExecutableProcessNode, ExecutableModelCommon>) = buildActivity(
            buildHelper)
    }

    override val childModel: ExecutableChildModel? get() = super.childModel?.let { it as ExecutableChildModel }
    private var _condition: ExecutableCondition?

    override val id: String get() = super.id ?: throw IllegalStateException("Excecutable nodes must have an id")

    override var condition: String?
        get() = _condition?.condition
        set(value) {
            _condition = condition?.let(::ExecutableCondition)
        }


    override fun builder() = Builder(node = this)

    /**
     * Determine whether the process can start.
     */
    override fun condition(engineData: ProcessEngineDataAccess, predecessor: IProcessNodeInstance, instance: IProcessNodeInstance): ConditionResult {
        return _condition?.run { eval(engineData, instance) } ?: ConditionResult.TRUE
    }

    override fun createOrReuseInstance(data: MutableProcessEngineDataAccess,
                                       processInstanceBuilder: ProcessInstance.Builder,
                                       predecessor: IProcessNodeInstance,
                                       entryNo: Int): ProcessNodeInstance.Builder<out ExecutableProcessNode, out ProcessNodeInstance<*>> {
        return if (childModel == null)
            super.createOrReuseInstance(data, processInstanceBuilder, predecessor, entryNo)
        else // TODO handle invalidating multiple instances
            processInstanceBuilder.getChild(this, entryNo) ?: CompositeInstance.BaseBuilder(this, predecessor.handle(),
                                                                                            processInstanceBuilder,
                                                                                            getInvalidHandle(),
                                                                                            processInstanceBuilder.owner,
                                                                                            entryNo)
    }

    override fun provideTask(engineData: ProcessEngineDataAccess,
                             instanceBuilder: ProcessNodeInstance.Builder<*, *>) = childModel != null

    /**
     * Take the task. Tasks are either process aware or finished when a reply is
     * received. In either case they should not be automatically taken.
     *
     * @return `false`
     */
    override fun takeTask(instance: ProcessNodeInstance.Builder<*, *>) = childModel != null

    /**
     * Start the task. Tasks are either process aware or finished when a reply is
     * received. In either case they should not be automatically started.
     *
     * @return `false`
     */
    override fun startTask(instance: ProcessNodeInstance.Builder<*, *>) = false

    @Throws(XmlException::class)
    override fun serializeCondition(out: XmlWriter) {
        out.writeChild(_condition)
    }

}