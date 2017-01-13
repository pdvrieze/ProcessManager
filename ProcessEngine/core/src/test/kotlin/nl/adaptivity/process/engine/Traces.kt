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

package nl.adaptivity.process.engine

import nl.adaptivity.process.util.Identified

/**
 * Functionality related to specifying traces.
 */

typealias Trace = Array<out TraceElement>

const val ANYINSTANCE = -1
const val SINGLEINSTANCE = -2
const val LASTINSTANCE = -3

private fun String.toTraceNo(): Int {
  return when (this) {
    "*" -> ANYINSTANCE
    "#" -> LASTINSTANCE
    else -> this.toInt()
  }
}

class TraceElement(val nodeId: String, val instanceNo:Int, val outputs:List<ProcessData> = emptyList()): Identified {
  private constructor(stringdescrs: Iterator<String>) : this(stringdescrs.next(), if (stringdescrs.hasNext()) stringdescrs.next().toInt() else SINGLEINSTANCE)
  constructor(stringdescr: String): this(stringdescr.splitToSequence(':').iterator())

  override val id: String get() = nodeId

  override fun toString(): String {
    return when (instanceNo) {
      SINGLEINSTANCE -> nodeId
      ANYINSTANCE    -> "$nodeId:*"
      LASTINSTANCE   -> "$nodeId:#"
      else           -> "$nodeId:$instanceNo"
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as TraceElement

    if (nodeId != other.nodeId) return false
    when {
      instanceNo == other.instanceNo -> Unit
      instanceNo == ANYINSTANCE -> Unit
      instanceNo == SINGLEINSTANCE && (other.instanceNo!=1) -> return false
      instanceNo == LASTINSTANCE || other.instanceNo == LASTINSTANCE -> TODO ("Last instance is not yet supported")
      other.instanceNo == ANYINSTANCE -> Unit
      instanceNo == 1 && other.instanceNo == SINGLEINSTANCE -> Unit
      else -> return false
    }
    if (outputs != other.outputs) return false

    return true
  }

  override fun hashCode(): Int {
    return nodeId.hashCode()
  }


}

class BTrace(internal val elems:Array<TraceElement>): Iterable<TraceElement> {
  operator fun get(idx:Int) = elems[idx]
  val size get() = elems.size
  operator fun plus(other: TraceElement) = BTrace(elems + other)
  operator fun rangeTo(other: TraceElement) = BTrace(elems + other)
  operator fun plus(other: BTrace) = BTrace(elems + other.elems)
  override fun iterator() = elems.iterator()
}

//private typealias BTrace = MutableList<TraceElement>
private typealias Traces = List<BTrace>

private operator fun <T> T.plus(array:Array<T>): Array<T> {
  val index = array.size
  @Suppress("UNCHECKED_CAST")
  val result = java.lang.reflect.Array.newInstance(array::class.java.getComponentType(), array.size + 1) as Array<T>;
  result[0] = this
  System.arraycopy(array, 0, result, 1, array.size)
  return result
}

class TraceBuilder {
  @PublishedApi
  internal inline fun String.toTraceElem() = TraceElement(this)
  @PublishedApi
  internal inline fun Identified.toTraceElem() = TraceElement(this.id, SINGLEINSTANCE)

  inline operator fun String.unaryPlus() = this.toTraceElem().unaryPlus()
  inline operator fun Identified.unaryPlus() = this.toTraceElem().unaryPlus()
  inline operator fun TraceElement.unaryPlus() = listOf(trace(this))

  inline operator fun String.plus(other: String)                   = this.toTraceElem() + other.toTraceElem()
  inline operator fun String.plus(other: Identified)               = this.toTraceElem() + other.toTraceElem()
  inline operator fun String.plus(other: TraceElement)             = this.toTraceElem() + other
  inline operator fun String.plus(other: BTrace)                   = this.toTraceElem() + other
  inline operator fun String.plus(other: Traces)                   = this.toTraceElem() + other
  inline operator fun Identified.plus(other: String)               = this.toTraceElem() + other.toTraceElem()
  inline operator fun Identified.plus(other: Identified)           = this.toTraceElem() + other.toTraceElem()
  inline operator fun Identified.plus(other: TraceElement)         = this.toTraceElem() + other
  inline operator fun Identified.plus(other: BTrace)               = this.toTraceElem() + other
  inline operator fun Identified.plus(other: Traces)               = this.toTraceElem() + other
  inline operator fun TraceElement.plus(other: String)             = this + other.toTraceElem()
  inline operator fun TraceElement.plus(other: Identified)         = this + other.toTraceElem()
  inline operator fun TraceElement.plus(other: TraceElement)       = listOf(trace(this, other))
         operator fun TraceElement.plus(other: BTrace)             = listOf(BTrace(this + other.elems))
         operator fun TraceElement.plus(other: Traces)             = other.map { BTrace(this + it.elems) }

