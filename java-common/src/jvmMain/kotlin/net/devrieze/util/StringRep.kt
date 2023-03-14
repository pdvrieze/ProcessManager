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

/*
 * Created on Jan 9, 2004
 *
 */

@file:Suppress("DEPRECATION")

package net.devrieze.util

/**
 *
 *
 * As there is no easy uniform way to access chararrays, stringbuffers and
 * strings, this class is a wrapper for any of them.
 *
 *
 *
 * Note that the respective functions can return the original data that was
 * entered in the wrapper. They do not copy it when not necessary.
 *
 *
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 */
@Deprecated("Stale class that shouldn't be used")
abstract class StringRep : CharSequence {

    /**
     * A class implementing StringRep for an array of characters.
     *
     * @author Paul de Vrieze
     * @version 1.0 $Revision$
     *
     * @constructor Create a new CharArrayStringRep based on the given character array.
     *
     * @param element The character array to be wrapped.
     */
    private class CharArrayStringRep constructor(private val element: CharArray) : StringRep() {
        override fun get(index: Int): Char {
            return element[index]
        }

        override val length: Int get() = element.size

        override fun toCharArray(): CharArray {
            return element.clone()
        }

        override fun toString(): String {
            return String(element)
        }

        override fun toStringBuffer(): StringBuffer {
            val result = StringBuffer(element.size)
            result.append(element)

            return result
        }

        override fun toStringBuilder(): StringBuilder {
            val result = StringBuilder(element.size)
            result.append(element)

            return result
        }
    }

    /**
     * A class that represents a stringrep for a character array that is only
     * partly used for the string.
     *
     * @author Paul de Vrieze
     * @version 1.0 $Revision$
     *
     * @constructor Create a new StringRep based on this character array.
     *
     * @param begin the index of the first character
     * @param end the index of the first character not belonging to the string
     * @param element the string to base the rep of.
     */
    private class IndexedCharArrayStringRep constructor(
        private val begin: Int,
        private val end: Int,
        private val element: CharArray
    ) : StringRep() {

        init {
            if (end < begin || begin < 0 || end >= element.size) {
                throw IndexOutOfBoundsException()
            }
        }

        override fun get(index: Int): Char {
            val innerIndex = index + begin

            if (innerIndex >= end) {
                throw IndexOutOfBoundsException("$innerIndex >= ${end - begin}")
            }

            return element[innerIndex]
        }

        override val length: Int get() = end - begin

        override fun toCharArray(): CharArray {
            return element.sliceArray(begin until end)
        }

        override fun toString(): String {
            return String(element, begin, end - begin)
        }

        override fun toStringBuffer(): StringBuffer {
            val length = end - begin
            val result = StringBuffer(length)
            result.append(element, begin, length)

            return result
        }

        override fun toStringBuilder(): StringBuilder {
            val length = end - begin
            val result = StringBuilder(length)
            result.append(element, begin, length)

            return result
        }
    }

    /**
     * @constructor Create a new RepStringRep.
     *
     * @param begin the start index of the substring
     * @param end the end index of the substring
     * @param element The string to base this one of
     */
    private class RepStringRep constructor(
        private val begin: Int,
        private val end: Int,
        private val element: StringRep
    ) : StringRep() {

        init {
            if (end < begin || begin < 0 || end > element.length) {
                throw IndexOutOfBoundsException()
            }
        }

        override fun get(index: Int): Char {
            val innerIndex = index + begin

            if (innerIndex >= end) {
                throw IndexOutOfBoundsException(Integer.toString(index) + " >= " + Integer.toString(end - begin))
            }

            return element[innerIndex]
        }

        override fun substring(pStart: Int, pEnd: Int): StringRep {
            if (pStart < 0 || pEnd > length) {
                throw IndexOutOfBoundsException()
            }

            val begin = begin + pStart
            val end = begin + pEnd - pStart

            return createRep(begin, end, element)
        }

        override val length: Int get() = end - begin

        override fun toCharArray(): CharArray {
            return element.toCharArray().sliceArray(begin until end)
        }

        override fun toString(): String {
            return String(element.toCharArray(), begin, end - begin)
        }

        override fun toStringBuffer(): StringBuffer {
            val length = end - begin
            val foo = StringBuffer(length)
            foo.append(element.toCharArray(), begin, length)

            return foo
        }

        override fun toStringBuilder(): StringBuilder {
            val length = end - begin
            val foo = StringBuilder(length)
            foo.append(element.toCharArray(), begin, length)

            return foo
        }
    }

