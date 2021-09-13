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
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.ChildProcessModel
import nl.adaptivity.process.processModel.ChildProcessModelBase
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.util.multiplatform.name
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable(XmlChildModel.Companion::class)
class XmlChildModel : ChildProcessModelBase<XmlProcessNode>, ChildProcessModel<XmlProcessNode> {

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
    companion object : KSerializer<XmlChildModel> {
        private val delegateSerializer = SerialDelegate.serializer()

        @OptIn(ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor =
            SerialDescriptor(XmlChildModel::class.name, delegateSerializer.descriptor)

        @Suppress("RedundantOverride")
        override fun serialize(encoder: Encoder, value: XmlChildModel) {
            delegateSerializer.serialize(encoder, SerialDelegate(value))
        }

        override fun deserialize(decoder: Decoder): XmlChildModel {
            throw UnsupportedOperationException("A Child model can not be loaded independently of the parent")
        }
    }
}
