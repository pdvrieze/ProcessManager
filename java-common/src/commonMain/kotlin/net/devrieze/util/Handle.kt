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

@Serializable(HandleSerializer::class)
interface Handle<out T : Any?> {

    val handleValue: Long

    @Deprecated("Use isValid", ReplaceWith("isValid"))
    val valid: Boolean get() = isValid

    val isValid get() = handleValue >= 0
}


class HandleSerializer<T>(@Suppress("UNUSED_PARAMETER") elemSerializer: KSerializer<T>): KSerializer<Handle<T>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("net.devrieze.util.Handle", PrimitiveKind.LONG)

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


@Suppress("NOTHING_TO_INLINE")
inline fun <T:Any?> Handle(handleValue:Long):Handle<T> = handle(handle= handleValue)

interface ComparableHandle<out T: Any?> : Handle<T>, Comparable<ComparableHandle<@kotlin.UnsafeVariance T>> {
    override fun compareTo(other: ComparableHandle<@kotlin.UnsafeVariance T>):Int {
        return handleValue.compareTo(other.handleValue)
    }
}

