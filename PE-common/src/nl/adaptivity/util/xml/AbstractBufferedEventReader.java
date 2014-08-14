package nl.adaptivity.util.xml;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;


public abstract class AbstractBufferedEventReader extends AbstractEventReader {
  private ArrayDeque<XMLEvent> aPeekBuffer = new ArrayDeque<>();

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    if (! aPeekBuffer.isEmpty()) {
      return aPeekBuffer.removeFirst();
    }
    if (! hasNext()) { throw new NoSuchElementException(); }
    peek();
    return aPeekBuffer.removeFirst();
  }

  @Override
  public boolean hasNext() {
    if (! aPeekBuffer.isEmpty()) { return true; }
    try {
      return peek()!=null;
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  protected boolean isPeekBufferEmpty() {
    return aPeekBuffer.isEmpty();
  }

  protected XMLEvent peekFirst() {
    return aPeekBuffer.peekFirst();
  }

  protected void add(XMLEvent event) {
    aPeekBuffer.addLast(event);
  }

  protected void addAll(Collection<? extends XMLEvent> events) {
    aPeekBuffer.addAll(events);
  }

  @Override
  public void close() throws XMLStreamException {
    aPeekBuffer.clear();
    aPeekBuffer = null;
  }

}
