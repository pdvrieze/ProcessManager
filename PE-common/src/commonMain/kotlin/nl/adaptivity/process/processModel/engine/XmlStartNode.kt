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
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.StartNodeBase

@Serializable(XmlStartNode.Companion::class)
class XmlStartNode : StartNodeBase<XmlProcessNode, XmlModelCommon>, XmlProcessNode {

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(builder: StartNode.Builder, newOwner: ProcessModel<*>) :
        super(builder, newOwner)

    override fun builder() = Builder(this)

    @SerialName("start")
    @Serializable
    class Builder : StartNodeBase.Builder, XmlProcessNode.Builder {

        constructor()

        constructor(base: StartNode) : super(base)

        fun build(buildHelper: XmlBuildHelper): XmlStartNode {
            return XmlStartNode(this, buildHelper.newOwner)
        }
    }

    @Serializer(XmlStartNode::class)
    companion object: KSerializer<XmlStartNode> {
/*
        val parentSerializer = StartNodeBase.serializer(this, XmlProcessModel.serializer()) as KSerializer<XmlStartNode>

        override fun serialize(encoder: kotlinx.serialization.Encoder,
                               obj: XmlStartNode) {
            parentSerializer.serialize(encoder, obj)
        }
*/

        override fun deserialize(decoder: Decoder): XmlStartNode {
            throw Exception("Deserializing a start directly is not possible")
        }

    }

}