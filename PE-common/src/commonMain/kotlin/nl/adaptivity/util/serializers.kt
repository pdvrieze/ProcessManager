/*
 * Copyright (c) 2019.
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

package nl.adaptivity.util

import foo.FakeSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.devrieze.util.security.SimplePrincipal
import net.devrieze.util.security.name
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.util.multiplatform.toUUID
import nl.adaptivity.util.security.Principal

@FakeSerializer(forClass = Principal::class)
class PrincipalSerializer : KSerializer<Principal> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): Principal {
        return SimplePrincipal(String.serializer().deserialize(decoder))
    }

    override fun serialize(encoder: Encoder, obj: Principal) {
        String.serializer().serialize(encoder, obj.name)
    }
}

@FakeSerializer(forClass = UUID::class)
class UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): UUID {
        return String.serializer().deserialize(decoder).toUUID()
    }

    override fun serialize(encoder: Encoder, obj: UUID) {
        String.serializer().serialize(encoder, obj.toString())
    }
}
