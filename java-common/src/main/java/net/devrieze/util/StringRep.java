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

/*
 * Created on Jan 9, 2004
 *
 */

package net.devrieze.util;

/**
 * <p>
 * As there is no easy uniform way to access chararrays, stringbuffers and
 * strings, this class is a wrapper for any of them.
 * </p>
 * <p>
 * Note that the respective functions can return the original data that was
 * entered in the wrapper. They do not copy it when not necessary.
 * </p>
 * 
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 */
public abstract class StringRep implements CharSequence {

  /** The default quotes that are used. */
  @SuppressWarnings("FieldNamingConvention")
  public static final String _DEFAULTQUOTES = "\"\"\'\'()[]{}";

  /**
   * A class implementing StringRep for an array of characters.
   * 
   * @author Paul de Vrieze
   * @version 1.0 $Revision$
   */
  private static final class CharArrayStringRep extends StringRep {

    private final char[] mElement;

    /**
     * Create a new CharArrayStringRep based on the given character array.
     * 
     * @param element The character array to be wrapped.
     */
    private CharArrayStringRep(final char[] element) {
      mElement = element;
    }

    @Override
    public char charAt(final int pIndex) {
      return mElement[pIndex];
    }

    @Override
    public int length() {
      return mElement.length;
    }

    @Override
    public char[] toCharArray() {
      return mElement.clone();
    }

    @Override
    public String toString() {
      return new String(mElement);
    }

    @Override
    public StringBuffer toStringBuffer() {
      final StringBuffer result = new StringBuffer(mElement.length);
      result.append(mElement);

      return result;
    }

    @Override
    public StringBuilder toStringBuilder() {
      final StringBuilder result = new StringBuilder(mElement.length);
      result.append(mElement);

      return result;
    }
  }

  /**
   * A class that represents a stringrep for a character array that is only
   * partly used for the string.
   * 
   * @author Paul de Vrieze
   * @version 1.0 $Revision$
   */
  private static final class IndexedCharArrayStringRep extends StringRep {

    private final char[] mElement;

    private final int mBegin;

    private final int mEnd;

    /**
     * Create a new StringRep based on this character array.
     * 
     * @param pBegin the index of the first character
     * @param pEnd the index of the first character not belonging to the string
     * @param element the string to base the rep of.
     */
    private IndexedCharArrayStringRep(final int pBegin, final int pEnd, final char[] element) {
      if ((pEnd < pBegin) || (pBegin < 0) || (pEnd >= element.length)) {
        throw new IndexOutOfBoundsException();
      }

      mBegin = pBegin;
      mEnd = pEnd;
      mElement = element;
    }

    @Override
    public char charAt(final int pIndex) {
      final int index = pIndex + mBegin;

      if (index >= mEnd) {
        throw new IndexOutOfBoundsException(Integer.toString(pIndex) + " >= " + Integer.toString(mEnd - mBegin));
      }

      return mElement[pIndex + mBegin];
    }

    @Override
    public int length() {
      return mEnd - mBegin;
    }

    @Override
    public char[] toCharArray() {
      final char[] result = new char[length()];
      System.arraycopy(mElement, mBegin, result, 0, result.length);

      return result;
    }

    @Override
    public String toString() {
      return new String(mElement, mBegin, mEnd - mBegin);
    }

    @Override
    public StringBuffer toStringBuffer() {
      final int length = mEnd - mBegin;
      final StringBuffer result = new StringBuffer(length);
      result.append(mElement, mBegin, length);

      return result;
    }

    @Override
    public StringBuilder toStringBuilder() {
      final int length = mEnd - mBegin;
      final StringBuilder result = new StringBuilder(length);
      result.append(mElement, mBegin, length);

      return result;
    }
  }

  private static final class RepStringRep extends StringRep {

    /**
     * The StringRep to base this Stringrep of.
     */
    private final StringRep mElement;

    /**
     * The start index.
     */
    private final int mBegin;

    /**
     * The end index.
     */
    private final int mEnd;

