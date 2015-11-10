package nl.adaptivity.util.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.NoSuchElementException;


public abstract class AbstractBufferedEventReader extends AbstractEventReader {
  @Nullable private ArrayDeque<XMLEvent> aPeekBuffer = new ArrayDeque<>();

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
    } catch (@NotNull final XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  protected void stripWhiteSpaceFromPeekBuffer() {
    while(aPeekBuffer.size()>0 && aPeekBuffer.peekLast().isCharacters() && XmlUtil.isXmlWhitespace(aPeekBuffer.peekLast().asCharacters().getData())) {
      aPeekBuffer.removeLast();
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  protected boolean isPeekBufferEmpty() {
    return aPeekBuffer.isEmpty();
  }

  protected XMLEvent peekFirst() {
    return aPeekBuffer.peekFirst();
  }

  protected void add(final XMLEvent event) {
    aPeekBuffer.addLast(event);
  }

  protected void addAll(final Collection<? extends XMLEvent> events) {
    aPeekBuffer.addAll(events);
  }

  @Override
  public void close() throws XMLStreamException {
    aPeekBuffer.clear();
    aPeekBuffer = null;
  }

}
