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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_IdentityConstraint
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_SimpleDerivation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_Annotated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*

interface XSSimpleRestrictionModel {
    val simpleTypes: List<XSLocalSimpleType>
    val facets: List<XSFacet>
}

@Serializable
@XmlSerialName("unique", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSUnique: G_IdentityConstraint.Unique

@Serializable
@XmlSerialName("key", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSKey: G_IdentityConstraint.Key

@Serializable
@XmlSerialName("keyref", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSKeyref: G_IdentityConstraint.Keyref

@Serializable
sealed class XSSimpleDerivation

@Serializable
@XmlSerialName("list", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
internal class XSSimpleList(
    val simpleType: XSLocalSimpleType,
    val id: ID? = null,
    val itemType: QName? = null,
    val annotation: XSAnnotation? = null
) : XSSimpleDerivation(), G_SimpleDerivation.List

@Serializable
@XmlSerialName("union", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
internal class XSSimpleUnion(
    val id: ID? = null,
    @XmlElement(false)
    val memberTypes: List<QName> = emptyList(),
    val annotation: XSAnnotation? = null,
    val simpleTypes: List<XSLocalSimpleType> = emptyList()
) : XSSimpleDerivation(), G_SimpleDerivation.Union

internal interface SimpleRestrictionModel {
    val simpleType: XSLocalSimpleType?
    val facets: List<XSFacet>
}

@Serializable
@XmlSerialName("facet", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
sealed class XSFacetBase(
    override val annotations: List<XSAnnotation> = emptyList(),
    @XmlOtherAttributes
    override val otherAttrs: Map<QName, String> = emptyMap(),
) : T_Annotated {
    abstract val fixed: Boolean

    abstract val value: Any
}

@Serializable
sealed class XSFacet : XSFacetBase {
    override val fixed: Boolean

    constructor(fixed: Boolean = false, annotations: List<XSAnnotation>, otherAttrs: Map<QName, String>) :
        super(annotations, otherAttrs) {
        this.fixed = fixed
    }
}

@Serializable
sealed class XSNoFixedFacet : XSFacetBase {
    constructor(annotations: List<XSAnnotation>, otherAttrs: Map<QName, String>) :
        super(annotations, otherAttrs)

    override val fixed get() = false
}

@Serializable
sealed class XSNumFacet : XSFacet {
    constructor(
        value: ULong,
        fixed: Boolean = false,
        annotations: List<XSAnnotation> = emptyList(),
        otherAttrs: Map<QName, String> = emptyMap()
    ) : super(fixed, annotations, otherAttrs) {
        this.value = value
    }

    override val value: ULong
}

@Serializable
sealed class XSIntFacet : XSFacet {
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

enum class T_ProcessContents {
    @SerialName("skip") SKIP,
    @SerialName("lax") LAX,
    @SerialName("strict") STRICT,
}

internal fun parseQName(d: XML.XmlInput?, str: String): QName {
    val cIdx = str.lastIndexOf(':')
    if (d==null) {
        if (cIdx<0) return QName(str)
        val localName = if(cIdx<0) str else str.substring(cIdx+1)
        if (str[0]!='{') throw SerializationException("Missing { before namespace")
        val clIdx = str.indexOf('}', 1)
        if (clIdx<1) throw SerializationException("Missing } after namespace")
        val namespace = str.substring(1, clIdx)
        val prefix = str.substring(clIdx+1, cIdx)
        return QName(namespace, localName, prefix)
    } else {
        val localName: String
        val prefix: String
        if(cIdx<0) {
            localName = str
            prefix = ""
        } else {
            localName = str.substring(cIdx+1)
            prefix = str.substring(0, cIdx)
        }
        val namespace = d.getNamespaceURI(prefix) ?: ""
        return QName(namespace, localName, prefix)
    }
}
