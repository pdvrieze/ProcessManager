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
@file:JvmName("StringUtil")

package net.devrieze.util

import net.devrieze.lang.Const
import nl.adaptivity.util.CharArraySequence
import nl.adaptivity.util.multiplatform.toLowercase
import org.jetbrains.annotations.Contract

import java.io.IOException
import java.io.Writer
import java.util.*


/**
 * A utility class for Strings.
 *
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */

@Deprecated("Use {@link Const#_CR}", ReplaceWith("Const._CR", "net.devrieze.lang.Const"))
val _CR = Const._CR


@Deprecated("Use {@link Const#_LF}", ReplaceWith("Const._LF", "net.devrieze.lang.Const"))
val _LF = Const._LF

private val FORMAT_SLACK = 20

private val EXTWHITESPACE = charArrayOf('\u0020', '\u000a', '\u000d', '\u0009', '\u0000', '\u000c')

private val AVG_WORD_SIZE = 8


private class IndentingWriter(level: Int, private val target: Writer) : Writer() {

    private val currentLineBuffer: CharArray = CharArray(level) { ' ' }

    private var lastSeenWasNewline: Boolean = true

    @Throws(IOException::class)
    override fun close() {
        target.close()
    }

    @Throws(IOException::class)
    override fun flush() {
        target.flush()
    }

    @Throws(IOException::class)
    override fun write(cbuf: CharArray, off: Int, len: Int) {
        val end = off + len
        var nextToWrite = off
        var i = off
        while (i < end) {
            val c = cbuf[i]
            if (c == Const._CR || c == Const._LF) {
                val lastChar = i - 1
                val d: Char = if (i + 1 >= end) 0.toChar() else cbuf[i + 1]
                if (c != d && (d == Const._CR || d == Const._LF)) {
                    ++i
                }
                if (lastChar != nextToWrite) {
                    // Skip indent in case newline follows directly
                    if (lastSeenWasNewline) {
                        target.write(currentLineBuffer)
                    }
                }
                target.write(cbuf, nextToWrite, i - nextToWrite + 1)
                nextToWrite = i + 1
                lastSeenWasNewline = true
            }
            ++i
        }
        if (nextToWrite < end) {
            if (lastSeenWasNewline) {
                target.write(currentLineBuffer)
            }
            target.write(cbuf, nextToWrite, end - nextToWrite)
            lastSeenWasNewline = false
        }
    }

}

private class RepeatingChars(private val char: Char, override val length: Int) : CharSequence {

    override operator fun get(index: Int): Char = char


    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return RepeatingChars(char, endIndex - startIndex)
    }

    override fun toString(): String {
        return buildString(length) { append(this) }
    }
}

/**
 * Utility method to determine whether a string is null or empty
 * @param value The value to check.
 * @return `true` if it is empty, `false` if not
 */
@Deprecated("Use kotlin version", ReplaceWith("value.isNullOrEmpty()"))
@Contract(value = "null -> true", pure = true)
fun isNullOrEmpty(value: CharSequence?) = value.isNullOrEmpty()

@Deprecated("No longer needed", ReplaceWith("string.toLowercase()", "nl.adaptivity.util.multiplatform.toLowercase"))
fun toLowerCase(charSequence: CharSequence): String = charSequence.toLowercase()

@Deprecated("Use kotlin version", ReplaceWith("text.indexOf(c)"))
fun indexOf(text: CharSequence, c: Char): Int = text.indexOf(c)

/**
 * Creates a string of the obj. Unlike [Objects.toString] it returns null on a null parameter.
 * @param obj The object to convert
 * @return The result of calling @{link #toString()} on the object.
 */
@Contract(value = "null -> null; !null -> !null", pure = true)
@Deprecated("Use kotlin", ReplaceWith("obj?.toString()"))
fun toString(obj: CharSequence?): String? = obj?.toString()

/**
 * Create a quoted version of the buffer.
 *
 * @receiver The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Deprecated("Use better named quoted", ReplaceWith("quoted()"))
fun CharSequence.quoteBuilder() = quoted()

/**
 * Create a quoted version of the buffer.
 *
 * @receiver The sequence that needs to be quoted
 * @return The result of the quoting
 */
fun CharSequence.quoted(): String = buildString(length + FORMAT_SLACK) {
    appendQuoted(this@quoted)
}

fun Appendable.appendQuoted(charSequence: CharSequence) {
    append('"')

    for (c in charSequence) {
        when (c) {
            '"'  -> append("\\\"")
            '\\' -> append("\\\\")
            '\t' -> append("\\t")
            '\n' -> append("\\n")
            else -> append(c)
        }
    }

    append('"')
}


