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

private typealias BTrace = MutableList<TraceElement>
private typealias Traces = List<List<TraceElement>>

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
  inline operator fun Identified.plus(other: String)               = this.toTraceElem() + other.toTraceElem()
  inline operator fun Identified.plus(other: Identified)           = this.toTraceElem() + other.toTraceElem()
  inline operator fun Identified.plus(other: TraceElement)         = this.toTraceElem() + other
  inline operator fun TraceElement.plus(other: String)             = this + other.toTraceElem()
  inline operator fun TraceElement.plus(other: Identified)         = this + other.toTraceElem()
  inline operator fun TraceElement.plus(other: TraceElement)       = listOf(trace(this, other))
  inline operator fun TraceElement.plus(other: List<TraceElement>) = listOf(listOf(this) + other)

  inline operator fun Traces.plus(other: String):Traces       = this + other.toTraceElem()
  inline operator fun Traces.plus(other: Identified):Traces   = this + other.toTraceElem()
  operator        fun Traces.plus(other: TraceElement):Traces = map { trace -> (trace as BTrace) + other }
  operator        fun Traces.plus(other: Trace):Traces        = map { trace -> (trace as BTrace) + other }
  operator        fun Traces.plus(other: BTrace):Traces       = map { trace -> (trace as BTrace) + other }


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
         infix fun Traces.or(other: TraceElement): Traces = this + listOf(trace(other))
  @JvmName("or2")
  inline infix fun Traces.or(other: BTrace): Traces       = this + other
         infix fun Traces.or(other: Traces): Traces       = this + other

  inline operator fun String.times(other: Traces): Traces = this.toTraceElem().times(other)
  inline operator fun Identified.times(other: Traces): Traces = this.toTraceElem().times(other)
  inline operator fun TraceElement.times(other: Traces): Traces = other.map { it -> (sequenceOf(this) + it.asSequence()).toList() }

  operator fun Traces.times(other: Traces):Traces = flatMap { left -> other.map { left + it } }

  fun trace(vararg elements: TraceElement): List<TraceElement> {
    return elements.toList()
  }


}

fun trace(builder: TraceBuilder.()->List<List<TraceElement>>): List<Trace> {
  return TraceBuilder().run(builder).map { it.toTypedArray() }
}

fun trace(vararg trace:String):Trace {
  return trace.map { TraceElement(it) }.toTypedArray()
}

fun trace(vararg elements: Identified): Trace {
  return elements.map { TraceElement(it.id, SINGLEINSTANCE) }.toTypedArray()
}