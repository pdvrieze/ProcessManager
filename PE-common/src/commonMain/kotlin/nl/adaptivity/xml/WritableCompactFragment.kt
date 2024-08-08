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

package nl.adaptivity.xml

import nl.adaptivity.xmlutil.IterableNamespaceContext
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.util.ICompactFragment

/**
 * Created by pdvrieze on 27/11/15.
 */
expect class WritableCompactFragment private constructor(
    data: ICompactFragment,
    dummy: Boolean
) : ICompactFragment {
    constructor(namespaces: Iterable<Namespace>, content: CharArray)
    constructor(string: String)
    constructor(orig: ICompactFragment)

    override fun getXmlReader(): XmlReader
    override val content: CharArray
    override val contentString: String
    override val isEmpty: Boolean
    override val namespaces: IterableNamespaceContext
    override fun serialize(out: XmlWriter)
}
