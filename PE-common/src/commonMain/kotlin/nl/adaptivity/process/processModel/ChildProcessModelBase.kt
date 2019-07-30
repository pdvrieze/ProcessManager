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

package nl.adaptivity.process.processModel

import kotlinx.serialization.*
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.process.util.IdentifiableSetSerializer
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.serialutil.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlPolyChildren
import nl.adaptivity.serialutil.encodeNullableStringElement
import kotlin.jvm.JvmField

/**
 * Base class for submodels
 */
@Serializable
abstract class ChildProcessModelBase<NodeT : ProcessNode> :
    ProcessModelBase<NodeT>, ChildProcessModel<NodeT> {

    @Suppress("LeakingThis")
    constructor(builder: ChildProcessModel.Builder, buildHelper: ProcessModel.BuildHelper<NodeT, ProcessModel<NodeT>, *, *>) :
        super(builder, buildHelper.pedantic) {
        modelNodes = buildNodes(builder, buildHelper.withOwner(this))
        val newOwner: ProcessModel<NodeT> = buildHelper.newOwner
        rootModel = newOwner?.rootModel
                    ?: throw IllegalProcessModelException("Childmodels must have roots")
        this.id = builder.childId
    }

    /* Invalid constructor purely for serialization */
    @Suppress("UNCHECKED_CAST", "LeakingThis")
    protected constructor() : super(emptyList(), emptyList()) {
        modelNodes = IdentifyableSet.processNodeSet()
        rootModel = XmlProcessModel(XmlProcessModel.Builder()) as RootProcessModel<NodeT>
        id = null
        if (id == null) {// stupid if to make the compiler not complain about uninitialised values
            throw UnsupportedOperationException("Actually invoking this constructor is invalid")
        }
    }

    @SerialName("nodes")
    @XmlPolyChildren(arrayOf("nl.adaptivity.process.processModel.engine.XmlActivity\$Builder=pe:activity",
                             "nl.adaptivity.process.processModel.engine.XmlStartNode\$Builder=pe:start",
                             "nl.adaptivity.process.processModel.engine.XmlSplit\$Builder=pe:split",
                             "nl.adaptivity.process.processModel.engine.XmlJoin\$Builder=pe:join",
                             "nl.adaptivity.process.processModel.engine.XmlEndNode\$Builder=pe:end",
                             "nl.adaptivity.process.processModel.engine.XmlActivity=pe:activity",
                             "nl.adaptivity.process.processModel.engine.XmlStartNode=pe:start",
                             "nl.adaptivity.process.processModel.engine.XmlSplit=pe:split",
                             "nl.adaptivity.process.processModel.engine.XmlJoin=pe:join",
                             "nl.adaptivity.process.processModel.engine.XmlEndNode=pe:end"))
    @Serializable(IdentifiableSetSerializer::class)
    override val modelNodes: IdentifyableSet<NodeT>

    @Transient
    override val rootModel: RootProcessModel<NodeT>

    @SerialName("id")
    override val id: String?


    override abstract fun builder(rootBuilder: RootProcessModel.Builder): ChildProcessModelBase.Builder

    override fun serialize(out: XmlWriter) {
        out.smartStartTag(ELEMENTNAME) {
            writeAttribute(ATTR_ID, id)

            writeChildren(imports)
            writeChildren(exports)
            writeChildren(modelNodes)
        }
    }

    abstract class Builder : ProcessModelBase.Builder, ChildProcessModel.Builder {


        @Transient
        private lateinit var _rootBuilder: RootProcessModel.Builder

        @Transient
        override val rootBuilder: RootProcessModel.Builder
            get() = _rootBuilder

        @SerialName("id")
        final override var childId: String?

        protected constructor() {
            childId = null
        }

        constructor(rootBuilder: RootProcessModel.Builder,
                    childId: String? = null,
                    nodes: Collection<ProcessNode.IBuilder> = emptyList(),
                    imports: Collection<IXmlResultType> = emptyList(),
                    exports: Collection<IXmlDefineType> = emptyList()) : super(nodes, imports, exports) {
            this._rootBuilder = rootBuilder
            this.childId = childId
        }

        @Transient
        override val elementName: QName
            get() = ELEMENTNAME

        /**
         * When this is overridden and it returns a non-`null` value, it will allow childmodels to be nested in eachother.
         * Note that this does not actually introduce a scope. The nesting is not retained.
         */
        open fun nestedBuilder(): ChildProcessModelBase.Builder? = null

        override fun <NodeT: ProcessNode, ChildT: ChildProcessModel<NodeT>>buildModel(buildHelper: ProcessModel.BuildHelper<NodeT, *, *, ChildT>): ChildT {
            return buildHelper.childModel(this)
        }

        override fun deserializeChild(reader: XmlReader): Boolean {
            if (reader.isElement(ProcessConsts.Engine.NAMESPACE, ChildProcessModel.ELEMENTLOCALNAME)) {
                nestedBuilder()?.let { rootBuilder.childModels.add(deserializeHelper(reader)) }
                ?: throw XmlException("Child models are not currently allowed to be nested")
                return true
            } else {
                return super.deserializeChild(reader)
            }
        }

        override fun deserializeAttribute(attributeNamespace: String?,
                                          attributeLocalName: String,
                                          attributeValue: String): Boolean {
            return when (attributeLocalName) {
                ATTR_ID -> {
                    childId = attributeValue; true
                }
                else    -> super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue)
            }
        }

        abstract class BaseSerializer<T : ChildProcessModelBase.Builder> : ProcessModelBase.Builder.BaseSerializer<T>() {
            override fun readElement(result: T, input: KInput, index: Int, name:String) {
                when (name) {
                    ATTR_ID -> result.childId = input.readNullableString(descriptor, index)
                    else    -> super.readElement(result, input, index, name)
                }
            }
        }

    }

    abstract class BaseSerializer<T : ChildProcessModelBase<*>> : ProcessModelBase.BaseSerializer<T>() {

        private val idIdx by lazy { descriptor.getElementIndex(ATTR_ID) }

        override fun writeValues(output: CompositeEncoder, obj: T) {
            output.encodeNullableStringElement(descriptor, idIdx, obj.id)
            super.writeValues(output, obj)
        }
    }

    companion object {
        const val ATTR_ID = "id"
        const val ELEMENTLOCALNAME = "childModel"
        @JvmField
        val ELEMENTNAME = QName(ProcessConsts.Engine.NAMESPACE, ELEMENTLOCALNAME,
                                ProcessConsts.Engine.NSPREFIX)


/*
        fun descriptor(name: String): SerialClassDescImpl {
            return SerialClassDescImpl(name).apply {
                addField(ChildProcessModelBase<*, *>::id)
                addFields(ProcessModelBase.descriptor)
            }
        }
*/

    }

}
