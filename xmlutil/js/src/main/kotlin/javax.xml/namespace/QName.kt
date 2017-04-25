/*
 * Copyright (c) 2017.
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

package javax.xml.namespace

import javax.xml.XMLConstants

/**
 * Javascript implementation of QName needed fro the xml util
 */
fun QName(namespaceURI: CharSequence?, localPart: CharSequence,  prefix:CharSequence = XMLConstants.DEFAULT_NS_PREFIX) = QName(namespaceURI ?: XMLConstants.NULL_NS_URI, localPart, prefix)

class QName(val namespaceURI: CharSequence = XMLConstants.NULL_NS_URI, val localPart: CharSequence, val prefix:CharSequence = XMLConstants.DEFAULT_NS_PREFIX)
{
}