    /**
     * Create a new RepStringRep.
     * 
     * @param pBegin the start index of the substring
     * @param pEnd the end index of the substring
     * @param element The string to base this one of
     */
    private RepStringRep(final int pBegin, final int pEnd, final StringRep element) {
      if ((pEnd < pBegin) || (pBegin < 0) || (pEnd > element.length())) {
        throw new IndexOutOfBoundsException();
      }

      mBegin = pBegin;
      mEnd = pEnd;
      mElement = element;
    }

    @Override
    public char charAt(final int pIndex) {
      final int index = pIndex + mBegin;

      if (index >= mEnd) {
        throw new IndexOutOfBoundsException(Integer.toString(pIndex) + " >= " + Integer.toString(mEnd - mBegin));
      }

      return mElement.charAt(pIndex + mBegin);
    }

    @Override
    public StringRep substring(final int pStart, final int pEnd) {
      if ((pStart < 0) || (pEnd > length())) {
        throw new IndexOutOfBoundsException();
      }

      final int begin = mBegin + pStart;
      final int end = (begin + pEnd) - pStart;

      return createRep(begin, end, mElement);
    }

    @Override
    public int length() {
      return mEnd - mBegin;
    }

    @Override
    public char[] toCharArray() {
      final char[] origCharArray = mElement.toCharArray();
      final char[] result = new char[length()];
      System.arraycopy(origCharArray, mBegin, result, 0, result.length);

      return result;
    }

    @Override
    public String toString() {
      return new String(mElement.toCharArray(), mBegin, mEnd - mBegin);
    }

    @Override
    public StringBuffer toStringBuffer() {
      final int length = mEnd - mBegin;
      final StringBuffer foo = new StringBuffer(length);
      foo.append(mElement.toCharArray(), mBegin, length);

      return foo;
    }

    @Override
    public StringBuilder toStringBuilder() {
      final int length = mEnd - mBegin;
      final StringBuilder foo = new StringBuilder(length);
      foo.append(mElement.toCharArray(), mBegin, length);

      return foo;
    }
  }

  private static final class CharSequenceStringRep extends StringRep {

    private final CharSequence mElement;

    /**
     * Create a new StringBufferStringRep based on this element.
     * 
     * @param element the stringbuffer to base of
     */
    private CharSequenceStringRep(final CharSequence element) {
      mElement = element;
    }

    @Override
    public char charAt(final int pIndex) {
      return mElement.charAt(pIndex);
    }

    @Override
    public int length() {
      return mElement.length();
    }

    @Override
    public char[] toCharArray() {
      final char[] buffer = new char[mElement.length()];
      for (int i = 0; i < buffer.length; i++) {
        buffer[i] = mElement.charAt(i);
      }
      return buffer;
    }

    @Override
    public String toString() {
      return mElement.toString();
    }

    @Override
    public StringBuffer toStringBuffer() {
      if (mElement instanceof StringBuffer) {
        return (StringBuffer) mElement;
      }
      return new StringBuffer(mElement);
    }

    @Override
    public StringBuilder toStringBuilder() {
      if (mElement instanceof StringBuilder) {
        return (StringBuilder) mElement;
      }
      return new StringBuilder(mElement);
    }

  }

  private static final class StringStringRep extends StringRep {

    private final String mElement;

    /**
     * Create a new StringStringRep based on this string.
     * 
     * @param element the string to base the rep on.
     */
    private StringStringRep(final String element) {
      mElement = element;
    }

    @Override
    public char charAt(final int pIndex) {
      return mElement.charAt(pIndex);
    }

    @Override
    public int length() {
      return mElement.length();
    }

    @Override
    public char[] toCharArray() {
      return mElement.toCharArray();
    }

    @Override
    public String toString() {
      return mElement;
    }

    @Override
    public StringBuffer toStringBuffer() {
      return new StringBuffer(mElement);
    }

    @Override
    public StringBuilder toStringBuilder() {
      return new StringBuilder(mElement);
    }
  }

  private static final class StringRepIteratorImpl implements StringRepIterator {

    private StringRep mLast = null;

    private final StringRep mStringRep;

    private char[] mCloseQuotes;

    private char[] mOpenQuotes;

    private final boolean mQuotes;

    private final char mToken;

    private int mPos = 0; /* current position in the stream */