    /**
     * @constructor Create a new StringBufferStringRep based on this element.
     *
     * @param element the stringbuffer to base of
     */
    private class CharSequenceStringRep constructor(private val element: CharSequence) : StringRep() {

        override fun get(index: Int): Char {
            return element[index]
        }

        override val length: Int get() = element.length

        override fun toCharArray(): CharArray {
            val buffer = CharArray(element.length)
            for (i in buffer.indices) {
                buffer[i] = element[i]
            }
            return buffer
        }

        override fun toString(): String {
            return element.toString()
        }

        override fun toStringBuffer(): StringBuffer {
            return element as? StringBuffer ?: StringBuffer(element)
        }

        override fun toStringBuilder(): StringBuilder {
            return element as? StringBuilder ?: StringBuilder(element)
        }

    }

    /**
     * @constructor Create a new StringStringRep based on this string.
     *
     * @param element the string to base the rep on.
     */
    private class StringStringRep constructor(private val element: String) : StringRep() {

        override fun get(index: Int): Char {
            return element[index]
        }

        override val length: Int get() = element.length

        override fun toCharArray(): CharArray {
            return element.toCharArray()
        }

        override fun toString(): String {
            return element
        }

        override fun toStringBuffer(): StringBuffer {
            return StringBuffer(element)
        }

        override fun toStringBuilder(): StringBuilder {
            return StringBuilder(element)
        }
    }

    /**
     * @constructor Create a new iterator.
     *
     * @param stringRep The StringRep to iterate over
     * @param token The token that splits the elements
     * @param quotes `true` if quotes need to be taken into account, `false` if not
     */
    private class StringRepIteratorImpl constructor(
        private val stringRep: StringRep,
        private val token: Char,
        private val quotes: Boolean,
        private val openQuotes: CharArray,
        private val closeQuotes: CharArray
    ) : StringRepIterator {


        constructor(stringRep: StringRep, token: Char, quotes: Boolean) :
            this(stringRep, token, quotes, charArrayOf(), charArrayOf()) {
            if (stringRep.length > pos) {
                /* trim on whitespace from left */
                var c = stringRep[pos]

                while (pos < stringRep.length && (c == ' ' || c == '\n' || c == '\t')) {
                    pos++
                    if (pos < stringRep.length) {
                        c = stringRep[pos]
                    }
                }
            }
        }

        private var last: StringRep? = null

        private var pos = 0 /* current position in the stream */

        /**
         * Create a new iterator.
         *
         * @param stringRep The StringRep to iterate over
         * @param token The token that splits the elements
         * @param quotes A string in which quotes are presented pairwise. For
         * example at pQuotes[0] the value is '(' and pQuotes[1]==')'
         */
        constructor(stringRep: StringRep, token: Char, quotes: CharSequence) :
            this(stringRep, token, true,
                CharArray(quotes.length / 2) { quotes[it * 2] },
                CharArray(quotes.length / 2) { quotes[it * 2 + 1] })


        /*
         *  the check on mLast is necessary to have empty strings not return
         * anything
         */
        override fun hasNext(): Boolean {
            return if (pos == stringRep.length && last != null) {
                true
            } else pos < stringRep.length

        }

        /**
         * Get the previous element.
         *
         * @return the previously returned value.
         */
        override fun last(): StringRep {
            return last!!
        }

        /**
         * Get the next element.
         *
         * @return the next element to be returned
         */
        override fun next(): StringRep {
            if (pos == stringRep.length) {
                pos++ /* remember to increase, else we get an endless loop */

                return createRep("")
            }

            val newPos: Int = when {
                quotes -> nextPosQuoted()
                else -> nextPos()
            }

            var right = newPos - 1

            /* trim on right whitespace */
            var c = stringRep[right]
            while (pos <= right && (c == ' ' || c == '\n' || c == '\t')) {
                right--
                c = stringRep[right]
            }

            last = stringRep.substring(pos, right + 1)
            pos = newPos + 1 /* increase as at newPos is the token. */

            if (pos < stringRep.length) {
                /* trim on whitespace from left */
                c = stringRep[pos]
                while (pos < stringRep.length && (c == ' ' || c == '\n' || c == '\t')) {
                    pos++
                    c = stringRep[pos]
                }
            }

            return last!!
        }

        private fun nextPos(): Int {
            for (i in pos until stringRep.length) {
                if (stringRep[i] == token) {
                    return i
                }
            }

            return stringRep.length
        }

        private fun nextPosQuoted(): Int {
            var quote = false

            if (openQuotes.isEmpty()) {
                var i = pos
                while (i < stringRep.length) {
                    val c = stringRep[i]

                    when {
                        quote -> when (c) {
                            '\\' -> i++ /* skip next char */
                            '"' -> quote = false
                        }

                        c == token -> return i

                        c == '"' -> quote = true
                    }
                    i++
                }
            } else {
                val stack = IntStack()

                var i = pos
                while (i < stringRep.length) {
                    val c = stringRep[i]

                    when {
                        !stack.isEmpty -> when (c) {
                            '\\' -> i++ /* skip next char */
                            closeQuotes[stack.peek()] -> stack.pop()
                            else -> for (j in openQuotes.indices) {
                                if (c == openQuotes[j]) {
                                    stack.push(j)

                                    break
                                }
                            }
                        }

                        c == token -> return i

                        else -> {
                            for (j in openQuotes.indices) {
                                if (c == openQuotes[j]) {
                                    stack.push(j)

                                    break
                                }
                            }
                        }
                    }
                    i++
                }

                if (!stack.isEmpty) {
                    quote = true
                }
            }

            if (quote) {
                throw NumberFormatException("Closing quote missing in a quoted split")
            }

            return stringRep.length
        }
    }

