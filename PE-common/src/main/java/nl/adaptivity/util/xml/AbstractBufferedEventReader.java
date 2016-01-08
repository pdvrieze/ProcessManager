package nl.adaptivity.util.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.NoSuchElementException;


public abstract class AbstractBufferedEventReader extends AbstractEventReader {
  @Nullable private ArrayDeque<XMLEvent> mPeekBuffer = new ArrayDeque<>();

  @Override
  public XMLEvent nextEvent() throws XMLStreamException {
    if (! mPeekBuffer.isEmpty()) {
      return mPeekBuffer.removeFirst();
    }
    if (! hasNext()) { throw new NoSuchElementException(); }
    peek();
    return mPeekBuffer.removeFirst();
  }

  @Override
  public boolean hasNext() {
    if (! mPeekBuffer.isEmpty()) { return true; }
    try {
      return peek()!=null;
    } catch (@NotNull final XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  protected void stripWhiteSpaceFromPeekBuffer() {
    while(mPeekBuffer.size()>0 && mPeekBuffer.peekLast().isCharacters() && XmlUtil.isXmlWhitespace(mPeekBuffer.peekLast().asCharacters().getData())) {
      mPeekBuffer.removeLast();
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  protected boolean isPeekBufferEmpty() {
    return mPeekBuffer.isEmpty();
  }

  protected XMLEvent peekFirst() {
    return mPeekBuffer.peekFirst();
  }

  protected void add(final XMLEvent event) {
    mPeekBuffer.addLast(event);
  }

  protected void addAll(final Collection<? extends XMLEvent> events) {
    mPeekBuffer.addAll(events);
  }

  @Override
  public void close() throws XMLStreamException {
    mPeekBuffer.clear();
    mPeekBuffer = null;
  }

}
