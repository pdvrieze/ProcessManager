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
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.util.SerialClassDescImpl
import nl.adaptivity.util.multiplatform.name
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable(XmlChildModel.Companion::class)
@SerialName(ChildProcessModel.ELEMENTLOCALNAME)
@XmlSerialName(ChildProcessModel.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
class XmlChildModel : ChildProcessModelBase<XmlProcessNode>, ChildProcessModel<XmlProcessNode>, XmlModelCommon {

    @Transient
    override val rootModel: XmlProcessModel
        get() = super.rootModel as XmlProcessModel

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(builder: ChildProcessModel.Builder,
                buildHelper: BuildHelper<XmlProcessNode, ProcessModel<XmlProcessNode>, *, *>) : super(builder, buildHelper)

    override fun builder(rootBuilder: RootProcessModel.Builder): XmlChildModel.Builder {
        return Builder(rootBuilder as XmlProcessModel.Builder, this)
    }

    @Serializable
    open class Builder : ChildProcessModelBase.Builder, XmlModelCommon.Builder {

        @Transient
        override val rootBuilder: XmlProcessModel.Builder
            get() = super.rootBuilder as XmlProcessModel.Builder

        protected constructor() : super()

        constructor(rootBuilder: XmlProcessModel.Builder,
                    childId: String? = null,
                    nodes: Collection<XmlProcessNode.Builder> = emptyList(),
                    imports: Collection<IXmlResultType> = emptyList(),
                    exports: Collection<IXmlDefineType> = emptyList()) : super(rootBuilder, childId, nodes, imports,
                                                                               exports)

        constructor(rootBuilder: XmlProcessModel.Builder, base: ChildProcessModel<*>) :
            this(rootBuilder,
                 base.id,
                 base.modelNodes.map { it.visit(XML_BUILDER_VISITOR) },
                 base.imports,
                 base.exports)

        @Serializer(forClass = Builder::class)
        companion object : ChildProcessModelBase.Builder.BaseSerializer<Builder>() {
            override val descriptor: SerialDescriptor = SerialClassDescImpl(XmlChildModel.descriptor,
                                                                            Builder::class.name)

            override fun builder(): Builder {
                return Builder()
            }


            @Suppress("RedundantOverride") // Without this serialization will generate the code
            override fun deserialize(decoder: Decoder): Builder {
                return super.deserialize(decoder)
            }

            override fun serialize(encoder: Encoder, obj: Builder) {
                val rootModel = XmlProcessModel.Builder().apply { childModels.add(obj) }.build()
                XmlChildModel.serialize(encoder, rootModel.childModels.single())
                throw UnsupportedOperationException("Cannot be independently saved")
            }
        }
    }

    @Serializer(forClass = XmlChildModel::class)
    companion object : ChildProcessModelBase.BaseSerializer<XmlChildModel>(), GeneratedSerializer<XmlChildModel> {

        init {
            // Hack to handle invalid codegen for the generated serializer
            val d = descriptor as SerialClassDescImpl
            for(childSerializer in childSerializers()) {
                d.pushDescriptor(childSerializer.descriptor)
            }
        }

        @Suppress("RedundantOverride")
        override fun serialize(encoder: Encoder, obj: XmlChildModel) {
            super.serialize(encoder, obj)
        }

        override fun deserialize(decoder: Decoder): XmlChildModel {
            throw UnsupportedOperationException("A Child model can not be loaded independently of the parent")
        }
    }
}
