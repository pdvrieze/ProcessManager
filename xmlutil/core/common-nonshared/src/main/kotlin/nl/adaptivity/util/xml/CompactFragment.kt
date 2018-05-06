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

package nl.adaptivity.util.xml

import kotlinx.serialization.Serializable
import kotlinx.serialization.*
import nl.adaptivity.util.multiplatform.Class
import nl.adaptivity.util.multiplatform.JvmStatic
import nl.adaptivity.xml.Namespace
import nl.adaptivity.xml.XmlDeserializerFactory
import nl.adaptivity.xml.XmlReader
import nl.adaptivity.xml.XmlSerializable
import kotlin.reflect.KClass

/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.2
 */
expect class CompactFragment : ICompactFragment {
    constructor(content: String)
    constructor(orig: ICompactFragment)
    constructor(content: XmlSerializable)
    constructor(namespaces: Iterable<Namespace>, content: CharArray?)
    constructor(namespaces: Iterable<Namespace>, content: String)

    class Factory() : XmlDeserializerFactory<CompactFragment>

    companion object {
        fun deserialize(reader: XmlReader): CompactFragment
    }
}