    /**
     * Create a new iterator.
     * 
     * @param pStringRep The StringRep to iterate over
     * @param pToken The token that splits the elements
     * @param pQuotes A string in which quotes are presented pairwise. For
     *          example at pQuotes[0] the value is '(' and pQuotes[1]==')'
     */
    private StringRepIteratorImpl(final StringRep pStringRep, final char pToken, final CharSequence pQuotes) {
      this(pStringRep, pToken, true);
      mOpenQuotes = new char[pQuotes.length() / 2];
      mCloseQuotes = new char[mOpenQuotes.length];

      for (int i = 0; i < mOpenQuotes.length; i++) {
        mOpenQuotes[i] = pQuotes.charAt(i * 2);
        mCloseQuotes[i] = pQuotes.charAt((i * 2) + 1);
      }
    }

    /**
     * Create a new iterator.
     * 
     * @param pStringRep The StringRep to iterate over
     * @param pToken The token that splits the elements
     * @param pQuotes {@code true} if quotes need to be taken into account,
     *          {@code false} if not
     */
    private StringRepIteratorImpl(final StringRep pStringRep, final char pToken, final boolean pQuotes) {
      mStringRep = pStringRep;
      mToken = pToken;
      mQuotes = pQuotes;

      if (mStringRep.length() > mPos) {
        /* trim on whitespace from left */
        char c = mStringRep.charAt(mPos);

        while ((mPos < mStringRep.length()) && ((c == ' ') || (c == '\n') || (c == '\t'))) {
          mPos++;
          if (mPos < mStringRep.length()) {
            c = mStringRep.charAt(mPos);
          }
        }
      }
    }

    @Override
    public boolean hasNext() {
      /*
       * the check on mLast is necessary to have empty strings not return
       * anything
       */
      if ((mPos == mStringRep.length()) && (mLast != null)) {
        return true;
      }

      return mPos < mStringRep.length();
    }

    /**
     * Get the previous element.
     * 
     * @return the previously returned value.
     */
    @Override
    public StringRep last() {
      return mLast;
    }

    /**
     * Get the next element.
     * 
     * @return the next element to be returned
     */
    @Override
    public StringRep next() {
      if (mPos == mStringRep.length()) {
        mPos++; /* remember to increase, else we get an endless loop */

        return createRep("");
      }

      final int newPos;

      if (mQuotes) {
        newPos = nextPosQuoted();
      } else {
        newPos = nextPos();
      }

      int right = newPos - 1;

      /* trim on right whitespace */
      char c = mStringRep.charAt(right);
      while ((mPos <= right) && ((c == ' ') || (c == '\n') || (c == '\t'))) {
        right--;
        c = mStringRep.charAt(right);
      }

      mLast = mStringRep.substring(mPos, right + 1);
      mPos = newPos + 1; /* increase as at newPos is the token. */

      if (mPos < mStringRep.length()) {
        /* trim on whitespace from left */
        c = mStringRep.charAt(mPos);
        while ((mPos < mStringRep.length()) && ((c == ' ') || (c == '\n') || (c == '\t'))) {
          mPos++;
          c = mStringRep.charAt(mPos);
        }
      }

      return mLast;
    }

    private int nextPos() {
      for (int i = mPos; i < mStringRep.length(); i++) {
        if (mStringRep.charAt(i) == mToken) {
          return i;
        }
      }

      return mStringRep.length();
    }

    private int nextPosQuoted() {
      /* TODO refactor this function */
      boolean quote = false;

      if (mOpenQuotes == null) {
        int i = mPos;
        while (i < mStringRep.length()) {
          final char c = mStringRep.charAt(i);

          if (quote) {
            if (c == '\\') {
              i++; /* skip next char */
            } else if (c == '"') {
              quote = false;
            }
          } else if (c == mToken) {
            return i;
          } else if (c == '"') {
            quote = true;
          }
          i++;
        }
      } else {
        final IntStack stack = new IntStack();

        int i = mPos;
        while (i < mStringRep.length()) {
          final char c = mStringRep.charAt(i);

          if (!stack.isEmpty()) {
            if (c == '\\') {
              i++; /* skip next char */
            } else if (c == mCloseQuotes[stack.peek()]) {
              stack.pop();
            } else {
              for (int j = 0; j < mOpenQuotes.length; j++) {
                if (c == mOpenQuotes[j]) {
                  stack.push(j);

                  break;
                }
              }
            }
          } else if (c == mToken) {
            return i;
          } else {
            for (int j = 0; j < mOpenQuotes.length; j++) {
              if (c == mOpenQuotes[j]) {
                stack.push(j);

                break;
              }
            }
          }
          i++;
        }

        if (!stack.isEmpty()) {
          quote = true;
        }
      }

      if (quote) {
        throw new NumberFormatException("Closing quote missing in a quoted split");
      }

      return mStringRep.length();
    }
  }

