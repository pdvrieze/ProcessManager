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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import net.devrieze.util.ArraySet
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.toMutableArraySet
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.xml.*
import nl.adaptivity.xml.serialization.XmlSerialName


/**
 * Class representing an activity in a process engine. Activities are expected
 * to invoke one (and only one) web service. Some services are special in that
 * they either invoke another process (and the process engine can treat this
 * specially in later versions), or set interaction with the user. Services can
 * use the ActivityResponse soap header to indicate support for processes and
 * what the actual state of the task after return should be (instead of
 *
 * @author Paul de Vrieze
 */
@Serializable
@XmlSerialName(Activity.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
class XmlActivity : ActivityBase<XmlProcessNode, XmlModelCommon>, XmlProcessNode {

    constructor(builder: Activity.Builder<*, *>,
                buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>) : super(builder, buildHelper)

    constructor(builder: Activity.ChildModelBuilder<*, *>,
                buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>) : super(builder, buildHelper)

    @Transient
    private var xmlCondition: XmlCondition? = null

    override fun builder(): Builder {
        return Builder(this)
    }

    @Throws(XmlException::class)
    override fun serializeCondition(out: XmlWriter) {
        out.writeChild(xmlCondition)
    }

    override var condition: String?
        get() = xmlCondition?.toString()
        set(condition) {
            xmlCondition = condition?.let { XmlCondition(it) }
            notifyChange()
        }

    public override fun setOwnerModel(newOwnerModel: XmlModelCommon) {
        super.setOwnerModel(newOwnerModel)
    }

    public override fun setPredecessors(predecessors: Collection<Identifiable>) {
        super.setPredecessors(predecessors)
    }

    public override fun removePredecessor(predecessorId: Identified) {
        super.removePredecessor(predecessorId)
    }

    public override fun addPredecessor(predecessorId: Identified) {
        super.addPredecessor(predecessorId)
    }

    public override fun addSuccessor(successorId: Identified) {
        super.addSuccessor(successorId)
    }

    public override fun removeSuccessor(successorId: Identified) {
        super.removeSuccessor(successorId)
    }

    public override fun setSuccessors(successors: Collection<Identified>) {
        super.setSuccessors(successors)
    }

    companion object {

        @Throws(XmlException::class)
        fun deserialize(buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>,
                        reader: XmlReader): XmlActivity {
            return XmlActivity.Builder().deserializeHelper(reader).build(buildHelper)
        }

        @Throws(XmlException::class)
        fun deserialize(reader: XmlReader): XmlActivity.Builder {
            return Builder().deserializeHelper(reader)
        }
    }

    @Serializable
    class Builder : ActivityBase.Builder<XmlProcessNode, XmlModelCommon>, XmlProcessNode.Builder {

        constructor()

        constructor(predecessor: Identified? = null,
                    successor: Identified? = null,
                    id: String? = null,
                    label: String? = null,
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    message: XmlMessage? = null,
                    condition: String? = null,
                    name: String? = null,
                    multiInstance: Boolean = false)
            : super(id, predecessor, successor, label, defines, results, message, condition, name, x, y,
                    multiInstance) {
        }

        constructor(node: Activity<*, *>) : super(node) {}

        override fun build(buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>): XmlActivity {
            return XmlActivity(this, buildHelper)
        }
    }

    @Serializable
    class ChildModelBuilder : XmlChildModel.Builder, Activity.ChildModelBuilder<XmlProcessNode, XmlModelCommon>, XmlModelCommon.Builder {

        override var id: String?
        override var condition: String?
        override var label: String?
        override var x: Double
        override var y: Double
        override var isMultiInstance: Boolean

        @SerialName("predecessor")
        override var predecessors: MutableSet<Identified>
            set(value) {
                field.replaceBy(value)
            }

        @Transient
        override var successors: MutableSet<Identified>
            set(value) {
                field.replaceBy(value)
            }

        @SerialName("define")
        override var defines: MutableCollection<IXmlDefineType>
            set(value) {
                field.replaceBy(value)
            }

        @SerialName("result")
        override var results: MutableCollection<IXmlResultType>
            set(value) {
                field.replaceBy(value)
            }

        private constructor(): super() {
            id = null
            condition = null
            label = null
            x = Double.NaN
            y = Double.NaN
            isMultiInstance = false
            predecessors = ArraySet()
            successors = ArraySet()
            defines = mutableListOf()
            results = mutableListOf()
        }

        constructor(rootBuilder: XmlProcessModel.Builder,
                    id: String? = null,
                    childId: String? = null,
                    nodes: Collection<XmlProcessNode.Builder> = emptyList(),
                    predecessors: Collection<Identified> = emptyList(),
                    condition: String? = null,
                    successors: Collection<Identified> = emptyList(),
                    label: String? = null,
                    imports: Collection<IXmlResultType> = emptyList(),
                    defines: Collection<IXmlDefineType> = emptyList(),
                    exports: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    isMultiInstance: Boolean = false) : super(rootBuilder, childId, nodes, imports,
                                                              exports) {
            this.id = id
            this.condition = condition
            this.label = label
            this.x = x
            this.y = y
            this.isMultiInstance = isMultiInstance
            this.predecessors = predecessors.toMutableArraySet()
            this.successors = successors.toMutableArraySet()
            this.defines = defines.toMutableList()
            this.results = results.toMutableList()
        }

        override fun buildModel(buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>): ChildProcessModel<XmlProcessNode, XmlModelCommon> {
            return XmlChildModel(this, buildHelper)
        }

        override fun buildActivity(buildHelper: ProcessModel.BuildHelper<XmlProcessNode, XmlModelCommon>): Activity<XmlProcessNode, XmlModelCommon> {
            return XmlActivity(this, buildHelper)
        }

        @Serializer(forClass = ChildModelBuilder::class)
        companion object: ChildProcessModelBase.Builder.BaseSerializer<ChildModelBuilder>() {
            override fun builder(): ChildModelBuilder {
                return ChildModelBuilder()
            }
        }
    }

}

