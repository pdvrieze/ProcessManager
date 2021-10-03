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

@file:UseSerializers(QNameSerializer::class)

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.NCName
import io.github.pdvrieze.formats.xmlschema.datatypes.XPathExpression
import io.github.pdvrieze.formats.xmlschema.datatypes.XSSimpleDerivationSet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_ComplexType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_GroupRef
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_LocalElement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName

interface XSOpenAttrs {
    @XmlOtherAttributes
    val otherAttrs: Map<QName, String>
}

interface T_Annotated: XSOpenAttrs {
    val annotations: List<XSAnnotation>
}

enum class  XSContentMode {
    @SerialName("interleave")
    INTERLEAVE,
    @SerialName("suffix")
    SUFFIx
}

/**
 * XSInclude | XSImport | XSRedefine | XSOverride | XSAnnotation
 */
sealed interface XSComposition

interface XSUseComposition {
    val annotations: List<XSAnnotation>
    val includes: List<XSInclude>
    val imports: List<XSImport>
    val redefines: List<XSRedefine>
    val overrides: List<XSOverride>
}

interface XSUseSchemaTop {
    val simpleTypes: List<XSToplevelSimpleType>
    val complexTypes: List<XSToplevelComplexType>
    val groups: List<XSGroup>
    val attributeGroups: List<XSAttributeGroup>
    val elements: List<XSElement>
    val attributes: List<XSAttribute>
    val notations: List<XSNotation>
}

@Serializable
@XmlSerialName("element", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSElement: G_SchemaTop.Element

@Serializable
@XmlSerialName("notation", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSNotation: G_SchemaTop.Notation

typealias XSDerivationSet=XSSimpleDerivationSet

interface XSUseSimpleContent

@XmlSerialName("simpleContent", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
@Serializable
class XSSimpleContent(
    val derivation: XSSimpleContentDerivation,
    override val annotations: List<XSAnnotation> = emptyList(),
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
): T_Annotated, G_ComplexTypeModel.SimpleContent

@XmlSerialName("complexContent", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
@Serializable
class XSComplexContent: G_ComplexTypeModel.ComplexContent

@Serializable
class XSAnyAttribute

@Serializable
sealed class XSSimpleContentDerivation(
    val base: QName,
    val id: ID? = null,
    val attributes: List<XSAttribute> = emptyList(),
    val attributeGroups: List<XSAttributeGroup> = emptyList(),
    val anyAttribute: XSAnyAttribute? = null,
    override val assertions: List<XSAssert> = emptyList(),
    override val annotations: List<XSAnnotation> = emptyList(),
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
): T_Annotated, XSUseAssertions

@Serializable
@XmlSerialName("restriction", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSSimpleContentRestriction: XSSimpleContentDerivation, XSUseAttrDecls {

    constructor(
        base: QName,
        id: ID? = null,
        attributes: List<XSAttribute> = emptyList(),
        attributeGroups: List<XSAttributeGroup> = emptyList(),
        anyAttribute: XSAnyAttribute? = null,
        assertions: List<XSAssert> = emptyList(),
        annotations: List<XSAnnotation> = emptyList(),
        otherAttrs: Map<QName, String> = emptyMap()
    ): super(base, id, attributes, attributeGroups, anyAttribute, assertions, annotations, otherAttrs)
}

@Serializable
@XmlSerialName("extension", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSSimpleContentExtension: XSSimpleContentDerivation {

    constructor(
        base: QName,
        id: ID? = null,
        attributes: List<XSAttribute> = emptyList(),
        attributeGroups: List<XSAttributeGroup> = emptyList(),
        anyAttribute: XSAnyAttribute? = null,
        assertions: List<XSAssert> = emptyList(),
        annotations: List<XSAnnotation> = emptyList(),
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(base, id, attributes, attributeGroups, anyAttribute, assertions, annotations, otherAttrs)
}

interface XSUseComplexContent

interface XSUseTypeDefParticles {
    @XmlSerialName("group", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
    val groups: List<XSGroupRef>

}

class XSAll: G_TypeDefParticle.All, G_Particle.All

class XSChoice: G_TypeDefParticle.Choice, G_NestedParticle.Choice, G_Particle.All

class XSSequence: G_TypeDefParticle.Sequence, G_NestedParticle.Sequence, G_Particle.Sequence

class XSAny: G_NestedParticle.Any, G_Particle.Any

class XSLocalElement: G_NestedParticle.Element, G_Particle.Element

interface XSIChoice

interface XSISequence

@Serializable
@XmlSerialName("group", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSGroupRef: T_GroupRef, G_TypeDefParticle.Group, G_NestedParticle.Group, G_Particle.Group {

}

interface XSUseAttrDecls

interface XSUseAssertions {
    val assertions: List<XSAssert>
}

interface XSAssertion: T_Annotated {
    val test: XPathExpression
    val xpathDefaultNamespace: XPathDefaultNamespace?
    override val annotations: List<XSAnnotation>
    override val otherAttrs: Map<QName, String>
}

@Serializable
@XmlSerialName("assert", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAssert(
    val test: XPathExpression,
    val xpathDefaultNamespace: XPathDefaultNamespace? = null,
    override val annotations: List<XSAnnotation> = emptyList(),
    override val otherAttrs: Map<QName, String> = emptyMap()
) : T_Annotated

interface XSUseComplexTypeModel: XSUseSimpleContent, XSUseComplexContent, XSOpenAttrs, XSUseTypeDefParticles, XSUseAttrDecls, XSUseAssertions {
    val openContent: List<XSOpenContent>
}

class XSOpenContent

@Serializable
@XmlSerialName("complexType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSToplevelComplexType(
    override val name: NCName,
    override val mixed: Boolean,
    override val abstract: Boolean,
    override val final: Set<XSDerivationSet>,
    override val block: Set<XSDerivationSet>,
    override val defaultAttributesApply: Boolean,
    override val annotations: List<XSAnnotation>,
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : T_ComplexType, XSUseComplexTypeModel, G_Redefinable.ComplexType {

}

@Serializable
@XmlSerialName("complexType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSLocalComplexType(
    override val mixed: Boolean,
    override val defaultAttributesApply: Boolean,
    override val annotations: List<XSAnnotation>,
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : T_ComplexType, XSUseComplexTypeModel {
    override val name: Nothing? get() = null
    override val abstract: Boolean get() = false
    override val final: Set<XSDerivationSet> get() = emptySet()
    override val block: Set<XSDerivationSet> get() = emptySet()

}



@Serializable
@XmlSerialName("group", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSGroup: G_Redefinable.Group

@Serializable
@XmlSerialName("attributeGroup", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAttributeGroup: G_Redefinable.AttributeGroup
