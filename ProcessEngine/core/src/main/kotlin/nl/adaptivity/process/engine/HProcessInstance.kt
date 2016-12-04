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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine

import net.devrieze.util.Handle
import net.devrieze.util.Handles
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.xml.*
import javax.xml.namespace.QName

@XmlDeserializer(HProcessInstance.Factory::class)
class HProcessInstance<T : ProcessTransaction>(handle: Handle<out ProcessInstance>) : XmlHandle<ProcessInstance>(handle) {

  class Factory : XmlDeserializerFactory<HProcessInstance<*>> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): HProcessInstance<*> {
      return HProcessInstance.deserialize(reader)
    }
  }

  constructor() : this(Handles.getInvalid())

  override val elementName: QName
    get() = ELEMENTNAME

  override fun equals(obj: Any?): Boolean {
    return obj === this || obj is HProcessInstance<*> && handleValue == obj.handleValue
  }

  override fun hashCode(): Int {
    return handleValue.toInt()
  }

  companion object {

    const val ELEMENTLOCALNAME = "instanceHandle"
    
    @JvmStatic
    val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

    @Throws(XmlException::class)
    private fun deserialize(xmlReader: XmlReader): HProcessInstance<*> {
      // For some reason a transactiontype is needed here even though it is dropped
      return HProcessInstance<ProcessDBTransaction>().deserializeHelper(xmlReader)
    }
  }

}
