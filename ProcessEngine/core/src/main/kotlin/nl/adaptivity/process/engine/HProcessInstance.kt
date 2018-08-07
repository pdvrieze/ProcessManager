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

import net.devrieze.util.ComparableHandle
import net.devrieze.util.getInvalidHandle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.xmlutil.*
import javax.xml.namespace.QName

@XmlDeserializer(HProcessInstance.Factory::class)
class HProcessInstance(handle: ComparableHandle<SecureObject<ProcessInstance>>) : XmlHandle<SecureObject<ProcessInstance>>(handle) {

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
      // For some reason a transactiontype is needed here even though it is dropped
      return HProcessInstance().deserializeHelper(xmlReader)
    }
  }

}
