/*
 * Copyright (c) 2016.
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
import net.devrieze.util.ComparableHandle
import net.devrieze.util.getInvalidHandle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@XmlSerialName(HProcessInstance.ELEMENTLOCALNAME, Engine.NAMESPACE, Engine.NSPREFIX)
@XmlDeserializer(HProcessInstance.Factory::class)
class HProcessInstance : XmlHandle<@ContextualSerialization SecureObject<@ContextualSerialization ProcessInstance>> {

    constructor(handle: ComparableHandle<SecureObject<ProcessInstance>>) : super(handle)

    class Factory : XmlDeserializerFactory<HProcessInstance> {

        @Throws(XmlException::class)
        override fun deserialize(reader: XmlReader): HProcessInstance {
            return HProcessInstance.deserialize(reader)
        }
    }

    constructor() : this(getInvalidHandle<SecureObject<ProcessInstance>>())

    override val elementName: QName
        get() = ELEMENTNAME

    override fun equals(other: Any?): Boolean {
        return other === this || other is HProcessInstance && handleValue == other.handleValue
    }

    override fun hashCode(): Int {
        return handleValue.toInt()
    }

    companion object {

        const val ELEMENTLOCALNAME = "instanceHandle"

        @JvmStatic
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

        @Throws(XmlException::class)
        private fun deserialize(xmlReader: XmlReader): HProcessInstance {
            return XML.parse(xmlReader, serializer())
        }
    }

}

@Serializable
@XmlSerialName(HProcessInstance.ELEMENTLOCALNAME, Engine.NAMESPACE, Engine.NSPREFIX)
internal class HProcessInstanceSerialHelper(@XmlValue(true) val handle: Long)
