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

package nl.adaptivity.xml.generators;

import nl.adaptivity.xml.XmlReaderUtil;
import nl.adaptivity.xml.XmlUtil;
import nl.adaptivity.xml.XmlWriterUtil;


/**
 * Class to help access to classes not available directly from within Kotlin without reflection.
 * Created by pdvrieze on 27/04/16.
 */
public class FactoryHelper {
  private FactoryHelper(){}

  static final Class<XmlWriterUtil> XMLWriterUtil = XmlWriterUtil.class;
  static final Class<XmlReaderUtil> XMLReaderUtil = XmlReaderUtil.class;
  static final Class<XmlUtil>       XMLUtil       = XmlUtil.class;

}
