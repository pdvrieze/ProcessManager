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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.ws.doclet

import java.io.Writer

/**
 * Created by pdvrieze on 11/04/16.
 */

interface Table {
  var targetWidth: Int
  fun row(block: Row.()->Unit)
}

inline fun Row.col(c: CharSequence) {
  col {text(c)}
}

interface Row {
  /** Create a simple column */
  fun col(block: Cell.()->Unit)
  /** Create a paragraph column that does word wrapping*/
  fun parcol(block: Cell.()->Unit)
}

interface Cell : FlowContent {
  val width: Int

}

interface FlowContent {
  fun text(c:CharSequence)
  fun link(target:CharSequence, label:CharSequence?=null)
}

interface OutputGenerator: FlowContent {
  fun heading1(s:CharSequence)
  fun heading2(s:CharSequence)
  fun heading3(s:CharSequence)
  fun appendln(s:CharSequence)
  fun appendln()
  fun table(vararg columns:String, block: Table.()->Unit)
}

fun Writer.markDown(block: OutputGenerator.()->Unit) = this.use { MdGen(it).block() }

private class MdTable(val columns: Array<out String>) : Table {
  fun appendTo(out: Appendable) {
    val foldable = BooleanArray(columns.size)
    val desiredColWidths = rows.fold(columns.map { it.length }.toTypedArray()) { array, row ->
      row.cells.forEachIndexed { i, cell ->
        if (cell.wrapped) {
          foldable[i]=true
        } else {
          array[i] = Math.max(array[i], cell.width)
        }
      }
      array
    }
    val foldableCount = foldable.count()
    if (foldableCount>0) {
      val totalExtra = targetWidth - (columns.size * 3 + 1) - desiredColWidths.sum()
      if (totalExtra>0) { // Slightly complicated to handle rounding errors
        var seenColumns = 0
        var usedSpace = 0
        foldable.forEachIndexed { i, foldable ->
          if (foldable) {
            ++seenColumns
            val extraSpace = (totalExtra * seenColumns / foldableCount) - usedSpace
            desiredColWidths[i]+=extraSpace
            usedSpace+=extraSpace
          }
        }
      }
    }

    columns.mapIndexed { i, h -> "$h${' '.pad(desiredColWidths[i]-h.length)}" }.joinTo(out, " | ", "| ", " |")
    out.appendln()
    desiredColWidths.joinTo(out, "-|-", "|-", "-|") { '-'.pad(it) }
    out.appendln()
    rows.forEach { row ->
      if (row.cells.count { it.wrapped }>0) {
        val lines= row.cells.mapIndexed { i, cell -> // Make the cells into lists of lines
          val escapedText = Escaper('|').apply { cell.appendTo(this) }
          if (foldable[i]) {
            escapedText.wordWrap(desiredColWidths[i])
          } else {
            sequenceOf(escapedText)
          }
        }.asSequence().flip()
        lines.forEach { line ->
          line.mapIndexed { i, part -> if (part==null) ' '.pad(desiredColWidths[i]) else "$part${' '.pad(desiredColWidths[i]-part.length)}" }
              .joinTo(out, " | ", "| ", " |")
          out.appendln()
        }
      } else {
        row.cells.mapIndexed { i, cell ->
          val text = Escaper('|').apply{ cell.appendTo(this) }
          "$text${' '.pad(desiredColWidths[i]-text.length)}"
        }.joinTo(out," | ", "| ", " |")
        out.appendln()
      }
    }


  }

  private class Escaper(val escapeChar:Char='\\', vararg val escapedChars:Char): Appendable, CharSequence {
    private val buffer= StringBuilder()

    override val length: Int get() = buffer.length

    override fun get(index: Int): Char = buffer.get(index)

    override fun subSequence(startIndex: Int, endIndex: Int) = buffer.subSequence(startIndex, endIndex)

    override fun append(csq: CharSequence?): Appendable = append(csq, 0, csq?.length?:4)

    override fun append(csq: CharSequence?, start: Int, end: Int): Appendable {
      if (csq==null) return append("null", start, end)
      csq.forEach { append(it) }
      return this
    }

    override fun append(c: Char): Appendable {
      if (c==escapeChar || escapedChars.contains(c)) {
        buffer.append(escapeChar)
      }
      buffer.append(c)
      return this
    }

    override fun toString() = buffer.toString()
    override fun equals(other: Any?) = buffer.equals(other)
    override fun hashCode() = buffer.hashCode()
  }

  private val rows = mutableListOf<MdRow>()

  override var targetWidth:Int = 100

  override fun row(block: Row.() -> Unit) {
    rows.add(MdRow(columns.size).apply { block() })
  }
}