/**
 * Create a quoted version of the buffer.
 *
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Just use quotation directly")
fun quote(buffer: CharSequence): StringRep {
    return StringRep.createRep(buffer.quoted())
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Deprecated("Just use quotation directly")
fun quote(buffer: CharArray): StringRep {
    @Suppress("DEPRECATION")
    return quote(CharArraySequence(buffer))
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param start The start index in the buffer that needs to be quoted
 * @param end The end index in the buffer to be quoted (exclusive)
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Deprecated("Just use quotation directly")
fun quote(start: Int, end: Int, buffer: CharArray): StringRep {
    @Suppress("DEPRECATION")
    return quote(CharArraySequence(buffer, start, end))
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Deprecated("Just use quotation directly")
fun quoteBuf(buffer: String): StringBuffer {
    @Suppress("DEPRECATION")
    return quoteBuf(buffer.toCharArray())
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Deprecated("Just use quotation directly")
fun quoteBuf(buffer: StringBuffer): StringBuffer {
    val charArray = CharArray(buffer.length)
    buffer.getChars(0, charArray.size, charArray, 0)

    @Suppress("DEPRECATION")
    return quoteBuf(charArray)
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Deprecated("Just bad. Don't use, StringBuffers are deprecated")
fun quoteBuf(buffer: CharArray): StringBuffer {
    @Suppress("DEPRECATION")
    return quoteBuf(0, buffer.size, buffer)
}

/**
 * Create a quoted version of the buffer for use in the script.
 *
 * @param start The start index in the buffer that needs to be quoted
 * @param end The end index in the buffer to be quoted (exclusive)
 * @param buffer The buffer that needs to be quoted
 * @return The result of the quoting
 */
@Deprecated("Just bad. Don't use, StringBuffers are deprecated")
fun quoteBuf(start: Int, end: Int, buffer: CharArray): StringBuffer {
    val result = StringBuffer(buffer.size + FORMAT_SLACK)
    result.append('"')

    for (i in start until end) {
        when (buffer[i]) {
            '"'  -> {
                result.append("\\\"")
            }

            '\\' -> {
                result.append("\\\\")
            }

            else -> result.append(buffer[i])
        }
    }

    result.append('"')

    return result
}

/**
 * @return `true` if eq`false` if not.
 */
@Deprecated("In favour of {@link #isEqual(CharSequence, CharSequence)}.", ReplaceWith("seq1.isEqual(seq2)"))
fun sequencesEqual(seq1: CharSequence?, seq2: CharSequence?): Boolean {
    return seq1.isEqual(seq2)
}

/**
 * Compare the two sequences for equality. This can return `true`
 * for objects of different classes as long as the sequences are equal.
 * Besides allowing arbitrary CharSequences, this also differs from [  ][String.equals] in that either value may be `null`
 * and it will return `true` when both sequences are
 * `null`.
 *
 * @receiver The first sequence.
 * @param sequence2 The second sequence.
 * @return `true` if equal, `false` if not.
 */
fun CharSequence?.isEqual(sequence2: CharSequence?): Boolean = when {
    this === sequence2                -> true
    this == null || sequence2 == null -> false
    length != sequence2.length        -> false
    else                              -> indices.asSequence().all { this[it] == sequence2[it] }
}

@Deprecated("Use kotlin standard library", ReplaceWith("this == seq2"))
fun String?.isEqual(seq2: String?) = this == seq2

fun Class<*>.simpleClassName(): String {
    return name.substringAfterLast('.')
}

fun Char.repeat(count: Int): CharSequence = RepeatingChars(this, count)

@Deprecated("Use the extension version", ReplaceWith("char.repeat(count)"))
fun charRepeat(count: Int, char: Char): CharSequence {
    return RepeatingChars(char, count)
}

fun <T : Appendable> T.appendRepeated(char: Char, count: Int): T = apply {
    for (i in 0 until count) append(char)
}

@Deprecated("Use appendRepeated", ReplaceWith("builder.appendRepeated(char, count)"))
fun addChars(builder: StringBuilder, count: Int, char: Char): StringBuilder =
    builder.appendRepeated(char, count)

/**
 * Indent the given string.
 *
 * @param level The level of indentation that should be added.
 * @param charSequence The string to be indented.
 * @return The indented string
 */
fun indent(level: Int, charSequence: CharSequence) = buildString(charSequence.length + level * 2) {
    appendIndented(charSequence, level)
}


@Deprecated("Use extension version", ReplaceWith("source.indent(level)"))
fun indent(level: Int, source: Writer): Writer = source.indent(level)

fun Writer.indent(level: Int): Writer {
    return IndentingWriter(level, this)
}

@Deprecated("Use extension version", ReplaceWith("target.appendIndented(string, level)"))
fun indentTo(target: StringBuilder, level: Int, string: CharSequence): StringBuilder {
    return target.appendIndented(string, level)
}

