/*
 * Copyright (c) 2017.
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

import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.JSCompactFragment

/**
 * Created by pdvrieze on 13/04/17.
 */
object XmlStreaming
{
}

fun CompactFragment(content:String): CompactFragment = JSCompactFragment(content)
fun CompactFragment(namespaces:Iterable<Namespace>, content:CharArray): CompactFragment = JSCompactFragment(namespaces, content)
fun CompactFragment(namespaces:Iterable<Namespace>, content:String): CompactFragment = JSCompactFragment(namespaces, content)
