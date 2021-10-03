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

import io.github.pdvrieze.formats.xmlschema.datatypes.NCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.T_Annotated
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSDerivationSet

interface T_ComplexType: T_Annotated {
    val name: NCName?
    val mixed: Boolean // May not have simpleContent child
    val abstract: Boolean // default false
    val final: Set<XSDerivationSet>
    val block: Set<XSDerivationSet>
    val defaultAttributesApply: Boolean // default true
}
