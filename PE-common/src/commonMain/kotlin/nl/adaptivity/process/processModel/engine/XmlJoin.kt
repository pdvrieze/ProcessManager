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
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.deserializeHelper

@Serializable(XmlJoin.Companion::class)
class XmlJoin : JoinBase<XmlProcessNode, ProcessModel<XmlProcessNode>>, XmlProcessNode {

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(builder: Join.Builder, buildHelper: BuildHelper<*,*,*,*>)
        : super(builder, buildHelper)

    @Serializer(XmlJoin::class)
    companion object: KSerializer<XmlJoin> {

        @Throws(XmlException::class)
        internal fun deserialize(reader: XmlReader,
                        buildHelper: XmlBuildHelper): XmlJoin {
            return XmlJoin(deserialize(reader), buildHelper)
        }

        @Throws(XmlException::class)
        fun deserialize(reader: XmlReader): JoinBase.Builder {
            return JoinBase.Builder().deserializeHelper(reader)
        }

        override fun deserialize(decoder: Decoder): XmlJoin {
            throw Exception("Deserializing an end node directly is not possible")
        }

    }

}
