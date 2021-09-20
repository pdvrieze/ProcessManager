/*
 * Copyright (c) 2018.
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

package net.devrieze.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.util.multiplatform.URI
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(HandleSerializer::class)
value class Handle<out T : Any?>(val handleValue: Long) : Comparable<Handle<@UnsafeVariance T>> {

    constructor(handleString: String) : this(handleString.toLong())

    constructor(handleUri: URI): this(handleUri.toHandleValue())

    val isValid get() = handleValue >= 0L

    override fun compareTo(other: Handle<@UnsafeVariance T>): Int {
        return handleValue.compareTo(other.handleValue)
    }

    override fun toString(): String {
        return "H:$handleValue"
    }

    companion object {
        fun <T> invalid(): Handle<T> = Handle(-1L)

        private fun URI.toHandleValue(): Long {
            val path: String = getPath()
            val slashPos = path.lastIndexOf('/')
            return if (slashPos > 0) {
                path.substring(slashPos + 1).toLong()
            } else {
                path.toLong()
            }

        }
    }
}

class HandleSerializer<T>(@Suppress("UNUSED_PARAMETER") elemSerializer: KSerializer<T>) : KSerializer<Handle<T>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("net.devrieze.util.Handle", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Handle<T> {
        return Handle(decoder.decodeLong())
    }

    override fun serialize(encoder: Encoder, value: Handle<T>) {
        when {
            value.isValid -> encoder.encodeLong(value.handleValue)
            else -> Unit //encoder.encodeNull()
        }
    }
}

