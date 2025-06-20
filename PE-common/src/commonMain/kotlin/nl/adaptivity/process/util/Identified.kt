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
 * Created by pdvrieze on 04/12/16.
 */
@Serializable(Identified.Companion::class)
interface Identified : Identifiable {
    override val id: String

    override val identifier: Identifier
        get() = Identifier(id)

    companion object: DelegatingSerializer<Identified, String>(String.serializer()) {
        override val descriptor: SerialDescriptor = SerialDescriptor("nl.adaptivity.process.util.Identified", String.serializer().descriptor)

        override fun fromDelegate(delegate: String): Identified = Identifier(delegate)

        override fun Identified.toDelegate(): String = id
    }
}
