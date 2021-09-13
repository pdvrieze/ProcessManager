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
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * A class representing a process model.
 *
 * @author Paul de Vrieze
 */
@Serializable(XmlProcessModel.Companion::class)
class XmlProcessModel : RootProcessModelBase<XmlProcessNode> {

    override val rootModel: XmlProcessModel
        get() = this

    @Suppress("UNCHECKED_CAST")
    override val childModels: Collection<XmlChildModel>
        get() = super.childModels as Collection<XmlChildModel>

    @Suppress("ConvertSecondaryConstructorToPrimary") // For serialization
    constructor(builder: RootProcessModel.Builder, pedantic: Boolean = true) : super(
        builder,
        XML_NODE_FACTORY as NodeFactory<XmlProcessNode, XmlProcessNode, ChildProcessModelBase<XmlProcessNode>>,
        pedantic
    )

    @Suppress("ConvertSecondaryConstructorToPrimary") // For serialization
    internal constructor(delegate: SerialDelegate, pedantic: Boolean = true) : super(
        Builder(delegate),
        XML_NODE_FACTORY as NodeFactory<XmlProcessNode, XmlProcessNode, ChildProcessModelBase<XmlProcessNode>>,
        pedantic
    )

    override fun builder(): RootProcessModel.Builder {
        return Builder(this)
    }

    override fun update(body: (RootProcessModel.Builder) -> Unit): RootProcessModelBase<XmlProcessNode> {
        return XmlProcessModel(builder().apply(body))
    }

    override fun getChildModel(childId: Identifiable): XmlChildModel? {
        return super.getChildModel(childId)?.let { it as XmlChildModel }
    }

    companion object : KSerializer<XmlProcessModel> {
        private val delegateSerializer = SerialDelegate.serializer()

        override val descriptor: SerialDescriptor get() = delegateSerializer.descriptor

        val serialModule = SerializersModule {
            include(ProcessNodeBase.serialModule)
        }

        @Suppress("RedundantOverride")
        override fun serialize(encoder: Encoder, value: XmlProcessModel) {
            delegateSerializer.serialize(encoder, SerialDelegate(value))
        }

        override fun deserialize(decoder: Decoder): XmlProcessModel {
            // TODO use concrete serializer instance
            return XmlProcessModel(delegateSerializer.deserialize(decoder), true)
        }

        @kotlin.jvm.JvmOverloads
        @kotlin.jvm.JvmStatic
        fun deserialize(reader: XmlReader, pedantic: Boolean = true): XmlProcessModel {
            val delegate: SerialDelegate = XML { autoPolymorphic = true }.decodeFromReader(delegateSerializer, reader)
            return XmlProcessModel(delegate, pedantic)
        }

    }


    @Serializable(Builder.Companion::class)
    open class Builder : RootProcessModelBase.Builder {

        constructor(
            nodes: Collection<ProcessNode.Builder> = emptySet(),
            childModels: Collection<ChildProcessModel.Builder> = emptySet(),
            name: String? = null,
            handle: Long = -1L,
            owner: Principal = SYSTEMPRINCIPAL,
            roles: Set<String> = emptySet(),
            uuid: UUID? = null,
            imports: List<IXmlResultType> = emptyList(),
            exports: List<IXmlDefineType> = emptyList()
        ) : super(
            nodes, childModels, name, handle, owner, roles, uuid,
            imports, exports
        )

        constructor(base: XmlProcessModel) : super(base)

        internal constructor(serialDelegate: SerialDelegate) : super(serialDelegate)

        @OptIn(InternalSerializationApi::class)
        companion object : KSerializer<Builder> {
            private val delegateSerializer = SerialDelegate.serializer()

            override val descriptor: SerialDescriptor get() = delegateSerializer.descriptor


            @Suppress("RedundantOverride")
            override fun deserialize(decoder: Decoder): Builder {
                return Builder(delegateSerializer.deserialize(decoder))
            }

            override fun serialize(encoder: Encoder, value: Builder) {
                delegateSerializer.serialize(encoder, SerialDelegate(value))
            }

            fun deserialize(reader: XmlReader): Builder {
                return Builder(XML.decodeFromReader(delegateSerializer, reader))
            }
        }
    }

}

val XML_BUILDER_VISITOR = object : ProcessNode.Visitor<ProcessNode.Builder> {
    override fun visitStartNode(startNode: StartNode) = StartNodeBase.Builder(startNode)

    override fun visitActivity(compositeActivity: CompositeActivity) =
        CompositeActivityBase.ReferenceBuilder(compositeActivity)

    override fun visitActivity(messageActivity: MessageActivity) = MessageActivityBase.Builder(messageActivity)

    override fun visitSplit(split: Split) = SplitBase.Builder(split)

    override fun visitJoin(join: Join) = JoinBase.Builder(join)

    override fun visitEndNode(endNode: EndNode) = EndNodeBase.Builder(endNode)
}


@Suppress("ClassName")
object XML_NODE_FACTORY : ProcessModelBase.NodeFactory<XmlProcessNode, XmlProcessNode, XmlChildModel> {

    private class Visitor(
        private val buildHelper: ProcessModel.BuildHelper<*, *, *, *>,
        private val otherNodes: Iterable<ProcessNode.Builder>
    ) : ProcessNode.BuilderVisitor<XmlProcessNode> {
        override fun visitStartNode(startNode: StartNode.Builder) = XmlStartNode(startNode, buildHelper.newOwner)

        override fun visitActivity(activity: MessageActivity.Builder) =
            XmlActivity(activity, buildHelper.newOwner, otherNodes)

        override fun visitActivity(activity: CompositeActivity.ModelBuilder) =
            XmlActivity(activity, buildHelper, otherNodes)

        override fun visitActivity(activity: CompositeActivity.ReferenceBuilder) =
            XmlActivity(activity, buildHelper, otherNodes)

        override fun visitSplit(split: Split.Builder) = XmlSplit(split, buildHelper.newOwner, otherNodes)

        override fun visitJoin(join: Join.Builder) = XmlJoin(join, buildHelper, otherNodes)

        override fun visitEndNode(endNode: EndNode.Builder) = XmlEndNode(endNode, buildHelper.newOwner, otherNodes)
    }

    override fun invoke(
        baseNodeBuilder: ProcessNode.Builder,
        buildHelper: ProcessModel.BuildHelper<XmlProcessNode, *, *, *>,
        otherNodes: Iterable<ProcessNode.Builder>
    ): XmlProcessNode {
        return baseNodeBuilder.visit(Visitor(buildHelper, otherNodes))
    }

    override fun invoke(
        baseChildBuilder: ChildProcessModel.Builder,
        buildHelper: ProcessModel.BuildHelper<XmlProcessNode, *, *, *>
    ): XmlChildModel {
        return XmlChildModel(baseChildBuilder, buildHelper)
    }

    override fun condition(condition: Condition) = condition as? XmlCondition ?: XmlCondition(condition.condition)
}
