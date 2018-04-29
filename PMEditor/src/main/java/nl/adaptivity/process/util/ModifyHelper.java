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

package nl.adaptivity.process.util;

import nl.adaptivity.process.util.ModifySequence.AttributeSequence;
import nl.adaptivity.process.util.ModifySequence.FragmentSequence;
import nl.adaptivity.xml.*;

import java.util.List;


/**
 * Utility class that helps with the modify namespace.
 * Created by pdvrieze on 15/02/16.
 */
public final class ModifyHelper {

  private ModifyHelper() {}

  /**
   * Parse any tag in the modify namespace.
   * @param in The parser
   * @throws XmlException When a reading error occurs, including unexpected tags.
   */
  public static ModifySequence parseAny(final XmlReader in) {
    in.require(EventType.START_ELEMENT, Constants.MODIFY_NS_STR, null);
    switch (in.getLocalName().toString()) {
      case "attribute":
        return parseAttribute(in);
      case "value":
        return parseValueOrElement(in);
      case "element":
        return parseValueOrElement(in);
      default:
        throw new XmlException("Expected a valid modification element");
    }

  }

  public static AttributeSequence parseAttribute(final XmlReader in) {
    final CharSequence defineName = in.getAttributeValue(null, "value");
    final CharSequence xpath = in.getAttributeValue(null, "xpath");
    final CharSequence paramName = in.getAttributeValue(null, "name");
    in.nextTag();
    in.require(EventType.END_ELEMENT, Constants.MODIFY_NS_STR, "attribute");
    return ModifySequence.newAttributeSequence(paramName, defineName, xpath);
  }

  public static FragmentSequence parseValueOrElement(final XmlReader in) {
    final CharSequence defineName = in.getAttributeValue(null, "value");
    final CharSequence xpath = in.getAttributeValue(null, "xpath");
    final CharSequence localName = in.getLocalName();
    in.nextTag();
    in.require(EventType.END_ELEMENT, Constants.MODIFY_NS_STR, localName);
    return ModifySequence.newFragmentSequence(localName.toString(), defineName, xpath);
  }

  public static void writeAttribute(final List<XmlSerializable> pending, final XmlWriter out, final String name, final CharSequence value) {
    if (value==null) { return; }
    if (value instanceof XmlSerializable) {
      pending.add((XmlSerializable) value);
    } else {
      out.attribute(null, name, null, value);
    }
  }
}
