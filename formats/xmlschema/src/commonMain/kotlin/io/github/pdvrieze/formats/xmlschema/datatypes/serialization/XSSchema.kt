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
import io.github.pdvrieze.formats.xmlschema.datatypes.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_BlockSet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_FormChoice
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_OpenAttrs
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_TypeDerivationControl
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlAfter
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("attribute", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSSchema(
    val targetNamespace: AnyURI,
    val version: Token? = null,
    val attributeFormDefault: T_FormChoice = T_FormChoice.UNQUALIFIED,
    @Serializable(SchemaEnumSetSerializer::class)
    val blockDefault: Set<T_BlockSet>,
    @Serializable(QNameSerializer::class)
    val defaultAttributes: QName? = null,
    val xpathDefaultNamespace: String? = null,
    val elementFormDefault: T_FormChoice = T_FormChoice.UNQUALIFIED,
    @Serializable(SchemaEnumSetSerializer::class)
    val finalDefault: Set<T_TypeDerivationControl> = emptySet(),
    val id: ID? = null,

    override val annotations: List<XSAnnotation> = emptyList(),
    override val includes: List<XSInclude> = emptyList(),
    override val imports: List<XSImport> = emptyList(),
    override val redefines: List<XSRedefine> = emptyList(),
    override val overrides: List<XSOverride> = emptyList(),

    @XmlAfter("includes", "imports", "redefines", "overrides")
    @XmlBefore("simpleTypes", "complexTypes", "groups", "attributeGroups", "elements, attributes, notations")
    val defaultOpenContent: List<XSDefaultOpenContent> = emptyList(),

    override val simpleTypes: List<XSToplevelSimpleType> = emptyList(),
    override val complexTypes: List<XSTopLevelComplexType> = emptyList(),
    override val groups: List<XSGroup> = emptyList(),
    override val attributeGroups: List<XSAttributeGroup> = emptyList(),
    override val elements: List<XSElement> = emptyList(),
    override val attributes: List<XSAttribute> = emptyList(),
    override val notations: List<XSNotation> = emptyList(),
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
) : T_OpenAttrs, XSUseComposition, XSUseSchemaTop
