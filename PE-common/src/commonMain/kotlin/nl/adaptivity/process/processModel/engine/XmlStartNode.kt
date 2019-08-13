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

import kotlinx.serialization.Decoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.StartNodeBase
import nl.adaptivity.process.processModel.ensureExportable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable(XmlStartNode.Companion::class)
@XmlSerialName("start", ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
class XmlStartNode : StartNodeBase<XmlProcessNode, ProcessModel<XmlProcessNode>>, XmlProcessNode {

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(builder: StartNode.Builder, newOwner: ProcessModel<*>) :
        super(builder.ensureExportable(), newOwner)

    override fun builder() = StartNodeBase.Builder(this)

    @Serializer(XmlStartNode::class)
    companion object : KSerializer<XmlStartNode> {

        override fun deserialize(decoder: Decoder): XmlStartNode {
            throw Exception("Deserializing a start directly is not possible")
        }

    }

}
