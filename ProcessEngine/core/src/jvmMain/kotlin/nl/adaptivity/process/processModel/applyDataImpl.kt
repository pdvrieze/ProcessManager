/*
 * Copyright (c) 2019.
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

import nl.adaptivity.process.engine.PETransformer
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.impl.dom.NodeList
import nl.adaptivity.process.engine.impl.dom.XPathConstants
import nl.adaptivity.process.engine.impl.dom.toDocumentFragment
import nl.adaptivity.process.processModel.IPlatformXmlResultType
import nl.adaptivity.process.processModel.path
import nl.adaptivity.util.DomUtil
import nl.adaptivity.xmlutil.SimpleNamespaceContext
import nl.adaptivity.xmlutil.siblingsToFragment
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

actual fun IPlatformXmlResultType.applyData(payload: ICompactFragment?): ProcessData {
    val xPath = this.xPath
    // shortcircuit missing path
    if (payload == null) {
        return ProcessData(getName(), CompactFragment(""))
    }
    val processData = if (getPath() == null || "." == getPath()) {
        ProcessData(getName(), payload)
    } else {
        ProcessData(
            getName(),
            DomUtil.nodeListToFragment(
                xPath!!.evaluate(
                    payload.toDocumentFragment(),
                    XPathConstants.NODESET
                                ) as NodeList
                                      )
                   )

        // TODO check whether this can use work by CompactFragment implementing InputSource instead
    }
    val content = content
    if (content?.isNotEmpty() ?: false) {
        val transformer = PETransformer.create(SimpleNamespaceContext.from(originalNSContext), processData)
        val reader = transformer.createFilter(bodyStreamReader)

        if (reader.hasNext()) reader.next() // Initialise the reader

        val transformed = reader.siblingsToFragment()
        return ProcessData(getName(), transformed)
    } else {
        return processData
    }
}
