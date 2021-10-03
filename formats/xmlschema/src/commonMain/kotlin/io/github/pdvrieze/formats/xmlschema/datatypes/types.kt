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

package io.github.pdvrieze.formats.xmlschema.datatypes

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ID(val value: String)

@JvmInline
@Serializable
value class NCName(val value: String)

@JvmInline
@Serializable
value class Token(val value: String)

@JvmInline
@Serializable
value class AnyURI(val value: String)

@JvmInline
@Serializable
value class XPathExpression(val test: String)

@Serializable(AllNNI.Serializer::class)
sealed class AllNNI {
    object UNBOUNDED: AllNNI() {
        override fun toString(): String = "unbounded"
    }
    class Value(val value: ULong): AllNNI() {
        override fun toString(): String = value.toString()
    }

    companion object Serializer: KSerializer<AllNNI> {

        operator fun invoke(v: Int): Value = Value(v.toULong())

        operator fun invoke(v: UInt): Value = Value(v.toULong())

        operator fun invoke(v: Long): Value = Value(v.toULong())

        operator fun invoke(v: ULong): Value = Value(v)

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AllNNI" , PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): AllNNI = when (val v= decoder.decodeString()) {
            "unbounded" -> UNBOUNDED
            else -> Value(v.toULong())
        }

        override fun serialize(encoder: Encoder, value: AllNNI) {
            encoder.encodeString(value.toString())
        }
    }
}
