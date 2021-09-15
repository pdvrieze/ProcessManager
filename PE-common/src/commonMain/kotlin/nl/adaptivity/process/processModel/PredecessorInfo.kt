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

package nl.adaptivity.process.processModel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.process.processModel.engine.XmlCondition
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
class PredecessorInfo(
    @SerialName("predecessor")
    @XmlValue(true) val id: String,
    @Serializable(Condition.Serializer::class)
    @XmlElement(false) val condition: Condition? = null
) {
    init {
        if (id.isEmpty()) throw IllegalArgumentException("Empty id's are not valid")
    }

    override fun toString(): String {
        return "PredecessorInfo(id='$id', condition=$condition)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PredecessorInfo) return false

        if (id != other.id) return false
        if ((condition?: "") != (other.condition ?: "")) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (condition?.hashCode() ?: 0)
        return result
    }


}
