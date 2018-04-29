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

package nl.adaptivity.process.engine

import net.devrieze.util.Named
import nl.adaptivity.util.xml.*
import nl.adaptivity.xml.*
import nl.adaptivity.xml.QName


/** Class to represent data attached to process instances.  */
expect class ProcessData(name: String?, value: ICompactFragment) : Named, ExtXmlDeserializable, XmlSerializable {

    var content: ICompactFragment
        private set

    val contentStream: XmlReader

//    fun copy(name: String? = this.name, value: ICompactFragment = content): ProcessData

    companion object {

        val ELEMENTLOCALNAME: String
        val ELEMENTNAME: QName

        fun missingData(name: String): ProcessData

        fun deserialize(reader: XmlReader): ProcessData
    }
}
