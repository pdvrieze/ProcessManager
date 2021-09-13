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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import net.devrieze.util.Named
import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.serialutil.decodeElements
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.ICompactFragmentSerializer
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

/** Class to represent data attached to process instances.  */
@Serializable(with = ProcessData.Companion::class)
@XmlSerialName(ProcessData.ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
class ProcessData
constructor(
    override val name: String?,
    val content: ICompactFragment
) : Named {

    val contentStream: XmlReader get() = content.getXmlReader()

    override fun copy(name: String?): ProcessData = copy(name, content)

    fun copy(name: String?, value: ICompactFragment): ProcessData = ProcessData(name, value)

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

    override fun toString(): String {
        return "ProcessData($name=$content)"
    }

    @Serializable
    @XmlSerialName(ELEMENTLOCALNAME, ProcessConsts.Engine.NAMESPACE, ProcessConsts.Engine.NSPREFIX)
    private class SerialAnnotationHelper

    companion object: KSerializer<ProcessData> {

        const val ELEMENTLOCALNAME = "value"
        val ELEMENTNAME = QName(ProcessConsts.Engine.NAMESPACE, ELEMENTLOCALNAME, ProcessConsts.Engine.NSPREFIX)

        fun missingData(name: String): ProcessData {
            return ProcessData(name, CompactFragment(""))
        }

        @OptIn(XmlUtilInternal::class, ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ProcessData") {
            annotations = SerialAnnotationHelper.serializer().descriptor.annotations
            element<String>("name")
            element<ICompactFragment>("content")
        }

        override fun deserialize(decoder: Decoder): ProcessData {
            var name: String? = null
            lateinit var content: ICompactFragment
            decoder.decodeStructure(descriptor) {
                decodeElements(this) { i ->
                    when (i) {
                        0 -> name = decodeSerializableElement(descriptor, 0, String.serializer().nullable)
                        1 -> content = when (this) {
                            is XML.XmlInput -> this.input.siblingsToFragment()
                            else            -> decodeSerializableElement(descriptor, 1, ICompactFragmentSerializer)
                        }
                    }

                }

            }
            return ProcessData(name, content)
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun serialize(encoder: Encoder, value: ProcessData) {
            val newOutput = encoder.beginStructure(descriptor)
            newOutput.encodeNullableSerializableElement(descriptor, 0, String.serializer(), value.name)

            if (newOutput is XML.XmlOutput) {
                value.content.serialize(newOutput.target)
            } else {
                newOutput.encodeSerializableElement(descriptor, 1, ICompactFragmentSerializer, value.content)
            }
            newOutput.endStructure(descriptor)
        }


    }
}
