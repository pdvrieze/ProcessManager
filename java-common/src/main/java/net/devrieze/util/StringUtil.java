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

/*
 * Created on Jan 9, 2004
 *
 */

package net.devrieze.util;

import net.devrieze.lang.Const;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.*;


/**
 * A utility class for Strings.
 *
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */
public final class StringUtil {


  private static class IndentingWriter extends Writer {

    private final char[] mBuffer;

    private final Writer mTarget;

    private boolean mLastSeenNewline;

    public IndentingWriter(final int pLevel, final Writer pTarget) {
      mBuffer = new char[pLevel];
      mTarget = pTarget;
      for (int i = 0; i < pLevel; i++) {
        mBuffer[i] = ' ';
      }
      mLastSeenNewline = true;
    }

    @Override
    public void close() throws IOException {
      mTarget.close();
    }

    @Override
    public void flush() throws IOException {
      mTarget.flush();
    }

    @Override
    public void write(final char[] pCbuf, final int pOff, final int pLen) throws IOException {
      final int end = pOff + pLen;
      int nextToWrite = pOff;
      for (int i = pOff; i < end; ++i) {
        final char c = pCbuf[i];
        if ((c == Const._CR) || (c == Const._LF)) {
          final int lastChar = i - 1;
          final char d = (i + 1) >= end ? 0 : pCbuf[i + 1];
          if ((c != d) && ((d == Const._CR) || (d == Const._LF))) {
            ++i;
          }
          if (lastChar != nextToWrite) {
            // Skip indent in case newline follows directly
            if (mLastSeenNewline) {
              mTarget.write(mBuffer);
            }
          }
          mTarget.write(pCbuf, nextToWrite, (i - nextToWrite) + 1);
          nextToWrite = i + 1;
          mLastSeenNewline = true;
        }
      }
      if (nextToWrite < end) {
        if (mLastSeenNewline) {
          mTarget.write(mBuffer);
        }
        mTarget.write(pCbuf, nextToWrite, end - nextToWrite);
        mLastSeenNewline = false;
      }
    }

  }

  /**
   * @deprecated Use {@link Const#_CR}
   */
  @Deprecated
  public static final char _CR = Const._CR;

  /**
   * @deprecated Use {@link Const#_LF}
   */
  @Deprecated
  public static final char _LF = Const._LF;

  private static final int _FORMAT_SLACK = 20;

  private static final int[] _WHITESPACE = new int[] { 32, 10, 13, 9, 0, 12 };

  private static final int _AVG_WORD_SIZE = 8;

  /**
   * Do not allow creating instances.
   */
  private StringUtil() {
    /* Do not alow an instance */
  }

  /**
   * Utility method to determine whether a string is null or empty
   * @param value The value to check.
   * @return <code>true</code> if it is empty, <code>false</code> if not
   */
  @Contract(value="null -> true", pure=true)
  public static boolean isNullOrEmpty(final CharSequence value) {
    return value==null || value.length()==0;
  }

  public static String toLowerCase(final CharSequence string) {
    StringBuilder result = new StringBuilder(string.length());
    final int l = string.length();
    for(int i=0; i<l; ++i) {
      result.append(Character.toLowerCase(string.charAt(i)));
    }
    return result.toString();
  }

