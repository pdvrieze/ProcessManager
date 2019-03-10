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

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.Condition
import nl.adaptivity.xmlutil.*


/**
 * Class encapsulating a condition.
 *
 * @author Paul de Vrieze
 */
class XmlCondition(override val condition: String) : XmlSerializable, Condition {

    override fun serialize(out: XmlWriter) {
        out.writeSimpleElement(QName(Engine.NAMESPACE, Condition.ELEMENTLOCALNAME,
                                                    Engine.NSPREFIX), condition)
    }

    companion object {

        fun deserialize(reader: XmlReader): XmlCondition {
            val condition = reader.readSimpleElement()
            return XmlCondition(condition)
        }
    }

}