    /**
     * Append the text to the buffer.
     *
     * @param buffer The buffer to which the text must be appended
     * @return the stringbuffer
     */
    @Deprecated("Replaced by implementation of CharSequence")
    fun bufferAppend(buffer: StringBuffer): StringBuffer {
        buffer.append(this)
        return buffer
    }

    /**
     * Insert itself into a stringbuffer.
     *
     * @param index The index of insertion
     * @param inBuffer The stringbuffer
     * @return the stringbuffer
     */
    @Deprecated("Replaced by implementation of CharSequence")
    fun bufferInsert(index: Int, inBuffer: StringBuffer): StringBuffer {
        inBuffer.insert(index, this)
        return inBuffer
    }

    /**
     * append the string to a new rep.
     *
     * @param rep the to be appended string
     * @return the result
     */
    fun appendCombine(rep: CharSequence): StringRep {
        val b = toStringBuilder()
        b.append(rep)

        return createRep(b)
    }

    /**
     * Check whether the rep ends with the the character.
     *
     * @param char the character
     * @return `true` if it is the last char
     */
    fun endsWith(char: Char): Boolean {
        return get(length - 1) == char
    }

    /**
     * Get the index of a character in the buffer.
     *
     * @param pChar The character to be found
     * @param pQuote If `true` take quotes into account
     * @return -1 if not found, else the index of the character
     */
    fun indexOf(pChar: Char, pQuote: Boolean): Int {
        return indexOf(pChar, 0, pQuote)
    }

    /**
     * Get the index of a character in the buffer.
     *
     * @param pChar The character to be found
     * @param pQuotes The quotes to take into account
     * @return -1 if not found, else the index of the character
     */
    fun indexOf(pChar: Char, pQuotes: CharSequence): Int {
        return indexOf(pChar, 0, pQuotes)
    }

    /**
     * Get the index of a character in the buffer. Not that the startindex should
     * not be within a quoted area if quotes are taken into account.
     *
     * @param pChar The character to be found
     * @param pStartIndex the index where searching should start
     * @param pQuote If `true` take quotes into account
     * @return -1 if not found, else the index of the character
     * @throws NumberFormatException When quotes in the string are unmatched
     */
    fun indexOf(pChar: Char, pStartIndex: Int, pQuote: Boolean): Int {
        var quote = false

        for (i in pStartIndex until length) {
            if (pQuote && get(i) == '"') {
                if (quote) {
                    if (i == 0 || get(i - 1) != '\\') {
                        quote = false
                    }
                } else {
                    quote = true
                }
            } else if (!quote && get(i) == pChar) {
                return i
            }
        }

        if (quote) {
            throw NumberFormatException("Closing quote missing in a quoted indexOf")
        }

        return -1
    }

