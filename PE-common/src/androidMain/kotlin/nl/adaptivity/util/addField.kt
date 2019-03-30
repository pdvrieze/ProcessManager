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

import kotlinx.serialization.SerialInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.KProperty

actual fun SerialClassDescImpl.addField(property: KProperty<*>) {
    var name = property.name

    val annotations = property.annotations
        .filter { annotation ->
            if (annotation is SerialName) {
                name = annotation.value
                false
            } else
                annotation::class.annotations.any { it is SerialInfo }
        }
    addElement(name)

    annotations.forEach { annotation -> pushAnnotation(annotation) }
}
