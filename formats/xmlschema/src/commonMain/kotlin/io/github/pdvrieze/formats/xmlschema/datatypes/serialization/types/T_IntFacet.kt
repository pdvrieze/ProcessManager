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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName

@Serializable
sealed class T_IntFacet : T_Facet {
    constructor(
        value: Int,
        fixed: Boolean = false,
        annotations: List<XSAnnotation> = emptyList(),
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(fixed, annotations, otherAttrs) {
        this.value = value
    }

    override val value: Int

}
