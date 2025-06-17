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

package nl.adaptivity.process.util

import kotlinx.serialization.Serializer
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.serialutil.DelegateSerializer
import nl.adaptivity.serialutil.DelegatingSerializer

/**
 * Interface for objects that may have identifiers.
 */
interface Identifiable : Comparable<Identifiable> {

    val id: String?

    val identifier: Identifier?
        get() = id?.let(::Identifier)

    override fun compareTo(other: Identifiable): Int {
        val otherId = other.id
        return if (otherId == null) {
            id?.let { 1 } ?: 0
        } else {
            id?.compareTo(otherId) ?: -1
        }
    }

    companion object: DelegatingSerializer<Identifiable, String?>(String.serializer().nullable) {
        override val descriptor: SerialDescriptor = SerialDescriptor(Identifiable::class.qualifiedName!!, String.serializer().descriptor)

        override fun fromDelegate(delegate: String?): Identifiable = Identifier(delegate!!)

        override fun Identifiable.toDelegate(): String? {
            return id
        }
    }

}

object IdentifiableListSerializer : DelegateSerializer<List<Identifiable>>(ListSerializer(Identifiable))

object IdentifiableSetSerializer : DelegateSerializer<Set<Identifiable>>(SetSerializer(Identifiable))
