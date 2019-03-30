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

package nl.adaptivity.util

import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.KProperty

fun SerialClassDescImpl(original: SerialDescriptor, name: String): SerialClassDescImpl {
    return SerialClassDescImpl(name).apply {
        for(a in original.getEntityAnnotations()) {
            pushClassAnnotation(a)
        }
        addFields(original)
    }
}

fun SerialClassDescImpl.addFields(origin: SerialDescriptor) {
    for (i in 0 until origin.elementsCount) {
        addElement(origin.getElementName(i), origin.isElementOptional(i))
        for (a in origin.getElementAnnotations(i)) {
            pushAnnotation(a)
        }
    }

}


expect fun SerialClassDescImpl.addField(property: KProperty<*>)

fun SerialDescriptor.describe(): String {
    return (0 until elementsCount).joinToString(",\n", prefix = "$name[$kind] (", postfix = ")") {
        getElementAnnotations(it).joinToString(postfix = " : ${getElementName(it)}")
    }
}
