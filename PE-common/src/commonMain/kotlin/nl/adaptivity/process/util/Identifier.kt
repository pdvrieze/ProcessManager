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

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.serialutil.DelegatingSerializer
import nl.adaptivity.serialutil.simpleSerialClassDesc


/**
 * A class representing a simple identifier. It just holds a single string.
 */
@Serializable(Identifier.Companion::class)
class Identifier(override var id: String) : Identified {

    @Transient
    override val identifier: Identifier
        get() = this

    private class ChangeableIdentifier(private val idBase: String) : Identified {
        private var idNo: Int = 1

        override val id: String
            get() = idBase + idNo.toString()

        override fun compareTo(other: Identifiable): Int {
            val otherId = other.id
            if (otherId == null) return 1
            return id.compareTo(otherId)
        }

        operator fun next() {
            ++idNo
        }
    }

    constructor(id: CharSequence) : this(id.toString()) {}

    override fun compareTo(o: Identifiable): Int {
        val otherId = o.id ?: return 1
        return id.compareTo(otherId)
    }

    override fun toString(): String = id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Identifier

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object: DelegatingSerializer<Identifier, String>(String.serializer()) {
        override fun fromDelegate(delegate: String): Identifier = Identifier(delegate)

        override fun Identifier.toDelegate(): String = id

        fun findIdentifier(idBase: String, exclusions: Iterable<Identifiable>): String {
            val idFactory = ChangeableIdentifier(idBase)
            return generateSequence({ idFactory.id }, { idFactory.next(); idFactory.id })
                .filter { candidate -> exclusions.none { candidate == it.id } }
                .first()
        }
    }
}
