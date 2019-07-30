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
import kotlinx.serialization.internal.GeneratedSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.security.Principal
import nl.adaptivity.xmlutil.XmlDeserializer
import nl.adaptivity.xmlutil.XmlDeserializerFactory
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * A class representing a process model.
 *
 * @author Paul de Vrieze
 */
@Serializable(XmlProcessModel.Companion::class)
@XmlDeserializer(XmlProcessModel.Factory::class)
@XmlSerialName(RootProcessModelBase.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
class XmlProcessModel : RootProcessModelBase<@ContextualSerialization XmlProcessNode> {

    @Transient
    override val rootModel: XmlProcessModel
        get() = this

    @Suppress("UNCHECKED_CAST")
    @XmlSerialName(ChildProcessModelBase.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE,
                   ProcessConsts.Engine.NSPREFIX)
    override val childModels: Collection<XmlChildModel>
        get() = super.childModels as Collection<XmlChildModel>

    @Suppress("ConvertSecondaryConstructorToPrimary") // For serialization
    constructor(builder: RootProcessModel.Builder, pedantic: Boolean = true) :
        super(builder,
              XML_NODE_FACTORY as ProcessModelBase.NodeFactory<XmlProcessNode, XmlProcessNode, ChildProcessModelBase<XmlProcessNode>>,
              pedantic)

    override fun copy(imports: Collection<IXmlResultType>,
                      exports: Collection<IXmlDefineType>,
                      nodes: Collection<ProcessNode>,
                      name: String?,
                      uuid: UUID?,
                      roles: Set<String>,
                      owner: Principal,
                      childModels: Collection<ChildProcessModel<XmlProcessNode>>): XmlProcessModel {
        return RootProcessModelBase.Builder(nodes.map { it.builder() }, emptySet(), name, handleValue, owner, roles,
                                       uuid).also { builder ->
            builder.childModels.replaceBy(childModels.map { it.builder(builder) })
        }.let{ XmlProcessModel(it, false) }
    }

    override fun builder(): RootProcessModel.Builder {
        return Builder(this)
    }

    override fun update(body: (RootProcessModel.Builder) -> Unit): RootProcessModelBase<XmlProcessNode> {
        return XmlProcessModel(builder().apply(body))
    }

    override fun getChildModel(childId: Identifiable): XmlChildModel? {
        return super.getChildModel(childId)?.let { it as XmlChildModel }
    }

    @Serializer(forClass = XmlProcessModel::class)
    companion object : RootProcessModelBase.BaseSerializer<XmlProcessModel>(), KSerializer<XmlProcessModel> {

        override val descriptor: SerialDescriptor get() = Builder.descriptor

        @Suppress("UNCHECKED_CAST")
        override val childModelSerializer: KSerializer<ChildProcessModel<*>>
            get() = XmlChildModel.serializer() as KSerializer<ChildProcessModel<*>>

        override fun deserialize(decoder: Decoder): XmlProcessModel {
            return XmlProcessModel(RootProcessModelBase.Builder.serializer().deserialize(decoder), true)
        }

        @Suppress("RedundantOverride")
        override fun serialize(encoder: Encoder, obj: XmlProcessModel) {
            super.serialize(encoder, obj)
        }

        @kotlin.jvm.JvmOverloads
        @kotlin.jvm.JvmStatic
        fun deserialize(reader: XmlReader, pedantic: Boolean = true): XmlProcessModel {
            return XmlProcessModel(RootProcessModelBase.Builder.deserialize(reader), pedantic)
        }

    }


    @Serializable
    @XmlSerialName(RootProcessModelBase.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    class Builder : RootProcessModelBase.Builder {

        @Transient
        val defaultPedantic: Boolean
            get() = true

        constructor(
            nodes: Collection<ProcessNode.IBuilder> = emptySet(),
            childModels: Collection<ChildProcessModel.Builder> = emptySet(),
            name: String? = null,
            handle: Long = -1L,
            owner: Principal = SYSTEMPRINCIPAL,
            roles: Set<String> = emptySet(),
            uuid: UUID? = null,
            imports: List<IXmlResultType> = emptyList(),
            exports: List<IXmlDefineType> = emptyList()) : super(nodes, childModels, name, handle, owner, roles, uuid,
                                                                 imports, exports)

        constructor(base: XmlProcessModel) : super(base)

        @Serializer(forClass = Builder::class)
        companion object : RootProcessModelBase.Builder.BaseSerializer<Builder>(), GeneratedSerializer<Builder> {
//            override val descriptor: SerialDescriptor = SerialClassDescImpl(XmlProcessModel.descriptor, Builder::class.name)

            init {
                // Some nasty hack as somehow initialisation is broken.
                val d = descriptor as SerialClassDescImpl
                for (childSerializer in  childSerializers()) {
                    d.pushDescriptor(childSerializer.descriptor)
                }
            }

            override fun builder() = Builder()

            @Suppress("RedundantOverride")
            override fun deserialize(decoder: Decoder): Builder {
                return super.deserialize(decoder)
            }

            override fun serialize(encoder: Encoder, obj: Builder) {
                XmlProcessModel.serializer().serialize(encoder, XmlProcessModel(obj))
            }

            fun deserialize(reader: XmlReader): XmlProcessModel.Builder {
                return RootProcessModelBase.Builder.deserialize(XmlProcessModel.Builder(), reader)
            }
        }
    }

    class Factory : XmlDeserializerFactory<XmlProcessModel> {

        override fun deserialize(reader: XmlReader): XmlProcessModel {
            return XmlProcessModel.deserialize(reader)
        }
    }

}

val XML_BUILDER_VISITOR = object : ProcessNode.Visitor<ProcessNode.IBuilder> {
    override fun visitStartNode(startNode: StartNode) = StartNodeBase.Builder(startNode)

    override fun visitActivity(activity: Activity) = ActivityBase.Builder(activity)

    override fun visitSplit(split: Split) = SplitBase.Builder(split)

    override fun visitJoin(join: Join) = JoinBase.Builder(join)

    override fun visitEndNode(endNode: EndNode) = EndNodeBase.Builder(endNode)
}


@Suppress("ClassName")
object XML_NODE_FACTORY : ProcessModelBase.NodeFactory<XmlProcessNode, XmlProcessNode, XmlChildModel> {

    private class Visitor(private val buildHelper: ProcessModel.BuildHelper<*, *, *, *>) : ProcessNode.BuilderVisitor<XmlProcessNode> {
        override fun visitStartNode(startNode: StartNode.Builder) = XmlStartNode(startNode, buildHelper.newOwner)

        override fun visitActivity(activity: Activity.Builder) = XmlActivity(activity, buildHelper)

        override fun visitActivity(activity: Activity.CompositeActivityBuilder) = XmlActivity(activity, buildHelper)

        override fun visitSplit(split: Split.Builder) = XmlSplit(split, buildHelper.newOwner)

        override fun visitJoin(join: Join.Builder) = XmlJoin(join, buildHelper)

        override fun visitEndNode(endNode: EndNode.Builder) = XmlEndNode(endNode, buildHelper.newOwner)
    }

    override fun invoke(baseNodeBuilder: ProcessNode.IBuilder,
                        buildHelper: ProcessModel.BuildHelper<XmlProcessNode, *, *, *>): XmlProcessNode {
        return baseNodeBuilder.visit(Visitor(buildHelper))
    }

    override fun invoke(baseChildBuilder: ChildProcessModel.Builder,
                        buildHelper: ProcessModel.BuildHelper<XmlProcessNode, *, *, *>): XmlChildModel {
        return XmlChildModel(baseChildBuilder, buildHelper)
    }

    override fun condition(text: String) = XmlCondition(text)
}
