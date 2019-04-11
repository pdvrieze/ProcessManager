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

import kotlinx.serialization.Transient
import net.devrieze.util.Handle
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.util.multiplatform.JvmField
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.multiplatform.toUUID
import nl.adaptivity.xmlutil.util.SimpleXmlDeserializable
import nl.adaptivity.xmlutil.*

@XmlDeserializer(ProcessModelRef.Factory::class)
class ProcessModelRef<NodeT : ProcessNode, out ObjectT : RootProcessModel<NodeT>>
constructor(override var name: String?,
            var handle: Long,
            override var uuid: UUID?)
    : IProcessModelRef<NodeT, ObjectT>, XmlSerializable, SimpleXmlDeserializable {

    @Transient
    override val elementName: QName
        get() = ELEMENTNAME


    override val handleValue: Long get() = handle

    constructor() : this(null, -1L, null)

    constructor(name: String?, handle: Handle<RootProcessModel<NodeT>>, uuid: UUID?) : this(name, handle.handleValue, uuid)

    constructor(source: IProcessModelRef<NodeT, ObjectT>) : this(source.name, source.handleValue, source.uuid)

    override fun deserializeChild(reader: XmlReader) = false

    override fun deserializeChildText(elementText: CharSequence) = false

    override fun deserializeAttribute(attributeNamespace: String?,
                                      attributeLocalName: String,
                                      attributeValue: String): Boolean {
        when (attributeLocalName) {
            "name"   -> name = attributeValue
            "handle" -> handle = attributeValue.toLong()
            "uuid"   -> uuid = attributeValue.toUUID()
            else     -> return false
        }
        return true
    }

    override fun onBeforeDeserializeChildren(reader: XmlReader) = Unit


    override fun serialize(out: XmlWriter) {
        out.smartStartTag(elementName) {
            writeAttribute("name", name)
            writeAttribute("handle", handle)
            writeAttribute("uuid", uuid?.toString())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessModelRef<*,*>) return false

        if (name != other.name) return false
        if (handle != other.handle) return false
        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + handle.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        return result
    }

    class Factory : XmlDeserializerFactory<ProcessModelRef<*,*>> {

        override fun deserialize(reader: XmlReader): ProcessModelRef<XmlProcessNode,XmlProcessModel> {
            // The type parameters here are just dummies as Kotlin insists on having parameters
            return ProcessModelRef.deserialize<XmlProcessNode, XmlProcessModel>(reader)
        }
    }

    companion object {

        const val ELEMENTLOCALNAME = "processModel"

        @JvmField
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

        @kotlin.jvm.JvmStatic
        fun <NodeT : ProcessNode, ObjectT : RootProcessModel<NodeT>> get(src: IProcessModelRef<NodeT, ObjectT>): ProcessModelRef<NodeT, ObjectT> {
            return src as? ProcessModelRef ?: ProcessModelRef(src)
        }

        @kotlin.jvm.JvmStatic
        fun <NodeT : ProcessNode, ObjectT : RootProcessModel<NodeT>> deserialize(reader: XmlReader): ProcessModelRef<NodeT, ObjectT> {
            return ProcessModelRef<NodeT, ObjectT>().deserializeHelper(reader)
        }

    }

}
