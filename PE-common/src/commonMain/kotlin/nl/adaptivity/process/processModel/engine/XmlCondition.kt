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
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.Condition
import nl.adaptivity.serialutil.DelegatingSerializer
import nl.adaptivity.serialutil.withName
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable(/*XmlCondition.Companion::class*/)
@XmlSerialName(Condition.ELEMENTLOCALNAME, Engine.NAMESPACE, Engine.NSPREFIX)
class XmlCondition(override val condition: String, override val label: String? = null) : Condition {

    override fun toString(): String {
        return "XmlCondition(condition='$condition')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XmlCondition) return false

        if (condition != other.condition) return false

        return true
    }

    override fun hashCode(): Int {
        return condition.hashCode()
    }

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