fun <T : Appendable> T.appendIndented(charSequence: CharSequence, level: Int): T {
    (this as? StringBuilder)?.run {
        ensureCapacity(length + 2 * level + charSequence.length)
    }

    appendRepeated(' ', level)

    var i = 0
    while (i < charSequence.length) {
        var j = i
        var c = charSequence[j]

        while (j < charSequence.length && c != Const._CR && c != Const._LF) {
            j++
            c = charSequence[j]
        }

        while (j < charSequence.length && (c == Const._CR || c == Const._LF)) {
            j++
            c = charSequence[j]
        }

        append(charSequence.subSequence(i, j))
        i = j

        if (j < charSequence.length) appendRepeated(' ', level)
        i++
    }
    return this
}

@Deprecated("Use extension version", ReplaceWith("char.isExtWhitespace()"))
fun Char.isWhite(): Boolean {
    return isExtWhitespace()
}

/**
 * Determine whether the character is extended whitespace. This includes some unicode/ascii characters that are not
 * part of the normal xml whitespace set.
 */
fun Char.isExtWhitespace(): Boolean {
    return this in EXTWHITESPACE
}

/**
 * Split the string into parts with the needle as splitter.
 *
 * @param string The string to split.
 * @param needle The character to split on.
 * @return The resulting list of strings.
 */
@Deprecated("Use kotlin split", ReplaceWith("string.split(needle)", "kotlin.text.split"))
fun split(string: String, needle: Char): List<String> {
    val result = ArrayList<String>()
    var i0 = 0
    var i1 = string.indexOf(needle)
    while (i0 < i1) {
        result.add(string.substring(i0, i1))
        i0 = i1 + 1
        i1 = string.indexOf(needle, i0)
    }
    if (i0 == string.length) {
        result.add("")
    } else if (i0 < string.length) {
        result.add(string.substring(i0))
    }
    return result
}

/**
 * Check whether the string contains the word. A word is anything that is
 * surrounded by either the end or start of the string or characters for which
 * [.isLetter] is false.
 *
 * @param haystack The string to search in.
 * @param needle The word to search for.
 * @return `true` if the word is contained.
 */
@Deprecated("Use Kotlin stdlib", ReplaceWith("haystack.contains(needle)"))
fun containsWord(haystack: String, needle: String): Boolean = haystack.contains(needle)

/**
 * Check whether the character is a letter.
 *
 * @param char The character to check
 * @return true if it is, false if not.
 */
@Deprecated("Use standard functions", ReplaceWith("char.isLetter()"))
fun isLetter(char: Char): Boolean {
    return char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char == '-'
}

/**
 * Split the string into lines.
 *
 * @param str The string to split.
 * @return The lines in the string.
 */
@Deprecated("Use kotlin standard library", ReplaceWith("str.lines().toTypedArray()"))
fun splitLines(str: String): Array<String> {
    return str.lines().toTypedArray()
}


/**
 * Prefix the given string with `a` or `an` depending on
 * the first character.
 *
 * @param string The string to prefix.
 * @return The string resulting of prefixing the code.
 */
fun prefixA(string: String) = when (Character.toLowerCase(string[0])) {
    'a', 'e', 'i', 'o', 'u' -> "an $string"
    else                    -> "a $string"
}

/**
 * Get the index of the character in the string, ignoring quotes.
 *
 * @receiver The string to search in.
 * @param needle The character to search.
 * @param startIndex The position to start searching at.
 * @return The result. Or `-1` when not found.
 */
@JvmOverloads
fun CharSequence.quoteIndexOf(needle: Char, startIndex: Int = 0): Int {
    var i = startIndex
    while (i < length) {
        val c = get(i)

        when (c) {
            needle -> return i

            '\\'   -> i++ // skip next character

            '\''   -> {
                var d = this[i + 1]
                while (d != c) {
                    i++
                    d = this[i + 1]
                }
            }

            '\"'   -> {
                i++
                var d = this[i]
                while (d != c) {
                    i++
                    d = this[i]
                    if (d == '\\') {
                        i += 2
                        d = this[i]
                    }
                }
            }
        }
        i++
    }
    return -1
}

/**
 * Join the words with the given separator.
 *
 * @param separator The separator
 * @param words The words.
 * @return String The resulting string.
 */
@Deprecated("Use kotlin stdlib", ReplaceWith("words.joinToString(separator)"))
fun join(separator: String, words: Iterable<String>): String {
    return words.joinToString(separator)
}

/**
 * Output as string with at least a certain length. The string is padded with
 * zeros.
 *
 * @receiver The integer to output
 * @param length the length.
 * @return the resulting string
 */
fun Int.toLengthString(length: Int): String {
    val str = toString()
    if (str.length >= length) return str
    return buildString {
        appendRepeated('0', length - str.length)
        append(str)
    }
}

/*
object StringUtil {
    @Deprecated("Don't use", ReplaceWith("s1 == s2"))
    fun isEqual(s1: String?, s2: String?): Boolean {
        return s1 == s2
    }

}*/
