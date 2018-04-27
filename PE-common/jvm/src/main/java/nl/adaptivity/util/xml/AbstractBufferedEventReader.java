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

package nl.adaptivity.util.xml;

import nl.adaptivity.xml.XmlUtil;
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
    while(mPeekBuffer.size()>0 && mPeekBuffer.peekLast().isCharacters() && XmlUtil.isXmlWhitespace(mPeekBuffer.peekLast()
                                                                                                                .asCharacters()
                                                                                                                .getData())) {
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
