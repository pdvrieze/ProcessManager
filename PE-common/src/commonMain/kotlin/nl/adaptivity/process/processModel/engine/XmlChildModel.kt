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

import foo.FakeSerializable
import foo.FakeSerializer
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.GeneratedSerializer
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.ChildProcessModel
import nl.adaptivity.process.processModel.ChildProcessModelBase
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@FakeSerializable(XmlChildModel.Companion::class)
@SerialName(ChildProcessModel.ELEMENTLOCALNAME)
@XmlSerialName(ChildProcessModel.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
class XmlChildModel : ChildProcessModelBase<XmlProcessNode>, ChildProcessModel<XmlProcessNode> {

    @Transient
    override val rootModel: XmlProcessModel
        get() = super.rootModel as XmlProcessModel

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(
        builder: ChildProcessModel.Builder,
        buildHelper: BuildHelper<XmlProcessNode, ProcessModel<XmlProcessNode>, *, *>
    ) : super(builder, buildHelper)

    override fun builder(rootBuilder: RootProcessModel.Builder): ModelBuilder {
        return ModelBuilder(rootBuilder, this)
    }

    @OptIn(InternalSerializationApi::class)
    @FakeSerializer(forClass = XmlChildModel::class)
    companion object : ChildProcessModelBase.BaseSerializer<XmlChildModel>() {

/*
        init {
            // Hack to handle invalid codegen for the generated serializer
            val d = descriptor as SerialClassDescImpl
            for (childSerializer in childSerializers()) {
                d.pushDescriptor(childSerializer.descriptor)
            }
        }
*/

        override val descriptor: SerialDescriptor
            get() = TODO("not implemented")

        @Suppress("RedundantOverride")
        override fun serialize(encoder: Encoder, value: XmlChildModel) {
            super.serialize(encoder, value)
        }

        override fun deserialize(decoder: Decoder): XmlChildModel {
            throw UnsupportedOperationException("A Child model can not be loaded independently of the parent")
        }
    }
}
