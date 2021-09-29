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
import io.github.pdvrieze.formats.xmlschema.datatypes.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.CompactFragmentSerializer
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

internal interface XSAnnotated {
    val annotation: XSAnnotation?
}

internal interface XSISimpleType : XSAnnotated {
    @XmlElement(false)
    val id: ID?

    val specification: XSSimpleTypeContent
}

@Serializable
@XmlSerialName("simpleType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
internal class XSToplevelSimpleType(
    @XmlElement(false)
    @Serializable(SimpleDerivationSetSerializer::class)
    val final: List<SimpleDerivation>?,
    @XmlElement(false)
    val name: NCName,
    override val specification: XSSimpleTypeContent,
    override val annotation: XSAnnotation? = null,
    override val id: ID? = null
) : XSISimpleType

@Serializable
@XmlSerialName("simpleType", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
internal class XSLocalSimpleType(
    @XmlElement(false)
    @Serializable(SimpleDerivationSetSerializer::class)
    val final: List<SimpleDerivation>?,
    override val specification: XSSimpleTypeContent,
    override val annotation: XSAnnotation? = null,
    override val id: ID? = null
) : XSISimpleType

private object SimpleDerivationSetSerializer : KSerializer<List<SimpleDerivation>?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("simpleDerivationSet", PrimitiveKind.STRING).nullable

    override fun serialize(encoder: Encoder, value: List<SimpleDerivation>?) {
        when (value) {
            null -> encoder.encodeNull()
            else -> {
                encoder.encodeNotNullMark()
                val attrValue = value.map {
                    when (it) {
                        SimpleDerivation.ALL -> {
                            encoder.encodeString("#all"); return
                        }
                        SimpleDerivation.RESTRICTION -> "restriction"
                        SimpleDerivation.EXTENSION -> "extension"
                        SimpleDerivation.LIST -> "list"
                        SimpleDerivation.UNION -> "union"
                    }
                }.also {
                    if (it.size == 4) {
                        encoder.encodeString("#all"); return
                    }
                }.joinToString()

                encoder.encodeString(attrValue)
            }
        }
    }

    override fun deserialize(decoder: Decoder): List<SimpleDerivation>? {
        if (!decoder.decodeNotNullMark()) return null
        return decoder.decodeString().split(' ', '\t', '\n', '\r')
            .map {
                when (it) {
                    "#all" -> return listOf(SimpleDerivation.ALL)
                    "list" -> SimpleDerivation.LIST
                    "union" -> SimpleDerivation.UNION
                    "restriction" -> SimpleDerivation.RESTRICTION
                    "extension" -> SimpleDerivation.EXTENSION
                    else -> throw SerializationException("Unknown value $it found for simpleDerivationSet")
                }
            }
    }
}

@Serializable
@XmlSerialName("annotation", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSAnnotation

internal interface SimpleRestrictionModel {
    val simpleType: XSLocalSimpleType?
    val facets: List<XSFacet>
}

@Serializable
sealed class XSSimpleTypeContent

@Serializable
@XmlSerialName("restriction", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
internal class Restriction(
    @XmlElement(false)
    val base: QName? = null,
    @XmlElement(false)
    val id: ID,
    val annotation: XSAnnotation? = null,
    val simpleType: XSLocalSimpleType? = null,

    val minExclusive: MinExclusiveRestriction? = null,
    val minInclusive: MinInclusiveRestriction? = null,
    val maxExclusive: MaxExclusiveRestriction? = null,
    val maxInclusive: MaxInclusiveRestriction? = null,
    val totalDigits: TotalDigitsRestriction? = null,
    val fractionDigits: FractionDigitsRestriction? = null,
    val length: LengthRestriction? = null,
    val minLength: MinLengthRestriction? = null,
    val maxLength: MaxLengthRestriction? = null,
    val enumeration: EnumerationRestriction? = null,
    val whiteSpace: WhiteSpaceRestriction? = null,
    val pattern: PatternRestriction? = null,
    val assertion: AssertionRestriction? = null,
    val explicitTimezone: ExplicitTimezoneRestriction? = null,

    @XmlValue(true)
    val otherRestrictions: List<@Serializable(CompactFragmentSerializer::class) CompactFragment>
) : XSSimpleTypeContent()


@Serializable
@XmlSerialName("facet", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
sealed class XSFacet(
    val fixed: Boolean = false,
    override val annotation: XSAnnotation? = null
) : XSAnnotated {
    abstract val value: Any
}

@Serializable
@XmlSerialName("minExclusive", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class MinExclusiveRestriction : XSFacet {
    constructor(value: String, fixed: Boolean, annotation: XSAnnotation? = null) :
        super(fixed, annotation) {

        this.value = value
    }

    override val value: String
}

@Serializable
@XmlSerialName("minInclusive", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class MinInclusiveRestriction : XSFacet {
    constructor(value: String, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(fixed, annotation) {

        this.value = value
    }

    override val value: String
}

@Serializable
@XmlSerialName("maxExclusive", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class MaxExclusiveRestriction : XSFacet {
    constructor(value: String, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(fixed, annotation) {

        this.value = value
    }

    override val value: String
}

@Serializable
@XmlSerialName("maxInclusive", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class MaxInclusiveRestriction : XSFacet {
    constructor(value: String, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(fixed, annotation) {

        this.value = value
    }

    override val value: String
}

@Serializable
sealed class XSNumFacet : XSFacet {
    constructor(
        value: ULong,
        fixed: Boolean = false,
        annotation: XSAnnotation? = null
    ) : super(fixed, annotation) {
        this.value = value
    }

    override val value: ULong

}

@Serializable
sealed class XSIntFacet : XSFacet {
    constructor(
        value: Int,
        fixed: Boolean = false,
        annotation: XSAnnotation? = null
    ) : super(fixed, annotation) {
        this.value = value
    }

    override val value: Int

}

@Serializable
@XmlSerialName("totalDigits", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class TotalDigitsRestriction : XSNumFacet {
    constructor(value: ULong, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(value, fixed, annotation)
}

@Serializable
@XmlSerialName("fractionDigits", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class FractionDigitsRestriction : XSNumFacet {
    constructor(value: ULong, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(value, fixed, annotation)
}

@Serializable
@XmlSerialName("length", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class LengthRestriction : XSNumFacet {
    constructor(value: ULong, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(value, fixed, annotation)
}

@Serializable
@XmlSerialName("minLength", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class MinLengthRestriction : XSNumFacet {
    constructor(value: ULong, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(value, fixed, annotation)
}

@Serializable
@XmlSerialName("maxLength", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class MaxLengthRestriction : XSNumFacet {
    constructor(value: ULong, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(value, fixed, annotation)
}

@Serializable
@XmlSerialName("enumeration", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class EnumerationRestriction : XSFacet {
    constructor(value: String, annotation: XSAnnotation? = null) : super( false, annotation) {
        this.value = value
    }

    override val value: String
}

@Serializable
@XmlSerialName("whiteSpace", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class WhiteSpaceRestriction : XSFacet {
    constructor(value: WhitespaceUse, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(fixed, annotation) {
            this.value = value
        }

    override val value: WhitespaceUse
}

@Serializable
@XmlSerialName("pattern", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class PatternRestriction : XSFacet {
    constructor(value: String, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(false, annotation) {
        this.value = value
    }

    override val value: String
}

@Serializable
@XmlSerialName("assertion", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class AssertionRestriction : XSFacet {
    constructor(test: XPathExpression, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(fixed, annotation) {
        this.value = test
    }

    @SerialName("test")
    override val value: XPathExpression
}

@Serializable
@XmlSerialName("explicitTimezone", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class ExplicitTimezoneRestriction : XSFacet {
    constructor(value: ExplicitTimezoneRequired, fixed: Boolean = false, annotation: XSAnnotation? = null) :
        super(fixed, annotation) {
            this.value = value
        }

    override val value: ExplicitTimezoneRequired
}

@Serializable
@XmlSerialName("list", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
internal class ListT(
    val simpleType: XSLocalSimpleType,
    val id: ID? = null,
    val itemType: QName? = null,
    val annotation: XSAnnotation? = null
) : XSSimpleTypeContent()

@Serializable
@XmlSerialName("union", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
internal class UnionT(
    val id: ID? = null,
    @XmlElement(false)
    val memberTypes: List<QName> = emptyList(),
    val annotation: XSAnnotation? = null,
    val simpleTypes: List<XSLocalSimpleType> = emptyList()
) : XSSimpleTypeContent()
