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

package nl.adaptivity.process.engine.processModel

import nl.adaptivity.process.engine.PETransformer.AbstractDataContext
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.util.Constants
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.xml.Namespace
import nl.adaptivity.xml.SimpleNamespaceContext
import nl.adaptivity.xml.XmlEvent
import java.util.*

class ProcessNodeInstanceContext(private val mProcessNodeInstance: ProcessNodeInstance<*>, private val mDefines: List<ProcessData>, private val mProvideResults: Boolean) : AbstractDataContext() {

  override fun getData(valueName: String): ProcessData? {
    when (valueName) {
      "handle"         -> return ProcessData(valueName,
                                             CompactFragment(java.lang.Long.toString(mProcessNodeInstance.getHandleValue())))
      "instancehandle" -> return ProcessData(valueName,
                                             CompactFragment(java.lang.Long.toString(mProcessNodeInstance.processInstance.handleValue)))
      "endpoint"       -> return ProcessData(valueName, createEndpoint())
      "owner"          -> return ProcessData(valueName,
                                             CompactFragment(mProcessNodeInstance.processInstance.owner.name))
    }

    for (define in mDefines) {
      if (valueName == define.name) {
        return define
      }
    }

    if (mProvideResults) {
      for (result in mProcessNodeInstance.results) {
        if (valueName == result.name) {
          return result
        }
      }
    }
    // allow for missing values in the database. If they were "defined" treat is as an empty value.
    for (resultDef in mProcessNodeInstance.node.defines) {
      if (valueName == resultDef.name) {
        return ProcessData(valueName, EMPTY_FRAGMENT)
      }
    }
    if (mProvideResults) {
      // allow for missing values in the database. If they were "defined" treat is as an empty value.
      for (resultDef in mProcessNodeInstance.node.results) {
        if (valueName == resultDef.name) {
          return ProcessData(valueName, EMPTY_FRAGMENT)
        }
      }
    }
    return null
  }

  private fun createEndpoint(): CompactFragment {
    val namespaces = SimpleNamespaceContext(Collections.singletonMap("jbi", Constants.MY_JBI_NS_STR))
    val content = StringBuilder()
    content.append("<jbi:endpointDescriptor")

    val localEndpoint = mProcessNodeInstance.processInstance.engine.localEndpoint
    content.append(" endpointLocation=\"").append(localEndpoint.endpointLocation.toString()).append('"')
    content.append(" endpointName=\"").append(localEndpoint.endpointName).append('"')
    content.append(" serviceLocalName=\"").append(localEndpoint.serviceName.localPart).append('"')
    content.append(" serviceNS=\"").append(localEndpoint.serviceName.namespaceURI).append('"')
    content.append(" />")
    return CompactFragment(namespaces, content.toString().toCharArray())
  }

  override fun resolveDefaultValue(): List<XmlEvent> {
    throw UnsupportedOperationException("There is no default in this context")
  }

  companion object {

    private val EMPTY_FRAGMENT = CompactFragment(emptyList<Namespace>(), CharArray(0))
  }

}