  public static int indexOf(final CharSequence text, final char c) {
    int length = text.length();
    for(int i = 0; i< length; ++i) {
      if (text.charAt(i)==c) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Creates a string of the obj. Unlike {@link Objects#toString(Object)} it returns null on a null parameter.
   * @param obj The object to convert
   * @return The result of calling @{link #toString()} on the object.
   */
  @Nullable
  @Contract(value="null -> null; !null -> !null", pure=true)
  public static String toString(final CharSequence obj) {
    return obj==null ? null : obj.toString();
  }

  /**
   * Create a quoted version of the buffer.
   *
   * @param pBuffer The buffer that needs to be quoted
   * @return The result of the quoting
   */
  public static StringBuilder quoteBuilder(final CharSequence pBuffer) {
    final StringBuilder result = new StringBuilder(pBuffer.length() + _FORMAT_SLACK);
    result.append('"');

    for (int i = 0; i < pBuffer.length(); i++) {
      switch (pBuffer.charAt(i)) {
        case '"': {
          result.append("\\\"");

          break;
        }

        case '\\': {
          result.append("\\\\");

          break;
        }

        case '\t': {
          result.append("\\t");
          break;
        }

        case '\n': {
          result.append("\\n");
          break;
        }

        default:
          result.append(pBuffer.charAt(i));
          break;
      }
    }

    result.append('"');
    return result;
  }

  /**
   * Create a quoted version of the buffer.
   *
   * @param pBuffer The buffer that needs to be quoted
   * @return The result of the quoting
   */

  public static StringRep quote(final CharSequence pBuffer) {
    return StringRep.createRep(quoteBuilder(pBuffer));
  }

  /**
   * Create a quoted version of the buffer for use in the script.
   *
   * @param pBuffer The buffer that needs to be quoted
   * @return The result of the quoting
   */
  public static StringRep quote(final char[] pBuffer) {
    return quote(0, pBuffer.length, pBuffer);
  }

  /**
   * Create a quoted version of the buffer for use in the script.
   *
   * @param pStart The start index in the buffer that needs to be quoted
   * @param pEnd The end index in the buffer to be quoted (exclusive)
   * @param pBuffer The buffer that needs to be quoted
   * @return The result of the quoting
   */
  public static StringRep quote(final int pStart, final int pEnd, final char[] pBuffer) {
    return quote(new String(pBuffer, pStart, pEnd));
  }

  /**
   * Create a quoted version of the buffer for use in the script.
   *
   * @param pBuffer The buffer that needs to be quoted
   * @return The result of the quoting
   */
  public static StringBuffer quoteBuf(final String pBuffer) {
    return quoteBuf(pBuffer.toCharArray());
  }

  /**
   * Create a quoted version of the buffer for use in the script.
   *
   * @param pBuffer The buffer that needs to be quoted
   * @return The result of the quoting
   */
  public static StringBuffer quoteBuf(final StringBuffer pBuffer) {
    final char[] buffer = new char[pBuffer.length()];
    pBuffer.getChars(0, buffer.length, buffer, 0);

    return quoteBuf(buffer);
  }

  /**
   * Create a quoted version of the buffer for use in the script.
   *
   * @param pBuffer The buffer that needs to be quoted
   * @return The result of the quoting
   */
  public static StringBuffer quoteBuf(final char[] pBuffer) {
    return quoteBuf(0, pBuffer.length, pBuffer);
  }

  /**
   * Create a quoted version of the buffer for use in the script.
   *
   * @param pStart The start index in the buffer that needs to be quoted
   * @param pEnd The end index in the buffer to be quoted (exclusive)
   * @param pBuffer The buffer that needs to be quoted
   * @return The result of the quoting
   */
  public static StringBuffer quoteBuf(final int pStart, final int pEnd, final char[] pBuffer) {
    final StringBuffer result = new StringBuffer(pBuffer.length + _FORMAT_SLACK);
    result.append('"');

    for (int i = pStart; i < pEnd; i++) {
      switch (pBuffer[i]) {
        case '"': {
          result.append("\\\"");
          break;
        }

        case '\\': {
          result.append("\\\\");
          break;
        }

        default:
          result.append(pBuffer[i]);
          break;
      }
    }

    result.append('"');

    return result;
  }

  /**
   * @deprecated In favour of {@link #isEqual(CharSequence, CharSequence)}.
   * @return <code>true</code> if eq<code>false</code> if not.
   */
  @Deprecated
  public static boolean sequencesEqual(final CharSequence pSeq1, final CharSequence pSeq2) {
    return isEqual(pSeq1, pSeq2);
  }

  /**
   * Compare the two sequences for equality. This can return <code>true</code>
   * for objects of different classes as long as the sequences are equal.
   * Besides allowing arbitrary CharSequences, this also differs from {@link
   *  String#equals(Object)} in that either value may be <code>null</code>
   * and it will return <code>true</code> when both sequences are
   * <code>null</code>.
   *
   * @param pSequence1 The first sequence.
   * @param pSequence2 The second sequence.
   * @return <code>true</code> if equal, <code>false</code> if not.
   */
  public static boolean isEqual(final CharSequence pSequence1, final CharSequence pSequence2) {
    if (pSequence1 == pSequence2) {
      return true;
    }
    if ((pSequence1 == null) || (pSequence2 == null)) {
      return false;
    }

    if (pSequence1.length() != pSequence2.length()) {
      return false;
    }

    for (int i = 0; i < pSequence1.length(); i++) {
      if (pSequence1.charAt(i) != pSequence2.charAt(i)) {
        return false;
      }
    }
    return true;

  }

  public static String simpleClassName(final Class<?> pClass) {
    String result = pClass.getName();
    final int i = result.lastIndexOf('.');
    if (i >= 0) {
      result = result.substring(i + 1);
    }
    return result;
  }

  public static CharSequence charRepeat(final int pCount, final char pChar) {
    return new CharSequence() {

      @Override
      public char charAt(final int pIndex) {
        return pChar;
      }

      @Override
      public int length() {
        return pCount;
      }

      @Override
      public CharSequence subSequence(final int pStart, final int pEnd) {
        return charRepeat(pEnd - pStart, pChar);
      }

      @Override
      public String toString() {
        return new StringBuilder(pCount).append(this).toString();
      }

    };
  }

  public static StringBuilder addChars(final StringBuilder pBuilder, final int pCount, final char pChar) {
    for (int i = 0; i < pCount; ++i) {
      pBuilder.append(pChar);
    }
    return pBuilder;
  }

  /**
   * Indent the given string.
   *
   * @param pLevel The level of indentation that should be added.
   * @param pString The string to be indented.
   * @return The indented string
   */
  public static String indent(final int pLevel, final CharSequence pString) {
    final StringBuilder result = new StringBuilder(pString.length() + pLevel);
    indentTo(result, pLevel, pString);
    return result.toString();
  }

  public static Writer indent(final int level, final Writer source) {
    return new IndentingWriter(level, source);
  }

  public static StringBuilder indentTo(final StringBuilder target, final int level, final CharSequence string) {
    target.ensureCapacity(target.length() + (2 * level) + string.length());
    for (int i = 0; i < level; i++) {
      target.append(' ');
    }
    int i = 0;
    while (i < string.length()) {
      int j = i;
      char c = string.charAt(j);
      while ((j < string.length()) && ((c != Const._CR) && (c != Const._LF))) {
        j++;
        c = string.charAt(j);
      }
      while ((j < string.length()) && ((c == Const._CR) || (c == Const._LF))) {
        j++;
        c = string.charAt(j);
      }
      target.append(string.subSequence(i, j));
      i = j;
      if (j < string.length()) {
        for (int k = 0; k < level; k++) {
          target.append(' ');
        }
      }
      i++;
    }
    return target;
  }

  public static boolean isWhite(final char pChar) {
    for (final int element : _WHITESPACE) {
      if (pChar == element) {
        return true;
      }
    }
    return false;
  }

  /**
   * Split the string into parts with the needle as splitter.
   *
   * @param pString The string to split.
   * @param pNeedle The character to split on.
   * @return The resulting list of strings.
   */
  public static List<String> split(final String pString, final char pNeedle) {
    final ArrayList<String> result = new ArrayList<>();
    int i0 = 0;
    int i1 = pString.indexOf(pNeedle);
    while (i0 < i1) {
      result.add(pString.substring(i0, i1));
      i0 = i1 + 1;
      i1 = pString.indexOf(pNeedle, i0);
    }
    if (i0 == pString.length()) {
      result.add("");
    } else if (i0 < pString.length()) {
      result.add(pString.substring(i0));
    }
    return result;
  }

  /**
   * Check whether the string contains the word. A word is anything that is
   * surrounded by either the end or start of the string or characters for which
   * {@link #isLetter(char)} is false.
   *
   * @param pString The string to search in.
   * @param pWord The word to search for.
   * @return <code>true</code> if the word is contained.
   */
  public static boolean containsWord(final String pString, final String pWord) {
    int i = pString.indexOf(pWord);
    while (i >= 0) {
      final boolean start = (i == 0) || !isLetter(pString.charAt(i - 1));
      final boolean end = ((i + pWord.length()) >= pString.length()) || !isLetter(pString.charAt(i + pWord.length()));
      if (start && end) {
        return true;
      }
      i = pString.indexOf(pWord, i + 1);
    }
    return false;
  }

  /**
   * Check whether the character is a letter.
   *
   * @param pC The character to check
   * @return true if it is, false if not.
   */
  public static boolean isLetter(final char pC) {
    return ((pC >= 'a') && (pC <= 'z')) || ((pC >= 'A') && (pC <= 'Z')) || ((pC >= '0') && (pC <= '9')) || (pC == '-');
  }

  /**
   * Split the string into lines.
   *
   * @param pStr The string to split.
   * @return The lines in the string.
   */
  public static String[] splitLines(final String pStr) {
    final ArrayList<String> result = new ArrayList<>();
    final int offset = 0;
    int i = 0;
    while (i < pStr.length()) {
      if (pStr.charAt(i) == Const._CR) {
        result.add(pStr.substring(offset, i));
        if (((i + 1) < pStr.length()) && (pStr.charAt(i + 1) == Const._LF)) {
          i++;
        }
      } else {
        if (pStr.charAt(i) == Const._LF) {
          result.add(pStr.substring(offset, i));
          if (((i + 1) < pStr.length()) && (pStr.charAt(i + 1) == Const._CR)) {
            i++;
          }
        }
      }
      i++;
    }
    return result.toArray(new String[] {});
  }

  /**
   * Prefix the given string with <code>a</code> or <code>an</code> depending on
   * the first character.
   *
   * @param pString The string to prefix.
   * @return The string resulting of prefixing the code.
   */
  public static String prefixA(final String pString) {
    final char first = Character.toLowerCase(pString.charAt(0));
    switch (first) {
      case 'a':
      case 'e':
      case 'i':
      case 'o':
      case 'u':
        return "an " + pString;
      default:
        return "a " + pString;
    }
  }

  /**
   * Get the index of the character in the string, ignoring quotes.
   *
   * @param pString The string to search in.
   * @param pC The character to search.
   * @return The result. Or <code>-1</code> when not found.
   */
  public static int quoteIndexOf(final CharSequence pString, final char pC) {
    return quoteIndexOf(pString, pC, 0);
  }

  /**
   * Get the index of the character in the string, ignoring quotes.
   *
   * @param pString The string to search in.
   * @param pC The character to search.
   * @param pStartIndex The position to start searching at.
   * @return The result. Or <code>-1</code> when not found.
   */
  public static int quoteIndexOf(final CharSequence pString, final char pC, final int pStartIndex) {
    int i = pStartIndex;
    while (i < pString.length()) {
      final char c = pString.charAt(i);
      if (c == pC) {
        return i;
      } else if (c == '\\') {
        i++; // skip next character
      } else if (c == '\'') {
        char d = pString.charAt(i + 1);
        while (d != c) {
          i++;
          d = pString.charAt(i + 1);
        }
      } else if (c == '\"') {
        i++;
        char d = pString.charAt(i);
        while (d != c) {
          i++;
          d = pString.charAt(i);
          if (d == '\\') {
            i += 2;
            d = pString.charAt(i);
          }
        }
      }
      i++;
    }
    return -1;
  }

  /**
   * Join the words with the given separator.
   *
   * @param pSep The separator
   * @param pWords The words.
   * @return String The resulting string.
   */
  public static String join(final String pSep, final Iterable<String> pWords) {
    final StringBuilder result;
    if (pWords instanceof Collection) {
      result = new StringBuilder(((Collection)pWords).size() * (_AVG_WORD_SIZE + pSep.length()));
    } else {
      result = new StringBuilder();
    }
    final Iterator<String> it = pWords.iterator();
    if (!it.hasNext()) {
      return "".intern();
    }
    result.append(it.next());
    while (it.hasNext()) {
      result.append(pSep).append(it.next());
    }
    return result.toString();
  }

  /**
   * Output as string with at least a certain length. The string is padded with
   * zeros.
   *
   * @param pInt The integer to output
   * @param pLength the length.
   * @return the resulting string
   */
  public static String toLengthString(final int pInt, final int pLength) {
    final StringBuilder result = new StringBuilder(pLength);
    result.append(pInt);
    while (result.length() < pLength) {
      result.insert(0, '0');
    }
    return result.toString();
  }

}
