package nl.adaptivity.util.xml;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;


public abstract class AbstractEventReader implements XMLEventReader{

  @Override
  public Object next() {
    try {
      return nextEvent();
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getElementText() throws XMLStreamException {
    StringBuilder result = new StringBuilder();
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
        throw new XMLStreamException("Unexpected tags enountered");
      }
    }
    throw new XMLStreamException("Unexpected end of document");
  }

}
