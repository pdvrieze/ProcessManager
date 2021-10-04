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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.GX_IdentityConstraints
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.XmlElement

interface T_Element: GX_IdentityConstraints, T_Annotated {
    val simpleTypes: List<T_LocalSimpleType>

    val complexTypes: List<T_ComplexType_Base>

    val alternatives: List<T_AltType>

    val type: QName

    val default: String?

    @XmlElement(false)
    val fixed: String?

    val nillable: Boolean?

    val block: Set<T_BlockSet>
}
