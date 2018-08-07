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

import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.KSerializer
import kotlinx.serialization.internal.ArrayListClassDesc
import kotlinx.serialization.internal.SIZE_INDEX
import nl.adaptivity.process.processModel.Condition
import nl.adaptivity.process.processModel.PredecessorInfo
import nl.adaptivity.process.processModel.engine.XmlCondition
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.serialization.writeBegin


internal class ConditionSerializer(ks: KSerializer<*>, vs: KSerializer<*>): KSerializer<MutableMap<Identifier, Condition?>> {
    override val serialClassDesc: KSerialClassDesc get() = ArrayListClassDesc
    private val predecessorSerializer = PredecessorInfo.serializer()

    override fun update(input: KInput, old: MutableMap<Identifier, Condition?>): MutableMap<Identifier, Condition?> {
        val desc = serialClassDesc
        val input = input.readBegin(desc)
        loop@while(true) {
            val index = input.readElement(desc)
            when (index) {
                0                -> input.readIntElementValue(desc, 0)
                KInput.READ_DONE -> break@loop
                KInput.READ_ALL  -> {
                    val size = input.readIntElementValue(desc, 0)
                    for (i in 1 .. size) {
                        val e = input.readSerializableElementValue(desc, i, predecessorSerializer)
                        old[Identifier(e.id)] = e.condition?.let(::XmlCondition)
                    }
                }
                else             -> {
                    val e = input.readSerializableElementValue(desc, index, predecessorSerializer)
                }

            }
        }
        input.readEnd(desc)
        return old
    }

    override fun load(input: KInput): MutableMap<Identifier, Condition?> {
        return update(input, mutableMapOf())
    }

    override fun save(output: KOutput, obj: MutableMap<Identifier, Condition?>) {
        val desc = serialClassDesc
        output.writeBegin(desc) {
            if (writeElement(serialClassDesc, SIZE_INDEX))
                writeIntValue(obj.size)

            var idx = 1
            for (elem in obj) {
                writeSerializableElementValue(desc, idx, predecessorSerializer, PredecessorInfo(elem.key.id,
                                                                                                elem.value?.condition))
                idx++
            }
        }
    }
}

