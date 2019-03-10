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

import kotlinx.serialization.*
import nl.adaptivity.xmlutil.serialization.simpleSerialClassDesc

/**
 * Created by pdvrieze on 04/12/16.
 */
interface Identified : Identifiable {
    override val id: String

    @Transient
    override val identifier: Identifier get() = Identifier(id)

    @Serializer(forClass = Identified::class)
    companion object {
        override val descriptor: SerialDescriptor
            get() = simpleSerialClassDesc<Identified>()

        override fun deserialize(decoder: Decoder): Identified {
            return Identifier(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, obj: Identified) {
            encoder.encodeString(obj.id)
        }
    }
}