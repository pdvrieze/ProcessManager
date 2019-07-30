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
import nl.adaptivity.process.processModel.IXmlDefineType
import nl.adaptivity.process.processModel.IXmlResultType
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.SplitBase
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.util.Identified

@Serializable(XmlSplit.Companion::class)
class XmlSplit : SplitBase, XmlProcessNode {

    @Deprecated("No need to use the buildHelper")
    constructor(builder: Split.Builder, buildHelper: XmlBuildHelper) :
        this(builder, buildHelper.newOwner)

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(builder: Split.Builder, newOwner: ProcessModel<*>) :
        super(builder, newOwner)

    @Serializer(XmlSplit::class)
    companion object: KSerializer<XmlSplit> {

        override fun deserialize(decoder: Decoder): XmlSplit {
            throw Exception("Deserializing a split directly is not possible")
        }

    }

}
