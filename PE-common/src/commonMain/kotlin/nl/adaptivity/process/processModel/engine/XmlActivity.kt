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

import kotlinx.serialization.*
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.util.SerialClassDescImpl
import nl.adaptivity.util.addField
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.multiplatform.name
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlDefault
import nl.adaptivity.xmlutil.serialization.XmlSerialName


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
@Serializable(XmlActivity.Companion::class)
@XmlSerialName(Activity.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
class XmlActivity : ActivityBase, XmlProcessNode {

    constructor(builder: Activity.Builder,
                buildHelper: BuildHelper<*, *, *, *>) : super(builder, buildHelper)

    constructor(builder: Activity.CompositeActivityBuilder,
                buildHelper: BuildHelper<*, *, *, *>) : super(builder, buildHelper)

    @Transient
    private var xmlCondition: XmlCondition? = null

    @Throws(XmlException::class)
    override fun serializeCondition(out: XmlWriter) {
        out.writeChild(xmlCondition)
    }

    override val condition: String?
        get() = xmlCondition?.toString()

    @Serializer(forClass = XmlActivity::class)
    companion object:KSerializer<XmlActivity> {

        override fun deserialize(decoder: Decoder): XmlActivity {
            throw UnsupportedOperationException("This can only done in the correct context")
        }
    }

    @Serializable
    class CompositeActivityBuilder : ChildProcessModelBase.Builder,
                                     Activity.CompositeActivityBuilder {

        override var id: String?
        override var condition: String?
        override var label: String?
        @XmlDefault("NaN")
        override var x: Double
        @XmlDefault("NaN")
        override var y: Double
        override var isMultiInstance: Boolean
        @Serializable(with = Identifiable.Companion::class)
        override var predecessor: Identifiable? = null
        @Transient
        override var successor: Identifiable? = null

        @Serializable(IXmlDefineTypeListSerializer::class)
        @SerialName("define")
        override var defines: MutableCollection<IXmlDefineType> = mutableListOf()
            set(value) {
                field.replaceBy(value)
            }

        @Serializable(IXmlResultTypeListSerializer::class)
        @SerialName("result")
        override var results: MutableCollection<IXmlResultType> = mutableListOf()
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

            defines = mutableListOf()
            results = mutableListOf()
        }

        constructor(rootBuilder: RootProcessModel.Builder,
                    id: String? = null,
                    childId: String? = null,
                    nodes: Collection<ProcessNode.IBuilder> = emptyList(),
                    predecessor: Identifiable? = null,
                    condition: String? = null,
                    successor: Identifiable? = null,
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
            this.predecessor = predecessor
            this.successor = successor
            this.defines = defines.toMutableList()
            this.results = results.toMutableList()
        }

        override fun <T : ProcessNode> build(buildHelper: ProcessModel.BuildHelper<T, *, *, *>): T = buildHelper.node(this)

        fun buildActivity(buildHelper: ProcessModel.BuildHelper<*,*,*,*>): XmlActivity {
            return XmlActivity(this, buildHelper)
        }

        @Serializer(forClass = CompositeActivityBuilder::class)
        companion object: ChildProcessModelBase.Builder.BaseSerializer<CompositeActivityBuilder>() {
            override val descriptor: SerialDescriptor = SerialClassDescImpl(ActivityBase.Builder.serializer().descriptor, ChildProcessModelBase.Builder::class.name).apply {
                addField(CompositeActivityBuilder::childId)
            }

            override fun builder(): CompositeActivityBuilder {
                return CompositeActivityBuilder()
            }
        }
    }

}

