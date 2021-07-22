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
import kotlinx.serialization.encoding.Decoder
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.processModel.*
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable(XmlSplit.Companion::class)
@XmlSerialName("split", ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
class XmlSplit : SplitBase, XmlProcessNode {

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(
        builder: Split.Builder,
        newOwner: ProcessModel<*>,
        otherNodes: Iterable<ProcessNode.Builder>
               ) :
        super(builder.ensureExportable(), newOwner, otherNodes)

    @Serializer(XmlSplit::class)
    companion object : KSerializer<XmlSplit> {

        override fun deserialize(decoder: Decoder): XmlSplit {
            throw Exception("Deserializing a split directly is not possible")
        }

    }

}
