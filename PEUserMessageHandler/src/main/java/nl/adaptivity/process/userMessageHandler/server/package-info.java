/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

@javax.xml.bind.annotation.XmlSchema(namespace = Constants.USER_MESSAGE_HANDLER_NS,
                                     xmlns = { @javax.xml.bind.annotation.XmlNs(namespaceURI = Constants.USER_MESSAGE_HANDLER_NS,
                                                                                prefix = "umh") },
                                     elementFormDefault=XmlNsForm.QUALIFIED)
package nl.adaptivity.process.userMessageHandler.server;

import nl.adaptivity.process.util.Constants;
import javax.xml.bind.annotation.XmlNsForm;


