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

import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.xml.*

import javax.xml.namespace.QName

@XmlDeserializer(HProcessNodeInstance.Factory::class)
class HProcessNodeInstance : XmlHandle<ProcessNodeInstance<*>> {

  class Factory : XmlDeserializerFactory<HProcessNodeInstance> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): HProcessNodeInstance {
      return HProcessNodeInstance.deserialize(reader)
    }
  }

  constructor() : super(-1) {
  }

  constructor(handle: Long) : super(handle) {
  }

  override val elementName: QName
    get() = ELEMENTNAME

  override fun equals(obj: Any?): Boolean {
    return obj === this || obj is HProcessNodeInstance && handleValue == obj.handleValue
  }

  override fun hashCode(): Int {
    return handleValue.toInt()
  }

  companion object {

    const val ELEMENTLOCALNAME = "nodeInstanceHandle"
    val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)

    @Throws(XmlException::class)
    private fun deserialize(xmlReader: XmlReader): HProcessNodeInstance {
      return HProcessNodeInstance().deserializeHelper<nl.adaptivity.process.engine.HProcessNodeInstance>(xmlReader)
    }
  }

}
