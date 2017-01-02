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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.util.xml;

import org.jetbrains.annotations.NotNull;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;


public abstract class AbstractEventReader implements XMLEventReader{

  @Override
  public Object next() {
    try {
      return nextEvent();
    } catch (@NotNull final XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  @Override
  public String getElementText() throws XMLStreamException {
    final StringBuilder result = new StringBuilder();
    for (XMLEvent event = nextEvent(); ! event.isEndElement();event = nextEvent()) {
      if (event.isCharacters()) {
        result.append(((Characters) event).getData());
      } else if (event.isAttribute()) {
        // ignore
      } else {
        throw new XMLStreamException("Unexpected child");
      }
    }
    return result.toString();
  }

  @Override
  public XMLEvent nextTag() throws XMLStreamException {
    for (XMLEvent event = nextEvent(); ! event.isEndDocument();event = nextEvent()) {
      if (event.isStartElement()) {
        return event;
      } else if (event.isEndElement()) {
        return event;
      } else if (event.isAttribute()) { // ignore
      } else if (event.isCharacters()) {
        if (!event.asCharacters().isIgnorableWhiteSpace()) {
          throw new XMLStreamException("Non-whitespace text encountered");
        }
      } else {
        throw new XMLStreamException("Unexpected tags encountered");
      }
    }
    throw new XMLStreamException("Unexpected end of document");
  }

}
