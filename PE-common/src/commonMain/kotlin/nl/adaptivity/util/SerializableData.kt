/*
 * Copyright (c) 2021.
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

package nl.adaptivity.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import net.devrieze.util.Transaction
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.serialization.XML

class SerializableData<T>(val serializer: KSerializer<T>, val data: T, val tagName: QName? = null) {
    fun encodeToWriter(writer: XmlWriter, format: XML = XML { autoPolymorphic = true }) {
        when (tagName) {
            null -> format.encodeToWriter(writer, serializer, data)
            else -> format.encodeToWriter(writer, serializer, data, tagName)
        }
    }
}

fun <T> Transaction.commitSerializable(serializer: KSerializer<T>, data: T, tagName: QName? = null): SerializableData<T> {
    return commit(SerializableData(serializer, data, tagName))
}

inline fun <reified T> Transaction.commitSerializable(data: T, tagName: QName? = null): SerializableData<T> {
    return commitSerializable(serializer<T>(), data, tagName)
}
