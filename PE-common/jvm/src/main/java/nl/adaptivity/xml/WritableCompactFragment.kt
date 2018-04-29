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

package nl.adaptivity.xml

import nl.adaptivity.io.Writable
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.ICompactFragment

import java.io.IOException
import java.io.Writer


/**
 * Created by pdvrieze on 27/11/15.
 */
class WritableCompactFragment private constructor(private val data: ICompactFragment,
                                                  dummy: Boolean) : ICompactFragment, Writable {

    override val isEmpty: Boolean
        get() = data.isEmpty

    override val namespaces: IterableNamespaceContext
        get() = data.namespaces

    override val content: CharArray
        get() = data.content

    override val contentString: String
        get() = data.contentString

    constructor(namespaces: Iterable<Namespace>, content: CharArray) : this(CompactFragment(namespaces, content), false)

    constructor(string: String) : this(CompactFragment(string), false) {}

    constructor(orig: ICompactFragment) : this(CompactFragment(orig.namespaces, orig.contentString), false) {}

    @Throws(IOException::class)
    override fun writeTo(destination: Writer) {
        destination.write(content)
    }

    override fun serialize(out: XmlWriter) {
        data.serialize(out)
    }
}
