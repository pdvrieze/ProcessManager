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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.*
import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.ICompactFragment
import nl.adaptivity.xml.Namespace
import nl.adaptivity.xml.siblingsToFragment
import kotlin.reflect.KClass

@Serializer(forClass = CompactFragment::class)
object CompactFragmentSerializer : KSerializer<CompactFragment> {
    override val serialClassDesc get() = MYSERIALCLASSDESC

    override fun load(input: KInput): CompactFragment {
        val serialClassDesc = serialClassDesc
        val newInput = input.readBegin(serialClassDesc)
        if (newInput is XML.XmlInput) {

            return newInput.input.run {
                next()
                siblingsToFragment().also { newInput.readEnd(serialClassDesc) }
            }
        } else {
            var namespaces: List<Namespace> = mutableListOf()
            var content = ""

            readElements(newInput) { elem ->
                when (elem) {
                    0 -> namespaces = newInput.readSerializableElementValue(serialClassDesc, elem, Namespace.list)
                    1 -> content = newInput.readStringElementValue(serialClassDesc, elem)
                }
            }
            newInput.readEnd(serialClassDesc)
            return CompactFragment(namespaces, content)
        }
    }

    override fun save(output: KOutput, obj: CompactFragment) {
        save(output, obj as ICompactFragment)
    }

    fun save(output: KOutput, obj: ICompactFragment) {
        val serialClassDesc = serialClassDesc
        output.writeBegin(serialClassDesc).let { childOut ->
            if (childOut is XML.XmlOutput) {
                val writer = childOut.target
                for(namespace in obj.namespaces) {
                    if (writer.getPrefix(namespace.namespaceURI) == null) {
                        writer.namespaceAttr(namespace)
                    }
                }

                obj.serialize(writer)
            } else {
                childOut.writeSerializableElementValue(serialClassDesc, 0, Namespace.list,
                                                       obj.namespaces.toList())
                childOut.writeStringElementValue(serialClassDesc, 1, obj.contentString)
            }
            childOut.writeEnd(serialClassDesc)
        }
    }


    @JvmStatic
    val MYSERIALCLASSDESC = object : KSerialClassDesc {
        override val kind: KSerialClassKind get() = KSerialClassKind.CLASS

        override val name: String get() = "compactFragment"

        override fun getElementIndex(name: String): Int {
            return when (name) {
                "namespaces" -> 0
                "content"    -> 1
                else         -> KInput.UNKNOWN_NAME
            }
        }

        override fun getElementName(index: Int): String {
            return when (index) {
                0    -> "namespaces"
                1    -> "content"
                else -> throw IndexOutOfBoundsException("$index")
            }
        }

        override val associatedFieldsCount: Int get() = 2

        override fun toString(): String {
            return "compactFragment[namespaces, content]"
        }
    }
}

@Serializer(forClass = ICompactFragment::class)
object ICompactFragmentSerializer : KSerializer<ICompactFragment> {

    override val serialClassDesc: KSerialClassDesc
        get() = CompactFragmentSerializer.serialClassDesc

    override fun load(input: KInput): ICompactFragment {
        return CompactFragmentSerializer.load(input)
    }

    override fun save(output: KOutput, obj: ICompactFragment) {
        CompactFragmentSerializer.save(output, obj)
    }
}

internal inline fun <reified T : Any> kClass(): KClass<T> = T::class