  /**
   * Create a new StringRep.
   * 
   * @param element The string to be encapsulated
   * @return a new StringRep
   */
  public static StringRep createRep(final String element) {
    return new StringStringRep(element);
  }

  /**
   * Create a new StringRep.
   * 
   * @param element The string to be encapsulated
   * @return a new StringRep
   */
  public static StringRep createRep(final CharSequence element) {
    return new CharSequenceStringRep(element);
  }

  /**
   * Create a new StringRep.
   * 
   * @param element The string to be encapsulated
   * @return a new StringRep
   */
  public static StringRep createRep(final char[] element) {
    return new CharArrayStringRep(element);
  }

  /**
   * Create a new StringRep.
   * 
   * @param pStart The starting index
   * @param pEnd The end index
   * @param element The string to be encapsulated
   * @return a new StringRep
   */
  public static StringRep createRep(final int pStart, final int pEnd, final char[] element) {
    return new IndexedCharArrayStringRep(pStart, pEnd, element);
  }

  /**
   * Create a new StringRep.
   * 
   * @param start The starting index
   * @param end The end index
   * @param element The string to be encapsulated
   * @return a new StringRep
   * @throws IndexOutOfBoundsException When the index values are invalid
   */
  public static StringRep createRep(final int start, final int end, final StringRep element) {
    return element.substring(start, end);
  }

  /**
   * Append the text to the buffer.
   * 
   * @param pBuffer The buffer to which the text must be appended
   * @return the stringbuffer
   * @deprecated Replaced by implementation of CharSequence
   */
  @Deprecated
  public final StringBuffer bufferAppend(final StringBuffer pBuffer) {
    pBuffer.append(this);
    return pBuffer;
  }

  /**
   * Insert itself into a stringbuffer.
   * 
   * @param pIndex The index of insertion
   * @param pIn The stringbuffer
   * @return the stringbuffer
   * @deprecated Replaced by implementation of CharSequence
   */
  @Deprecated
  public final StringBuffer bufferInsert(final int pIndex, final StringBuffer pIn) {
    pIn.insert(pIndex, this);
    return pIn;
  }

  /**
   * append the string to a new rep.
   * 
   * @param pRep the to be appended string
   * @return the result
   */
  public StringRep appendCombine(final CharSequence pRep) {
    final StringBuilder b = toStringBuilder();
    b.append(pRep);

    return createRep(b);
  }

  /**
   * Check whether the rep ends with the the character.
   * 
   * @param pChar the character
   * @return {@code true} if it is the last char
   */
  public boolean endsWith(final char pChar) {
    return charAt(length() - 1) == pChar;
  }

  /**
   * Get the index of a character in the buffer.
   * 
   * @param pChar The character to be found
   * @param pQuote If {@code true} take quotes into account
   * @return -1 if not found, else the index of the character
   */
  public int indexOf(final char pChar, final boolean pQuote) {
    return indexOf(pChar, 0, pQuote);
  }

  /**
   * Get the index of a character in the buffer.
   * 
   * @param pChar The character to be found
   * @param pQuotes The quotes to take into account
   * @return -1 if not found, else the index of the character
   */
  public int indexOf(final char pChar, final CharSequence pQuotes) {
    return indexOf(pChar, 0, pQuotes);
  }

