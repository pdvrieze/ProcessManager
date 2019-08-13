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

import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

/**
 * Created by pdvrieze on 27/11/15.
 */
actual class WritableCompactFragment private actual constructor(
    private val data: ICompactFragment,
    dummy: Boolean
                                                               ) : ICompactFragment by data {
    actual constructor(namespaces: Iterable<Namespace>, content: CharArray) : this(
        CompactFragment(namespaces, content),
        false
                                                                                  )

    actual constructor(string: String) : this(CompactFragment(string), false) {}

    actual constructor(orig: ICompactFragment) : this(CompactFragment(orig.namespaces, orig.contentString), false) {}

}
