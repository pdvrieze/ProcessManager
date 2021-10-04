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
import io.github.pdvrieze.formats.xmlschema.datatypes.ID
import io.github.pdvrieze.formats.xmlschema.datatypes.NCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_Redefinable
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlin.math.abs

@Serializable(XSTopLevelComplexType.Serializer::class)
@XmlSerialName("complexType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
abstract class XSTopLevelComplexType(
    override val name: NCName,
    override val mixed: Boolean,
    override val abstract: Boolean,
    override val final: Set<XSDerivationSet>,
    override val block: Set<XSDerivationSet>,
    override val defaultAttributesApply: Boolean,
    override val id: ID? = null,
    override val annotations: List<XSAnnotation> = emptyList(),
    override val otherAttrs: Map<QName, String> = emptyMap()
) : T_TopLevelComplexType_Base, G_Redefinable.ComplexType {
    abstract override val content: G_ComplexTypeModel.Base

    protected abstract fun toSerialDelegate(): SerialDelegate

    @Serializable
    class SerialDelegate(
        val name: NCName,
        val mixed: Boolean,
        val abstract: Boolean,
        val final: Set<XSDerivationSet>,
        val block: Set<XSDerivationSet>,
        val complexContent: XSComplexContent? = null,
        val simpleContent: XSSimpleContent? = null,
        val groups: List<XSGroupRef> = emptyList(),
        val alls: List<XSAll> = emptyList(),
        val choices: List<XSChoice> = emptyList(),
        val sequences: List<XSSequence> = emptyList(),
        val asserts: List<XSAssert> = emptyList(),
        val atributes: List<T_Attribute> = emptyList(),
        val atributeGroups: List<T_AttributeGroupRef> = emptyList(),
        val anyAttributes: List<XSAnyAttribute> = emptyList(),
        val openContents: List<XSOpenContent> = emptyList(),
        val defaultAttributesApply: Boolean,
        val id: ID?,
        val annotations: List<XSAnnotation>,
        @XmlOtherAttributes
        val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
    ) {
        fun toTopLevelComplexType(): XSTopLevelComplexType {
            // TODO verify
            return when {
                simpleContent!=null -> XSTopLevelComplexTypeSimple(
                    name = name,
                    mixed = mixed,
                    abstract = abstract,
                    final = final,
                    block = block,
                    defaultAttributesApply = defaultAttributesApply,
                    content = simpleContent,
                    id = id,
                    annotations = annotations,
                    otherAttrs = otherAttrs,
                )
                complexContent!=null -> XSTopLevelComplexTypeComplex(
                    name = name,
                    mixed = mixed,
                    abstract = abstract,
                    final = final,
                    block = block,
                    defaultAttributesApply = defaultAttributesApply,
                    content = complexContent,
                    id = id,
                    annotations = annotations,
                    otherAttrs = otherAttrs,
                )
                else -> XSTopLevelComplexTypeShorthand(
                    name = name,
                    mixed = mixed,
                    abstract = abstract,
                    final = final,
                    block = block,
                    defaultAttributesApply = defaultAttributesApply,
                    groups = groups,
                    alls = alls,
                    choices = choices,
                    sequences = sequences,
                    asserts = asserts,
                    atributes = atributes,
                    atributeGroups = atributeGroups,
                    anyAttributes = anyAttributes,
                    openContents = openContents,
                    id = id,
                    annotations = annotations,
                    otherAttrs = otherAttrs,
                )
            }

        }

    }

    companion object Serializer: KSerializer<XSTopLevelComplexType> {
        private val delegateSerializer = SerialDelegate.serializer()

        @OptIn(ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor =
            SerialDescriptor("complexType", delegateSerializer.descriptor)

        override fun serialize(encoder: Encoder, value: XSTopLevelComplexType) {
            delegateSerializer.serialize(encoder, value.toSerialDelegate())
        }

        override fun deserialize(decoder: Decoder): XSTopLevelComplexType {
            return delegateSerializer.deserialize(decoder).toTopLevelComplexType()
        }

    }
}

class XSTopLevelComplexTypeComplex(
    name: NCName,
    mixed: Boolean,
    abstract: Boolean,
    final: Set<XSDerivationSet>,
    block: Set<XSDerivationSet>,
    defaultAttributesApply: Boolean,
    override val content: XSComplexContent,
    id: ID? = null,
    annotations: List<XSAnnotation>,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSTopLevelComplexType(
    name,
    mixed,
    abstract,
    final,
    block,
    defaultAttributesApply,
    id,
    annotations,
    otherAttrs
), T_TopLevelComplexType_Complex {
    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            name = name,
            mixed = mixed,
            abstract= abstract,
            final = final,
            block = block,
            defaultAttributesApply = defaultAttributesApply,
            complexContent = content,
            id = id,
            annotations = annotations,
            otherAttrs = otherAttrs
        )
    }
}

class XSTopLevelComplexTypeSimple(
    name: NCName,
    mixed: Boolean,
    abstract: Boolean,
    final: Set<XSDerivationSet>,
    block: Set<XSDerivationSet>,
    defaultAttributesApply: Boolean,
    override val content: XSSimpleContent,
    id: ID? = null,
    annotations: List<XSAnnotation>,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSTopLevelComplexType(
    name,
    mixed,
    abstract,
    final,
    block,
    defaultAttributesApply,
    id,
    annotations,
    otherAttrs
), T_TopLevelComplexType_Simple  {
    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            name = name,
            mixed = mixed,
            abstract= abstract,
            final = final,
            block = block,
            defaultAttributesApply = defaultAttributesApply,
            simpleContent = content,
            id = id,
            annotations = annotations,
            otherAttrs = otherAttrs
        )
    }
}

class XSTopLevelComplexTypeShorthand(
    name: NCName,
    mixed: Boolean,
    abstract: Boolean,
    final: Set<XSDerivationSet>,
    block: Set<XSDerivationSet>,
    defaultAttributesApply: Boolean,
    override val groups: List<XSGroupRef>,
    override val alls: List<XSAll>,
    override val choices: List<XSChoice>,
    override val sequences: List<XSSequence>,
    override val asserts: List<XSAssert>,
    override val atributes: List<T_Attribute>,
    override val atributeGroups: List<T_AttributeGroupRef>,
    override val anyAttributes: List<XSAnyAttribute>,
    override val openContents: List<XSOpenContent>,
    id: ID? = null,
    annotations: List<XSAnnotation>,
    otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String>
) : XSTopLevelComplexType(
    name,
    mixed,
    abstract,
    final,
    block,
    defaultAttributesApply,
    id,
    annotations,
    otherAttrs
), T_TopLevelComplexType_Shorthand {
    override val content: G_ComplexTypeModel.Shorthand get() = this

    override fun toSerialDelegate(): SerialDelegate {
        return SerialDelegate(
            name = name,
            mixed = mixed,
            abstract= abstract,
            final = final,
            block = block,
            defaultAttributesApply = defaultAttributesApply,
            groups = groups,
            alls = alls,
            choices = choices,
            sequences = sequences,
            asserts = asserts,
            atributes = atributes,
            atributeGroups = atributeGroups,
            anyAttributes = anyAttributes,
            openContents = openContents,
            id = id,
            annotations = annotations,
            otherAttrs = otherAttrs
        )
    }

}
