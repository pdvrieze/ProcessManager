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

package nl.adaptivity.process.engine

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringSerializer
import net.devrieze.util.Named
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

/** Class to represent data attached to process instances.  */
@Serializable
@XmlSerialName(ProcessData.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
class ProcessData constructor(@XmlElement(false) override val name: String?,
                              @Serializable(with = ICompactFragmentSerializer::class)
                              val content: ICompactFragment) : Named, XmlSerializable {

    @Transient
    val contentStream: XmlReader
        get() = content.getXmlReader()

    override fun copy(name: String?): ProcessData = copy(name, content)

    fun copy(name: String?, value: ICompactFragment): ProcessData = ProcessData(name, value)

    override fun serialize(out: XmlWriter) {
        out.smartStartTag(ELEMENTNAME) {
            writeAttribute("name", name)
            content.serialize(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProcessData

        if (name != other.name) return false
        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + content.hashCode()
        return result
    }

    @Serializer(forClass = ProcessData::class)
    companion object {

        const val ELEMENTLOCALNAME = "value"
        val ELEMENTNAME = QName(ProcessConsts.Engine.NAMESPACE, ELEMENTLOCALNAME, ProcessConsts.Engine.NSPREFIX)

        fun missingData(name: String): ProcessData {
            return ProcessData(name, CompactFragment(""))
        }

        fun deserialize(reader: XmlReader): ProcessData {
            return XML.parse(reader)
//            return ProcessData(null, CompactFragment("")).deserializeHelper(reader)
        }

        override fun load(input: KInput): ProcessData {
            var name: String? = null
            lateinit var content: ICompactFragment
            input.readBegin(serialClassDesc) {
                val i = readElement(serialClassDesc)
                loop@ while (i != KInput.READ_DONE) {
                    @Suppress("UNCHECKED_CAST")
                    when (i) {
                        KInput.READ_ALL -> throw UnsupportedOperationException()
                        0               -> name = readNullableSerializableElementValue(serialClassDesc, 0,
                                                                                       StringSerializer as KSerialLoader<String?>)
                        1               -> content = when (this) {
                            is XML.XmlInput -> this.input.siblingsToFragment()
                            else            -> readSerializableElementValue(serialClassDesc, 1,
                                                                            ICompactFragmentSerializer)
                        }
                    }
                }
            }
            return ProcessData(name, content)
        }

        override fun save(output: KOutput, obj: ProcessData) {
            val newOutput = output.writeBegin(serialClassDesc)
            if (newOutput.writeElement(serialClassDesc, 0))
                newOutput.writeNullableSerializableValue(StringSerializer, obj.name)
            if (newOutput is XML.XmlOutput) {
                obj.content.serialize(newOutput.target)
            } else {
                if (newOutput.writeElement(serialClassDesc, 1))
                    newOutput.writeSerializableValue(ICompactFragmentSerializer, obj.content)
            }
            newOutput.writeEnd(serialClassDesc)
        }


    }
}
