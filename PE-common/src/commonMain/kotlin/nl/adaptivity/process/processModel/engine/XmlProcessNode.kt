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
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.processModel.ProcessNode


/**
 * Fix compilation by converting it properly to Kotlin.
 */
interface XmlProcessNode : ProcessNode<XmlProcessNode, XmlModelCommon> {

    interface Builder : ProcessNode.IBuilder<XmlProcessNode, XmlModelCommon> {

        override fun build(buildHelper: BuildHelper<XmlProcessNode, XmlModelCommon>): XmlProcessNode
    }

    override fun builder(): Builder

    @Serializer(forClass = XmlProcessNode::class)
    companion object: KSerializer<XmlProcessNode> {
        override val descriptor: SerialDescriptor = PolymorphicSerializer(XmlProcessNode::class).descriptor

        fun serializer(): KSerializer<XmlProcessNode> = this

        override fun deserialize(decoder: Decoder): XmlProcessNode {
            throw SerializationException("XmlProcessNodes cannot be actually deserialized, only instances can")
        }

        override fun serialize(encoder: Encoder, obj: XmlProcessNode) {
            throw SerializationException("XmlProcessNodes cannot be actually deserialized, only instances can")
        }
    }
}
