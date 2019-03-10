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

package nl.adaptivity.process.processModel.serialization

import kotlinx.serialization.*
import nl.adaptivity.process.processModel.PredecessorInfo
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.serialization.decodeStructure
import nl.adaptivity.xmlutil.serialization.writeStructure

internal class ConditionStringSerializer(val keySerializer: KSerializer<Identifier>, val valueSerializer: KSerializer<String>): KSerializer<MutableMap<Identifier, String?>> {



    private val predecessorSerializer = PredecessorInfo.serializer()
    override val descriptor: SerialDescriptor get() = predecessorSerializer.list.descriptor

    override fun patch(decoder: Decoder, old: MutableMap<Identifier, String?>): MutableMap<Identifier, String?> {
        val desc = descriptor
        decoder.decodeStructure(desc) {
            var size = -1
            loop@ while (true) {
                val index = decodeElementIndex(desc)
                when (index) {
                    0                -> size = decodeIntElement(desc, 0)
                    CompositeDecoder.READ_DONE -> break@loop
                    CompositeDecoder.READ_ALL  -> {
                        size = decodeIntElement(desc, 0)
                        for (i in 1..size) {
                            val e = decodeSerializableElement(desc, i, predecessorSerializer)
                            old[Identifier(e.id)] = e.condition
                        }
                    }
                    else             -> {
                        val e = decodeSerializableElement(desc, index, predecessorSerializer)
                        old[Identifier(e.id)] = e.condition
                    }

                }
            }
        }
        return old
    }

    override fun deserialize(decoder: Decoder): MutableMap<Identifier, String?> {
        return patch(decoder, mutableMapOf())
    }

    override fun serialize(encoder: Encoder, obj: MutableMap<Identifier, String?>) {
        val desc = descriptor
        encoder.writeStructure(desc) {
            encodeIntElement(desc, 0, obj.size)

            var idx = 1
            for (elem in obj) {
                encodeSerializableElement(desc, idx, predecessorSerializer, PredecessorInfo(elem.key.id, elem.value))
                idx++
            }
        }
    }
}