fun CharSequence.wordWrap(maxWidth:Int) : Sequence<CharSequence> {
  val result = mutableListOf<CharSequence>()
  var start=0
  var lastWordEnd=0
  forEachIndexed { i, c ->
    when (c) {
      '\n', '\r', ' ' -> lastWordEnd=i
      ',', '.', '!', '?' -> lastWordEnd=i+1
    }
    if ((start<=lastWordEnd && i-start>=maxWidth) || c=='\n' || c=='\r') { // don't wrap if the word is too long
      result.add(subSequence(start, lastWordEnd))
      start = lastWordEnd
      while(start<length && get(start)==' ') { start++ }// start after any spaces
    }
  }
  if (lastWordEnd<length) {
    val last = subSequence(start, length)
    if (! last.isBlank()) { result.add(last) }
  }
  return result.asSequence()
}


private class FlipIterator<T>(inSeq:Sequence<Sequence<T>>):Iterator<Sequence<T?>> {
  val innerSequences:List<Iterator<T>> = inSeq.map{it.iterator()}.toList()

  override fun hasNext() = innerSequences.any { it.hasNext() }

  override fun next(): Sequence<T?> {
    return innerSequences.asSequence().map { if (it.hasNext()) it.next() else null }
  }

}

fun <T> Sequence<Sequence<T>>.flip():Sequence<Sequence<T?>> = object:Sequence<Sequence<T?>> {
  override fun iterator(): Iterator<Sequence<T?>> = FlipIterator<T>(this@flip)
}

private fun Char.pad(count:Int):CharSequence = object:CharSequence {
  override val length: Int = if(count>0) count else 0

  override fun get(index: Int) = this@pad

  override fun subSequence(startIndex: Int, endIndex: Int) = this@pad.pad(endIndex-startIndex)

  override fun toString(): String = StringBuilder(length).append(this).toString()
}

private class MdRow(val colCount:Int):Row {
  val cells = mutableListOf<MdCell>()

  override fun col(block: Cell.() -> Unit) { cells.add(MdCell().apply { block() }) }

  override fun parcol(block: Cell.() -> Unit) { cells.add(MdCell(true).apply { block() }) }
}

private class MdCell(val wrapped:Boolean=false):Cell {
  private val buffer = StringBuilder()

  override fun text(c: CharSequence) {
    buffer.append(c.normalize())
  }

  override fun link(target: CharSequence, label: CharSequence?) = buffer._link(target, label)

  fun appendTo(a:Appendable) { a.append(buffer) }

  override val width: Int get() = buffer.length
}

private class StripNewLines(val orig:CharSequence):CharSequence {
  override val length:Int get() = orig.length

  override fun get(index: Int) = orig.get(index).let{ when (it) {
    '\n', '\r', '\t' -> ' '
    else -> it
  } }

  override fun subSequence(startIndex: Int, endIndex: Int):CharSequence {
    val subSequence = orig.subSequence(startIndex, endIndex)
    return subSequence.normalize()
  }

  override fun toString(): String {
    return StringBuilder(length).append(this).toString()
  }
}

fun CharSequence?.normalize():CharSequence = StringBuilder(this?.length?:0).apply {
  this@normalize?.forEach { c ->
    when(c) {
      in '\u0000'..'\u001f', ' ', '\u00a0' -> if (length>0 && last()!=' ') append(' ')
      else -> append(c)
    }
  }
}

fun CharSequence.tidy():CharSequence = StringBuilder(this.length).apply {
  var addNewLines:Int=0
  this@tidy.forEach { c ->
    when(c) {
      '\u000a', '\u000d' -> addNewLines ++
      in '\u0000'..'\u0009', '\u000b', '\u000c', in '\u000e'..'\u001f', ' ', '\u00a0' -> if (length>0 && last()!=' ') append(' ')
      else -> {
        if (addNewLines>1){ appendln(); appendln() };
        addNewLines=0;
        append(c)
      }
    }
  }
}

private fun Appendable._link(target:CharSequence, label: CharSequence?) {
  if (label!=null) {
    append("[$label]($target)")
  } else if(target.startsWith('#')) {
    append("[${target.substring(1)}]($target)")
  } else {
    append("<$target>")
  }
}

private class MdGen(val out: Appendable): OutputGenerator {
  override fun heading1(s: CharSequence) { out.appendln("# $s") }
  override fun heading2(s: CharSequence) { out.appendln("## $s") }
  override fun heading3(s: CharSequence) { out.appendln("### $s") }

  override fun text(s: CharSequence) { out.appendln(s.tidy().wordWrap(WORDWRAP)) }

  override fun appendln(s: CharSequence) { out.appendln(s) }

  override fun appendln() { out.appendln() }

  override fun link(target: CharSequence, label: CharSequence?) = out._link(target, label)

  override fun table(vararg columns: String, block: Table.() -> Unit) {
    MdTable(columns).apply { this.block() }.appendTo(out)
  }
}

/**
 * Append each of the charSequences in the parameters as a new line.
 *
 * @param seq The sequence of items to append.
 */
fun Appendable.appendln(seq: Sequence<CharSequence>) {
  seq.forEach { appendln(it) }
}
