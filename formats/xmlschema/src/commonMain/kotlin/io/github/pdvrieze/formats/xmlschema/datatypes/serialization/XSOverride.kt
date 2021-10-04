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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.AnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_Composition
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_Annotated
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("override", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSOverride(
    val schemaLocation: AnyURI,
    val id: ID? = null,
    override val annotations: List<XSAnnotation> = emptyList(),
    override val simpleTypes: List<XSToplevelSimpleType> = emptyList(),
    override val complexTypes: List<XSTopLevelComplexType> = emptyList(),
    override val groups: List<XSGroup> = emptyList(),
    override val attributeGroups: List<XSAttributeGroup> = emptyList(),
    override val elements: List<XSElement> = emptyList(),
    override val attributes: List<XSAttribute> = emptyList(),
    override val notations: List<XSNotation> = emptyList(),
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap(),
): XSComposition, T_Annotated, XSUseSchemaTop, G_Composition.Override