  /**
   * Get the index of a character in the buffer. Not that the startindex should
   * not be within a quoted area if quotes are taken into account.
   * 
   * @param pChar The character to be found
   * @param pStartIndex the index where searching should start
   * @param pQuote If {@code true} take quotes into account
   * @return -1 if not found, else the index of the character
   * @throws NumberFormatException When quotes in the string are unmatched
   */
  public int indexOf(final char pChar, final int pStartIndex, final boolean pQuote) {
    boolean quote = false;

    for (int i = pStartIndex; i < length(); i++) {
      if (pQuote && (charAt(i) == '"')) {
        if (quote) {
          if ((i == 0) || (charAt(i - 1) != '\\')) {
            quote = false;
          }
        } else {
          quote = true;
        }
      } else if (!quote && (charAt(i) == pChar)) {
        return i;
      }
    }

    if (quote) {
      throw new NumberFormatException("Closing quote missing in a quoted indexOf");
    }

    return -1;
  }

  /**
   * Get the index of a character in the buffer. Not that the startindex should
   * not be within a quoted area if quotes are taken into account.
   * 
   * @param pChar The character to be found
   * @param pStartIndex the index where searching should start
   * @param pQuotes The quote characters, Alternating the starting quote and the
   *          closing quote. The opening quotes at the even indices, the closing
   *          at the odd.
   * @return -1 if not found, else the index of the character
   * @throws NumberFormatException When quotes are unmatched
   */
  public int indexOf(final char pChar, final int pStartIndex, final CharSequence pQuotes) {
    /* TODO refactor this function */
    boolean quote = false;

    if (pQuotes == null) {
      int i = pStartIndex;
      while (i < length()) {
        final char c = charAt(i);

        if (quote) {
          if (c == '\\') {
            i++; /* skip next char */
          } else if (c == '"') {
            quote = false;
          }
        } else if (c == pChar) {
          return i;
        } else if (c == '"') {
          quote = true;
        }
        i++;
      }
    } else {
      final char[] openQuotes = new char[pQuotes.length() / 2];
      final char[] closeQuotes = new char[openQuotes.length];

      for (int i = 0; i < openQuotes.length; i++) {
        openQuotes[i] = pQuotes.charAt(i * 2);
        closeQuotes[i] = pQuotes.charAt((i * 2) + 1);
      }
      final IntStack stack = new IntStack();

      int i = pStartIndex;
      while (i < length()) {
        final char c = charAt(i);

        if (!stack.isEmpty()) {
          if (c == '\\') {
            i++; /* skip next char */
          } else if (c == closeQuotes[stack.peek()]) {
            stack.pop();
          } else {
            for (int j = 0; j < openQuotes.length; j++) {
              if (c == openQuotes[j]) {
                stack.push(j);

                break;
              }
            }
          }
        } else if (c == pChar) {
          return i;
        } else {
          for (int j = 0; j < openQuotes.length; j++) {
            if (c == openQuotes[j]) {
              stack.push(j);

              break;
            }
          }
        }
        i++;
      }

      if (!stack.isEmpty()) {
        quote = true;
      }
    }

    if (quote) {
      throw new NumberFormatException("Closing quote missing in a quoted indexOf");
    }

    return -1;
  }

  /**
   * Insert a string into the rep.
   * 
   * @param pIndex the index
   * @param pIn the stringrep to be inserted
   * @return the resulting rep
   */
  public StringRep insertCombine(final int pIndex, final CharSequence pIn) {
    final StringBuilder b = toStringBuilder();
    b.insert(pIndex, pIn);

    return createRep(b);
  }

  /**
   * Create a quoted representation of this rep.
   * 
   * @return the new quoted rep
   */
  public StringRep quote() {
    return StringUtil.quote(this);
  }

  /**
   * Create an iterator for splitting a stringrep into substrings separated by
   * the token.
   * 
   * @param pToken the token
   * @param pQuotes if {@code true}, double quotes are recognised
   * @return the iterator
   */
  public StringRepIterator splitOn(final char pToken, final boolean pQuotes) {
    return new StringRepIteratorImpl(this, pToken, pQuotes);
  }

  /**
   * Create an iterator for splitting a stringrep into substrings separated by
   * the token.
   * 
   * @param pToken the token
   * @param pQuotes if {@code true}, double quotes are recognised
   * @return the iterator
   */
  public StringRepIterator splitOn(final char pToken, final CharSequence pQuotes) {
    return new StringRepIteratorImpl(this, pToken, pQuotes);
  }