  inline operator fun Traces.plus(other: String):Traces       = this + other.toTraceElem()
  inline operator fun Traces.plus(other: Identified):Traces   = this + other.toTraceElem()
  operator        fun Traces.plus(other: TraceElement):Traces = map { trace -> trace + other }
  operator        fun Traces.plus(other: BTrace):Traces       = map { left -> left + other }
  operator        fun Traces.plus(other: Traces):Traces       = flatMap { left -> other.map { right -> left + right} }


  inline infix fun String.or(other: String)             = this.toTraceElem().or(other.toTraceElem())
  inline infix fun String.or(other: Identified)         = this.toTraceElem().or(other.toTraceElem())
  inline infix fun String.or(other: TraceElement)       = this.toTraceElem().or(other)
  inline infix fun Identified.or(other: String)         = this.toTraceElem().or(other.toTraceElem())
  inline infix fun Identified.or(other: Identified)     = this.toTraceElem().or(other.toTraceElem())
  inline infix fun Identified.or(other: TraceElement)   = this.toTraceElem().or(other)
  inline infix fun TraceElement.or(other: String)       = this.or(other.toTraceElem())
  inline infix fun TraceElement.or(other: Identified)   = this.or(other.toTraceElem())
  inline infix fun TraceElement.or(other: TraceElement) = listOf(trace(this), trace(other))


  inline infix fun Traces.or(other: String): Traces       = this.or(other.toTraceElem())
  inline infix fun Traces.or(other: Identified): Traces   = this.or(other.toTraceElem())
  inline infix fun Traces.or(other: TraceElement): Traces = this.or(listOf(trace(other)))
  inline infix fun Traces.or(other: BTrace): Traces       = this.or(listOf(other))
         infix fun Traces.or(other: Traces): Traces       = ArrayList<BTrace>(this.size+other.size).apply { addAll(this@or); addAll(other) }

  inline operator fun String.times(other: Traces): Traces = this.toTraceElem().times(other)
  inline operator fun Identified.times(other: Traces): Traces = this.toTraceElem().times(other)
         operator fun TraceElement.times(other: Traces): Traces = other.map { right:BTrace -> BTrace(this + right.elems) }

  operator fun Traces.times(other: Traces):Traces = flatMap { left -> other.map { left + it } }








  inline operator fun String.rem(other: String)             = this.toTraceElem().rem(other.toTraceElem())
  inline operator fun String.rem(other: Identified)         = this.toTraceElem().rem(other.toTraceElem())
  inline operator fun String.rem(other: TraceElement)       = this.toTraceElem().rem(other)
  inline operator fun String.rem(other: BTrace)             = this.toTraceElem().rem(other)
  inline operator fun String.rem(other: Traces)             = this.toTraceElem().rem(other)
  inline operator fun Identified.rem(other: String)         = this.toTraceElem().rem(other.toTraceElem())
  inline operator fun Identified.rem(other: Identified)     = this.toTraceElem().rem(other.toTraceElem())
  inline operator fun Identified.rem(other: TraceElement)   = this.toTraceElem().rem(other)
  inline operator fun Identified.rem(other: BTrace)         = this.toTraceElem().rem(other)
  inline operator fun Identified.rem(other: Traces)         = this.toTraceElem().rem(other)
  inline operator fun TraceElement.rem(other: String)       = this.rem(other.toTraceElem())
  inline operator fun TraceElement.rem(other: Identified)   = this.rem(other.toTraceElem())
         operator fun TraceElement.rem(other: TraceElement) = listOf(trace(this, other), trace(other, this))
         operator fun TraceElement.rem(other: BTrace)       = listOf((trace(this) + other), (other + trace(this)))
  inline operator fun TraceElement.rem(other: Traces)       = other.flatMap { listOf(this + it, it + this) }


  inline operator fun Traces.rem(other: String): Traces       = this.rem(trace(other.toTraceElem()))
  inline operator fun Traces.rem(other: Identified): Traces   = this.rem(trace(other.toTraceElem()))
  inline operator fun Traces.rem(other: TraceElement): Traces = this.rem(trace(other))
  inline operator fun Traces.rem(other: BTrace): Traces       = flatMap { left -> listOf(left + other, other + left) }
         operator fun Traces.rem(other: Traces): Traces       = flatMap { left -> other.flatMap { right -> listOf(left + right, right + left) } }






  fun trace(vararg elements: TraceElement): BTrace {
    return BTrace(arrayOf(*elements))
  }


}

fun trace(builder: TraceBuilder.()->List<BTrace>): List<Trace> {
  return TraceBuilder().run(builder).map { it.elems }
}

fun trace(vararg trace:String):Trace {
  return trace.map { TraceElement(it) }.toTypedArray()
}

fun trace(vararg elements: Identified): Trace {
  return elements.map { TraceElement(it.id, SINGLEINSTANCE) }.toTypedArray()
}