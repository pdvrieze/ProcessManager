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

import foo.FakeSerializable
import foo.FakeSerializer
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.engine.XML_BUILDER_VISITOR
import nl.adaptivity.process.processModel.engine.XmlChildModel
import nl.adaptivity.process.processModel.engine.XmlProcessModel
import nl.adaptivity.process.util.IdentifiableSetSerializer
import nl.adaptivity.process.util.IdentifyableSet
import nl.adaptivity.serialutil.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlPolyChildren
import nl.adaptivity.serialutil.encodeNullableStringElement
import nl.adaptivity.util.multiplatform.name
import kotlin.jvm.JvmField

/**
 * Base class for submodels
 */
@FakeSerializable
abstract class ChildProcessModelBase<NodeT : ProcessNode> :
    ProcessModelBase<NodeT>, ChildProcessModel<NodeT> {

    @SerialName("nodes")
    @XmlPolyChildren(
        arrayOf(
            "nl.adaptivity.process.processModel.engine.XmlActivity\$Builder=pe:activity",
            "nl.adaptivity.process.processModel.engine.XmlStartNode\$Builder=pe:start",
            "nl.adaptivity.process.processModel.engine.XmlSplit\$Builder=pe:split",
            "nl.adaptivity.process.processModel.engine.XmlJoin\$Builder=pe:join",
            "nl.adaptivity.process.processModel.engine.XmlEndNode\$Builder=pe:end",
            "nl.adaptivity.process.processModel.engine.XmlActivity=pe:activity",
            "nl.adaptivity.process.processModel.engine.XmlStartNode=pe:start",
            "nl.adaptivity.process.processModel.engine.XmlSplit=pe:split",
            "nl.adaptivity.process.processModel.engine.XmlJoin=pe:join",
            "nl.adaptivity.process.processModel.engine.XmlEndNode=pe:end"
               )
                    )
    @FakeSerializable(IdentifiableSetSerializer::class)
    override val modelNodes: IdentifyableSet<NodeT>

    @Transient
    private var _rootModel: RootProcessModel<NodeT> = XmlProcessModel(RootProcessModelBase.Builder()) as RootProcessModel<NodeT>

    override val rootModel: RootProcessModel<NodeT>
        get() = _rootModel

    @SerialName("id")
    override val id: String?

    @Suppress("LeakingThis")
    constructor(
        builder: ChildProcessModel.Builder,
        buildHelper: ProcessModel.BuildHelper<NodeT, ProcessModel<NodeT>, *, *>
               ) :
        super(builder, buildHelper.pedantic) {
        modelNodes = buildNodes(builder, buildHelper.withOwner(this))
        val newOwner: ProcessModel<NodeT> = buildHelper.newOwner
        _rootModel = newOwner.rootModel
        this.id = builder.childId
    }

    /* Invalid constructor purely for serialization */
    @Suppress("UNCHECKED_CAST", "LeakingThis")
    protected constructor() : super(emptyList(), emptyList()) {
        modelNodes = IdentifyableSet.processNodeSet()
        _rootModel = XmlProcessModel(RootProcessModelBase.Builder()) as RootProcessModel<NodeT>
        id = null
        if (id == null) {// stupid if to make the compiler not complain about uninitialised values
            throw UnsupportedOperationException("Actually invoking this constructor is invalid")
        }
    }


    override abstract fun builder(rootBuilder: RootProcessModel.Builder): ModelBuilder

    @FakeSerializable(with = ModelBuilder.Companion::class)
    open class ModelBuilder : ProcessModelBase.Builder, ChildProcessModel.Builder {

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

        constructor(
            rootBuilder: RootProcessModel.Builder,
            childId: String? = null,
            nodes: Collection<ProcessNode.Builder> = emptyList(),
            imports: Collection<IXmlResultType> = emptyList(),
            exports: Collection<IXmlDefineType> = emptyList()
                   ) : super(nodes, imports, exports) {
            this._rootBuilder = rootBuilder
            this.childId = childId
        }

        constructor(rootBuilder: RootProcessModel.Builder, base: ChildProcessModel<*>) :
            this(
                rootBuilder,
                base.id,
                base.modelNodes.map { it.visit(XML_BUILDER_VISITOR) },
                base.imports,
                base.exports
                )

        /**
         * When this is overridden and it returns a non-`null` value, it will allow childmodels to be nested in eachother.
         * Note that this does not actually introduce a scope. The nesting is not retained.
         */
        open fun nestedBuilder(): ModelBuilder? = null

        abstract class BaseSerializer<T : ModelBuilder> : ProcessModelBase.Builder.BaseSerializer<T>() {
            override fun readElement(result: T, input: CompositeDecoder, index: Int, name: String) {
                when (name) {
                    ATTR_ID -> result.childId = input.readNullableString(descriptor, index)
                    else    -> super.readElement(result, input, index, name)
                }
            }
        }

        @FakeSerializer(forClass = ModelBuilder::class)
        companion object : BaseSerializer<ModelBuilder>() {
            override val descriptor: SerialDescriptor = XmlChildModel.descriptor.withName(
                ModelBuilder::class.name
            )

            override fun builder(): ModelBuilder {
                return ModelBuilder()
            }


            @Suppress("RedundantOverride") // Without this serialization will generate the code
            override fun deserialize(decoder: Decoder): ModelBuilder {
                return super.deserialize(decoder)
            }

            override fun serialize(encoder: Encoder, obj: ModelBuilder) {
                val rootModel = XmlProcessModel(RootProcessModelBase.Builder().apply { childModels.add(obj) })
                XmlChildModel.serialize(encoder, rootModel.childModels.single())
                throw UnsupportedOperationException("Cannot be independently saved")
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
        val ELEMENTNAME = QName(
            ProcessConsts.Engine.NAMESPACE, ELEMENTLOCALNAME,
            ProcessConsts.Engine.NSPREFIX
                               )


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