    /**
     * Get the index of a character in the buffer. Not that the startindex should
     * not be within a quoted area if quotes are taken into account.
     *
     * @param char The character to be found
     * @param pStartIndex the index where searching should start
     * @param pQuotes The quote characters, Alternating the starting quote and the
     * closing quote. The opening quotes at the even indices, the closing
     * at the odd.
     * @return -1 if not found, else the index of the character
     * @throws NumberFormatException When quotes are unmatched
     */
    fun indexOf(char: Char, pStartIndex: Int, pQuotes: CharSequence?): Int {
        var quote = false

        if (pQuotes == null) {
            var i = pStartIndex
            while (i < length) {
                val c = get(i)

                when {
                    quote -> when (c) {
                        '\\' -> i++ /* skip next char */
                        '"' -> quote = false
                    }

                    c == char -> return i

                    c == '"' -> quote = true
                }
                i++
            }
        } else {
            val openQuotes = CharArray(pQuotes.length / 2)
            val closeQuotes = CharArray(openQuotes.size)

            for (i in openQuotes.indices) {
                openQuotes[i] = pQuotes[i * 2]
                closeQuotes[i] = pQuotes[i * 2 + 1]
            }
            val stack = IntStack()

            var i = pStartIndex
            while (i < length) {
                val c = get(i)

                if (!stack.isEmpty) {
                    if (c == '\\') {
                        i++ /* skip next char */
                    } else if (c == closeQuotes[stack.peek()]) {
                        stack.pop()
                    } else {
                        for (j in openQuotes.indices) {
                            if (c == openQuotes[j]) {
                                stack.push(j)

                                break
                            }
                        }
                    }
                } else if (c == char) {
                    return i
                } else {
                    for (j in openQuotes.indices) {
                        if (c == openQuotes[j]) {
                            stack.push(j)

                            break
                        }
                    }
                }
                i++
            }

            if (!stack.isEmpty) {
                quote = true
            }
        }

        if (quote) {
            throw NumberFormatException("Closing quote missing in a quoted indexOf")
        }

        return -1
    }

    /**
     * Insert a string into the rep.
     *
     * @param pIndex the index
     * @param pIn the stringrep to be inserted
     * @return the resulting rep
     */
    fun insertCombine(pIndex: Int, pIn: CharSequence): StringRep {
        val b = toStringBuilder()
        b.insert(pIndex, pIn)

        return createRep(b)
    }

    /**
     * Create a quoted representation of this rep.
     *
     * @return the new quoted rep
     */
    fun quote(): StringRep {
        return quote(this)
    }

    /**
     * Create an iterator for splitting a stringrep into substrings separated by
     * the token.
     *
     * @param pToken the token
     * @param pQuotes if `true`, double quotes are recognised
     * @return the iterator
     */
    fun splitOn(pToken: Char, pQuotes: Boolean): StringRepIterator {
        return StringRepIteratorImpl(this, pToken, pQuotes)
    }

    /**
     * Create an iterator for splitting a stringrep into substrings separated by
     * the token.
     *
     * @param token the token
     * @param quotes if `true`, double quotes are recognised
     * @return the iterator
     */
    fun splitOn(token: Char, quotes: CharSequence): StringRepIterator {
        return StringRepIteratorImpl(this, token, quotes)
    }

    /**
     * Create an iterator for splitting a stringrep into substrings separated by
     * the token, however taking quotes into account.
     *
     * @param pToken the token
     * @return the iterator
     */
    fun splitOn(pToken: Char): StringRepIterator {
        return StringRepIteratorImpl(this, pToken, _DEFAULTQUOTES)
    }

    /**
     * Check whether the rep starts with the the character.
     *
     * @param pChar the character
     * @return `true` if it is the first char
     */
    fun startsWith(pChar: Char): Boolean {
        return if (length == 0) {
            false
        } else get(0) == pChar
    }

    /**
     * a chararray representation.
     *
     * @return a chararray
     */
    abstract fun toCharArray(): CharArray

    /**
     * The stringbuffer representation.
     *
     * @return the stringbuffer
     */
    abstract fun toStringBuffer(): StringBuffer

