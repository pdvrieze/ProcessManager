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

package nl.adaptivity.process.engine.processModel

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.engine.PETransformer.AbstractDataContext
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.processModel.name
import nl.adaptivity.process.util.Constants
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.xmlutil.*
import java.util.*

actual class ProcessNodeInstanceContext actual constructor(private val processNodeInstance: ProcessNodeInstance<*>, private val defines: List<ProcessData>, private val provideResults: Boolean, private val localEndpoint: EndpointDescriptor) : AbstractDataContext() {

  override fun getData(valueName: String): ProcessData? {
    when (valueName) {
      "handle"         -> return ProcessData(valueName, CompactFragment(
          processNodeInstance.getHandleValue().toString()))
      "instancehandle" -> return ProcessData(valueName, CompactFragment(
          processNodeInstance.hProcessInstance.handleValue.toString()))
      "endpoint"       -> return ProcessData(valueName, createEndpoint())
      "owner"          -> return ProcessData(valueName, CompactFragment(
          processNodeInstance.owner.name.xmlEncode()))
    }

    defines.firstOrNull { valueName == it.name }?.let { return it }

    if (provideResults) {
      processNodeInstance.results
        .firstOrNull { valueName == it.name }
        ?.let { return it }
    }
    // allow for missing values in the database. If they were "defined" treat is as an empty value.
    processNodeInstance.node.defines
      .firstOrNull { valueName == it.name }
      ?.let { return ProcessData(valueName, EMPTY_FRAGMENT) }

    if (provideResults) {
      // allow for missing values in the database. If they were "defined" treat is as an empty value.
      processNodeInstance.node.results
        .firstOrNull { valueName == it.getName() }
        ?.let { return ProcessData(valueName, EMPTY_FRAGMENT) }
    }
    return null
  }

  private fun createEndpoint(): CompactFragment {
    val namespaces = SimpleNamespaceContext(Collections.singletonMap("jbi", Constants.MY_JBI_NS_STR))
    val content = StringBuilder()
    content.append("<jbi:endpointDescriptor")

    content.append(" endpointLocation=\"").append(localEndpoint.endpointLocation.toString()).append('"')
    content.append(" endpointName=\"").append(localEndpoint.endpointName).append('"')
    localEndpoint.serviceName?.let { serviceName ->
      content.append(" serviceLocalName=\"").append(serviceName.localPart).append('"')
      content.append(" serviceNS=\"").append(serviceName.namespaceURI).append('"')
    }
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
