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

package nl.adaptivity.process.processModel


import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.engine.XmlCondition
import nl.adaptivity.xmlutil.QName


interface Condition {

    val condition: String

    val label: String?

    companion object {

        const val ELEMENTLOCALNAME = "condition"
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)
    }

    object Serializer: KSerializer<Condition> {
        private val delegate =  XmlCondition.Companion
        override val descriptor: SerialDescriptor get() = delegate.descriptor

        override fun deserialize(decoder: Decoder): Condition {
            return delegate.deserialize(decoder)
        }

        override fun serialize(encoder: Encoder, value: Condition) {
            delegate.serialize(encoder, value as? XmlCondition ?: XmlCondition(value.condition, value.label))
        }
    }

}
