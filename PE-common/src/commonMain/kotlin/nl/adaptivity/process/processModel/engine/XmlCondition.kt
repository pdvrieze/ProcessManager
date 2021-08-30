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

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.Condition
import nl.adaptivity.serialutil.DelegatingSerializer
import nl.adaptivity.serialutil.withName
import nl.adaptivity.xmlutil.*


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable(XmlCondition.Companion::class)
class XmlCondition(override val condition: String) : Condition {

    companion object : DelegatingSerializer<XmlCondition, String>(String.serializer()) {

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("condition", PrimitiveKind.STRING)

        override fun fromDelegate(delegate: String): XmlCondition {
            return XmlCondition(delegate)
        }

        override fun XmlCondition.toDelegate(): String {
            return condition
        }
    }
}
