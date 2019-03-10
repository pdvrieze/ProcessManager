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

import javax.xml.bind.JAXBElement
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAnyElement
import javax.xml.bind.annotation.XmlMixed
import javax.xml.namespace.QName


@XmlAccessorType(XmlAccessType.NONE)
class JAXBCollectionWrapper(
    @get:XmlMixed
    @get:XmlAnyElement(lax = true)
    val elements: Collection<*>,
    val elementType: Class<*>) {

    constructor(): this(emptyList<Any>(), Any::class.java)

    fun getJAXBElement(pName: QName): JAXBElement<JAXBCollectionWrapper> {
        return JAXBElement(pName, JAXBCollectionWrapper::class.java, this)
    }

}