    /**
     * The stringbuffer representation.
     *
     * @return the stringbuffer
     */
    abstract fun toStringBuilder(): StringBuilder

    /**
     * Create a substring representation.
     *
     * @param pStart The starting character
     * @param pEnd The end character (exclusive)
     * @return A new rep (note that this can be the same one, but does not need to
     * be)
     */
    open fun substring(pStart: Int, pEnd: Int): StringRep {
        if (pStart < 0 || pEnd > length) {
            throw IndexOutOfBoundsException()
        }

        return RepStringRep(pStart, pEnd, this)
    }

    /**
     * Create a substring representation.
     *
     * @param pStart The starting character
     * @return A new rep (note that this can be the same one, but does not need to
     * be)
     */
    fun substring(pStart: Int): StringRep {
        return substring(pStart, length)
    }

    /**
     * Get a subsequence. This is actually equal to the substring method.
     *
     */
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return substring(startIndex, endIndex)
    }

    /**
     * Does the string start with the specified substring.
     *
     * @param pString The substring
     * @return `true` if it starts with it
     */
    fun endsWith(pString: CharSequence): Boolean {
        if (pString.length > length) {
            return false
        }

        val length = pString.length
        val start = length - length

        for (i in 0 until length) {
            if (get(start + i) != pString[i]) {
                return false
            }
        }

        return true
    }

    /**
     * Does the string start with the specified substring.
     *
     * @param pString The substring
     * @return `true` if it starts with it
     */
    fun startsWith(pString: CharSequence): Boolean {
        if (pString.length > length) {
            return false
        }

        val length = pString.length

        for (i in 0 until length) {
            if (get(i) != pString[i]) {
                return false
            }
        }
        return true
    }

    /**
     * Create a rep without starting and ending whitespace.
     *
     * @return a new rep
     */
    fun trim(): StringRep {
        var left = 0
        var right = length - 1
        var c = get(left)

        while (left <= right && (c == ' ' || c == '\n' || c == '\t')) {
            left++
            c = get(left)
        }

        if (left == length) {
            return createRep("")
        }

        c = get(right)
        while (left <= right && (c == ' ' || c == '\n' || c == '\t')) {
            right--
            c = get(right)
        }

        return createRep(left, right + 1, this)
    }

    /**
     * Unquote the string. If it does not start and end with quotes an exception
     * will be thrown.
     *
     * @return An unquoted string
     * @throws NumberFormatException When the quotes are unmatched
     */
    fun unQuote(): StringRep {
        val result = StringBuilder(length)

        if (!(startsWith('"') && endsWith('"'))) {
            throw NumberFormatException("The element to be unquoted does not start or end with quotes")
        }

        var index = 1

        while (index < length - 1) {
            if (get(index) == '\\') {
                if (index == length - 1) {
                    throw NumberFormatException("last quote is escaped")
                }

                index++
            } else if (get(index) == '"') {
                throw NumberFormatException("Internal quotes are not allowed unless escaped")
            }

            result.append(get(index))
            index++
        }

        return createRep(result)
    }

    companion object {

        /** The default quotes that are used.  */
        val _DEFAULTQUOTES = "\"\"\'\'()[]{}"

        /**
         * Create a new StringRep.
         *
         * @param element The string to be encapsulated
         * @return a new StringRep
         */
        fun createRep(element: String): StringRep {
            return StringStringRep(element)
        }

        /**
         * Create a new StringRep.
         *
         * @param element The string to be encapsulated
         * @return a new StringRep
         */
        fun createRep(element: CharSequence): StringRep {
            return CharSequenceStringRep(element)
        }

        /**
         * Create a new StringRep.
         *
         * @param element The string to be encapsulated
         * @return a new StringRep
         */
        fun createRep(element: CharArray): StringRep {
            return CharArrayStringRep(element)
        }

        /**
         * Create a new StringRep.
         *
         * @param pStart The starting index
         * @param pEnd The end index
         * @param element The string to be encapsulated
         * @return a new StringRep
         */
        fun createRep(pStart: Int, pEnd: Int, element: CharArray): StringRep {
            return IndexedCharArrayStringRep(pStart, pEnd, element)
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
        fun createRep(start: Int, end: Int, element: StringRep): StringRep {
            return element.substring(start, end)
        }
    }
}