  /**
   * Create an iterator for splitting a stringrep into substrings separated by
   * the token, however taking quotes into account.
   * 
   * @param pToken the token
   * @return the iterator
   */
  public StringRepIterator splitOn(final char pToken) {
    return new StringRepIteratorImpl(this, pToken, _DEFAULTQUOTES);
  }

  /**
   * Check whether the rep starts with the the character.
   * 
   * @param pChar the character
   * @return {@code true} if it is the first char
   */
  public boolean startsWith(final char pChar) {
    if (length() == 0) {
      return false;
    }
    return charAt(0) == pChar;
  }

  /**
   * a chararray representation.
   * 
   * @return a chararray
   */
  public abstract char[] toCharArray();

  /**
   * The stringbuffer representation.
   * 
   * @return the stringbuffer
   */
  public abstract StringBuffer toStringBuffer();

  /**
   * The stringbuffer representation.
   * 
   * @return the stringbuffer
   */
  public abstract StringBuilder toStringBuilder();

  /**
   * Create a substring representation.
   * 
   * @param pStart The starting character
   * @param pEnd The end character (exclusive)
   * @return A new rep (note that this can be the same one, but does not need to
   *         be)
   */
  public StringRep substring(final int pStart, final int pEnd) {
    if ((pStart < 0) || (pEnd > length())) {
      throw new IndexOutOfBoundsException();
    }

    return new RepStringRep(pStart, pEnd, this);
  }

  /**
   * Create a substring representation.
   * 
   * @param pStart The starting character
   * @return A new rep (note that this can be the same one, but does not need to
   *         be)
   */
  public final StringRep substring(final int pStart) {
    return substring(pStart, length());
  }

  /**
   * Get a subsequence. This is actually equal to the substring method.
   *
   */
  @Override
  public CharSequence subSequence(final int pStart, final int pEnd) {
    return substring(pStart, pEnd);
  }

  /**
   * Does the string start with the specified substring.
   * 
   * @param pString The substring
   * @return {@code true} if it starts with it
   */
  public boolean endsWith(final CharSequence pString) {
    if (pString.length() > length()) {
      return false;
    }

    final int length = pString.length();
    final int start = length() - length;

    for (int i = 0; i < length; i++) {
      if (charAt(start + i) != pString.charAt(i)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Does the string start with the specified substring.
   * 
   * @param pString The substring
   * @return {@code true} if it starts with it
   */
  public boolean startsWith(final CharSequence pString) {
    if (pString.length() > length()) {
      return false;
    }

    final int length = pString.length();

    for (int i = 0; i < length; i++) {
      if (charAt(i) != pString.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Create a rep without starting and ending whitespace.
   * 
   * @return a new rep
   */
  public StringRep trim() {
    int left = 0;
    int right = length() - 1;
    char c = charAt(left);

    while ((left <= right) && ((c == ' ') || (c == '\n') || (c == '\t'))) {
      left++;
      c = charAt(left);
    }

    if (left == length()) {
      return createRep("");
    }

    c = charAt(right);
    while ((left <= right) && ((c == ' ') || (c == '\n') || (c == '\t'))) {
      right--;
      c = charAt(right);
    }

    return createRep(left, right + 1, this);
  }

  /**
   * Unquote the string. If it does not start and end with quotes an exception
   * will be thrown.
   * 
   * @return An unquoted string
   * @throws NumberFormatException When the quotes are unmatched
   */
  public StringRep unQuote() {
    final StringBuilder result = new StringBuilder(length());

    if (!(startsWith('"') && endsWith('"'))) {
      throw new NumberFormatException("The element to be unquoted does not start or end with quotes");
    }

    int index = 1;

    while (index < (length() - 1)) {
      if (charAt(index) == '\\') {
        if (index == (length() - 1)) {
          throw new NumberFormatException("last quote is escaped");
        }

        index++;
      } else if (charAt(index) == '"') {
        throw new NumberFormatException("Internal quotes are not allowed unless escaped");
      }

      result.append(charAt(index));
      index++;
    }

    return createRep(result);
  }
}
