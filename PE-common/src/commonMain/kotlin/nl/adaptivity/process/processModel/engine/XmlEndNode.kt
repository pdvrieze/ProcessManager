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
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.EndNodeBase
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.deserializeHelper

@Serializable(XmlEndNode.Companion::class)
class XmlEndNode : EndNodeBase, XmlProcessNode {

    @Suppress("ConvertSecondaryConstructorToPrimary") // For serialization
    constructor(builder: EndNode.Builder, newOwner: ProcessModel<*>) :
        super(builder, newOwner)

    override fun builder() = Builder(this)

    @Serializer(XmlEndNode::class)
    companion object: KSerializer<XmlEndNode> {

        @Throws(XmlException::class)
        fun deserialize(reader: XmlReader): XmlEndNode.Builder {
            return Builder().deserializeHelper(reader)
        }

        override fun deserialize(decoder: Decoder): XmlEndNode {
            throw Exception("Deserializing an end node directly is not possible")
        }
    }

    @Serializable
    class Builder : EndNodeBase.Builder, XmlProcessNode.Builder {

        constructor() : this(id = null)


        constructor(predecessor: Identified? = null,
                    id: String? = null,
                    label: String? = null,
                    x: Double = Double.NaN,
                    y: Double = Double.NaN,
                    defines: Collection<IXmlDefineType> = emptyList(),
                    results: Collection<IXmlResultType> = emptyList(),
                    multiInstance: Boolean = false)
            : super(id, predecessor, label, defines, results, x, y, multiInstance)


        constructor(node: EndNode) : super(node)

        fun build(newOwner: ProcessModel<*>): XmlEndNode {
            return XmlEndNode(this, newOwner)
        }
    }

}
