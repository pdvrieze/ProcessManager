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

package nl.adaptivity.process.processModel

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.xmlutil.util.XMLFragmentStreamReader


/**
 * This class can contain xml content. It allows it to be transformed, and input/output
 * Created by pdvrieze on 30/10/15.
 */
@OptIn(XmlUtilInternal::class)
abstract class XMLContainer(
    fragment: ICompactFragment
) {
    val fragment: CompactFragment = CompactFragment(fragment)

    constructor(namespaces: Iterable<Namespace>, content: CharArray) :
        this(CompactFragment(namespaces, content))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XMLContainer

        return fragment == other.fragment
    }

    override fun hashCode(): Int {
        return fragment.hashCode()
    }

}
