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
import nl.adaptivity.process.processModel.*

@FakeSerializable(XmlEndNode.Companion::class)
class XmlEndNode : EndNodeBase, XmlProcessNode {

    @Suppress("ConvertSecondaryConstructorToPrimary") // For serialization
    constructor(
        builder: EndNode.Builder,
        newOwner: ProcessModel<*>,
        otherNodes: Iterable<ProcessNode.Builder>
               ) :
        super(builder.ensureExportable(), newOwner, otherNodes)

    @FakeSerializer(XmlEndNode::class)
    companion object : KSerializer<XmlEndNode> {

        override fun deserialize(decoder: Decoder): XmlEndNode {
            throw Exception("Deserializing an end node directly is not possible")
        }

        override val descriptor: SerialDescriptor
            get() = TODO("not implemented")

        override fun serialize(encoder: Encoder, value: XmlEndNode) {
            TODO("not implemented")
        }
    }

}
