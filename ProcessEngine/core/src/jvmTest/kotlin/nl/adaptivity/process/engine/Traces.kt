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

import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.spek.allChildren
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xmlutil.util.CompactFragment

/**
 * Functionality related to specifying traces.
 */

typealias Trace = Array<out TraceElement>

const val ANYINSTANCE = -1
const val SINGLEINSTANCE = -2
const val LASTINSTANCE = -3

private fun String.toTraceNo(): Int {
    return when (this) {
        "*"  -> ANYINSTANCE
        "#"  -> LASTINSTANCE
        else -> this.toInt()
    }
}

@DslMarker
annotation class TraceDsl

class TraceElement(val nodeId: String, val instanceNo: Int, val outputs: List<ProcessData> = emptyList()) : Identified {
    private constructor(stringdescrs: Iterator<String>) : this(stringdescrs.next(),
                                                               if (stringdescrs.hasNext()) stringdescrs.next().toInt() else SINGLEINSTANCE)

    constructor(stringdescr: String) : this(stringdescr.splitToSequence(':').iterator())

    override val id: String get() = nodeId

    override fun toString(): String {
        return when (instanceNo) {
            SINGLEINSTANCE -> nodeId
            ANYINSTANCE    -> "$nodeId[*]"
            LASTINSTANCE   -> "$nodeId[#]"
            else           -> "$nodeId[$instanceNo]"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as TraceElement

        if (nodeId != other.nodeId) return false
        when {
            instanceNo == other.instanceNo                                 -> Unit
            instanceNo == ANYINSTANCE                                      -> Unit
            instanceNo == SINGLEINSTANCE && (other.instanceNo != 1)        -> return false
            instanceNo == LASTINSTANCE || other.instanceNo == LASTINSTANCE -> TODO("Last instance is not yet supported")
            other.instanceNo == ANYINSTANCE                                -> Unit
            instanceNo == 1 && other.instanceNo == SINGLEINSTANCE          -> Unit
            else                                                           -> return false
        }
        if (outputs != other.outputs) return false

        return true
    }

    override fun hashCode(): Int {
        return nodeId.hashCode()
    }

    /**
     * The data that will be used as the return of the service behind the node.
     */
    val resultPayload: CompactFragment? get() = null

    fun getNodeInstance(transaction: StubProcessTransaction, instance: ProcessInstance): ProcessNodeInstance<*>? {
        return when (instanceNo) {
            ANYINSTANCE    -> instance.allChildren(transaction).firstOrNull { it.node.id == nodeId }
            LASTINSTANCE   -> instance.allChildren(transaction).filter { it.node.id == nodeId }.maxBy { it.entryNo }
            SINGLEINSTANCE -> instance.allChildren(transaction).filter { it.node.id == nodeId }.also {
                if (it.count() > 1) throw ProcessTestingException(
                    "Only one instance is allowed with this trace: $this found: [${it.joinToString()}]")
            }.singleOrNull()
            else           -> instance.allChildren(
                transaction).firstOrNull { it.node.id == nodeId && it.entryNo == instanceNo }
        }
    }


}

class BTrace(val elems: Array<TraceElement>) : Iterable<TraceElement> {
    operator fun get(idx: Int) = elems[idx]
    val size get() = elems.size
    operator fun plus(other: TraceElement) = BTrace(elems + other)
    operator fun rangeTo(other: TraceElement) = BTrace(elems + other)
    operator fun plus(other: BTrace) = BTrace(elems + other.elems)
    override fun iterator() = elems.iterator()

    fun slice(indices: IntRange) = BTrace(elems.sliceArray(indices))
    fun slice(startIdx: Int) = BTrace(elems.sliceArray(startIdx until elems.size))
}

//private typealias BTrace = MutableList<TraceElement>
private typealias Traces = List<BTrace>

private operator fun <T> T.plus(array: Array<T>): Array<T> {
    val index = array.size
    @Suppress("UNCHECKED_CAST")
    val result = java.lang.reflect.Array.newInstance(array::class.java.getComponentType(), array.size + 1) as Array<T>;
    result[0] = this
    System.arraycopy(array, 0, result, 1, array.size)
    return result
}

@Suppress("NOTHING_TO_INLINE")
@TraceDsl
class TraceBuilder {
    internal inline operator fun Identified.get(instanceNo: Int) = TraceElement(id, instanceNo)

    /** Create trace element */
    @TraceDsl
    @PublishedApi
    internal inline fun String.toTraceElem() = TraceElement(this)

    /** Create trace element */
    @TraceDsl
    @PublishedApi
    internal inline fun Identified.toTraceElem() = TraceElement(this.id, SINGLEINSTANCE)

    /** Create single element trace */
    @TraceDsl
    inline operator fun String.unaryPlus() = this.toTraceElem().unaryPlus()
    /** Create single element trace */
    @TraceDsl
    inline operator fun Identified.unaryPlus() = this.toTraceElem().unaryPlus()
    /** Create single element trace */
    @TraceDsl
    inline operator fun TraceElement.unaryPlus() = listOf(trace(this))

    /** sequence */
    @TraceDsl
    inline operator fun String.rangeTo(other: String): Traces = this.toTraceElem()..other.toTraceElem()
    /** sequence */
    @TraceDsl
    inline operator fun String.rangeTo(other: Identified): Traces = this.toTraceElem()..other.toTraceElem()
    /** sequence */
    @TraceDsl
    inline operator fun String.rangeTo(other: TraceElement): Traces = this.toTraceElem()..other
    /** sequence */
    @TraceDsl
    inline operator fun String.rangeTo(other: BTrace): Traces = this.toTraceElem()..other
    /** sequence */
    @TraceDsl
    inline operator fun String.rangeTo(other: Traces): Traces = this.toTraceElem()..other
    /** sequence */
    @TraceDsl
    inline operator fun Identified.rangeTo(other: String): Traces = this.toTraceElem()..other.toTraceElem()
    /** sequence */
    @TraceDsl
    inline operator fun Identified.rangeTo(other: Identified): Traces = this.toTraceElem()..other.toTraceElem()
    /** sequence */
    @TraceDsl
    inline operator fun Identified.rangeTo(other: TraceElement): Traces = this.toTraceElem()..other
    /** sequence */
    @TraceDsl
    inline operator fun Identified.rangeTo(other: BTrace): Traces = this.toTraceElem()..other
    /** sequence */
    @TraceDsl
    inline operator fun Identified.rangeTo(other: Traces): Traces = this.toTraceElem()..other
    /** sequence */
    @TraceDsl
    inline operator fun TraceElement.rangeTo(other: String): Traces = this..other.toTraceElem()
    /** sequence */
    @TraceDsl
    inline operator fun TraceElement.rangeTo(other: Identified): Traces = this..other.toTraceElem()
    /** sequence */
    @TraceDsl
    inline operator fun TraceElement.rangeTo(other: TraceElement): Traces = listOf(trace(this, other))
    /** sequence */
    @TraceDsl
    operator fun TraceElement.rangeTo(other: BTrace): Traces = listOf(BTrace(this + other.elems))
    /** sequence */
    @TraceDsl
    operator fun TraceElement.rangeTo(other: Traces): Traces = other.map { BTrace(this + it.elems) }

    /** sequence */
    @TraceDsl
    inline operator fun Traces.rangeTo(other: String): Traces = this..other.toTraceElem()
    /** sequence */
    @TraceDsl
    inline operator fun Traces.rangeTo(other: Identified): Traces = this..other.toTraceElem()
    /** sequence */
    @TraceDsl
    operator fun Traces.rangeTo(other: TraceElement): Traces = map { trace -> trace + other }
    /** sequence */
    @TraceDsl
    operator fun Traces.rangeTo(other: BTrace): Traces = map { left -> left + other }
    /** sequence */
    @TraceDsl
    @Deprecated("Use times", ReplaceWith("this * other"))
    operator fun Traces.rangeTo(other: Traces): Traces = flatMap { left -> other.map { right -> left + right } }

    /** choice */
    @TraceDsl
    inline infix fun String.or(other: String): Traces = this.toTraceElem().or(other.toTraceElem())
    /** choice */
    @TraceDsl
    inline infix fun String.or(other: Identified): Traces = this.toTraceElem().or(other.toTraceElem())
    /** choice */
    @TraceDsl
    inline infix fun String.or(other: TraceElement): Traces = this.toTraceElem().or(other)
    /** choice */
    @TraceDsl
    inline infix fun Identified.or(other: String): Traces = this.toTraceElem().or(other.toTraceElem())
    /** choice */
    @TraceDsl
    inline infix fun Identified.or(other: Identified): Traces = this.toTraceElem().or(other.toTraceElem())
    /** choice */
    @TraceDsl
    inline infix fun Identified.or(other: TraceElement): Traces = this.toTraceElem().or(other)
    /** choice */
    @TraceDsl
    inline infix fun Identified.or(other: BTrace): Traces = this.toTraceElem().or(other)
    /** choice */
    @TraceDsl
    inline infix fun Identified.or(other: Traces): Traces = this.toTraceElem().or(other)
    /** choice */
    @TraceDsl
    inline infix fun TraceElement.or(other: String): Traces = this.or(other.toTraceElem())
    /** choice */
    @TraceDsl
    inline infix fun TraceElement.or(other: Identified): Traces = this.or(other.toTraceElem())
    /** choice */
    @TraceDsl
    inline infix fun TraceElement.or(other: TraceElement): Traces = listOf(trace(this), trace(other))
    /** choice */
    @TraceDsl
    inline infix fun TraceElement.or(other: BTrace): Traces = listOf(trace(this)).or(other)
    /** choice */
    @TraceDsl
    inline infix fun TraceElement.or(other: Traces): Traces = listOf(trace(this)).or(other)


    /** choice */
    @TraceDsl
    inline infix fun Traces.or(other: String): Traces = this.or(other.toTraceElem())
    /** choice */
    @TraceDsl
    inline infix fun Traces.or(other: Identified): Traces = this.or(other.toTraceElem())
    /** choice */
    @TraceDsl
    inline infix fun Traces.or(other: TraceElement): Traces = this.or(listOf(trace(other)))
    /** choice */
    @TraceDsl
    inline infix fun Traces.or(other: BTrace): Traces = this.or(listOf(other))
    /** choice */
    @TraceDsl
    infix fun Traces.or(other: Traces): Traces = ArrayList<BTrace>(this.size + other.size).apply {
        addAll(this@or); addAll(other)
    }

    /** combination sequence (all options left with all options right) */
    @TraceDsl
    inline operator fun String.times(other: Traces): Traces = this.toTraceElem().times(other)
    /** combination sequence (all options left with all options right) */
    @TraceDsl
    inline operator fun Identified.times(other: Traces): Traces = this.toTraceElem().times(other)
    /** combination sequence (all options left with all options right) */
    @TraceDsl
    operator fun TraceElement.times(other: Traces): Traces = other.map { right: BTrace -> BTrace(this + right.elems) }

    /** combination sequence (all options left with all options right) */
    @TraceDsl
    operator fun Traces.times(other: Traces): Traces = flatMap { left -> other.map { right -> left + right } }

    /** optional trace element (creates two traces)*/
    @TraceDsl
    inline val String.opt: Traces get() = this.toTraceElem().opt
    /** optional trace element (creates two traces)*/
    @TraceDsl
    inline val Identified.opt: Traces get() = this.toTraceElem().opt
    /** optional trace element (creates two traces)*/
    @TraceDsl
    inline val TraceElement.opt: Traces get() = trace(this).opt
    /** optional trace element (creates two traces)*/
    @TraceDsl
    val BTrace.opt: Traces get() = listOf(trace(), this)
    /** optional trace element (creates two traces)*/
    @TraceDsl
    val Traces.opt: Traces get() = listOf(trace()) or this

    /** flatten parallalel sequences, generating all valid zips between the two ordered lists */
    @TraceDsl
    operator fun BTrace.div(other: BTrace): Traces {
        return when {
            size == 0       -> listOf(other)
            other.size == 0 -> listOf(this)
            else            -> (0..size).flatMap { split ->
                listOf(slice(0 until split)..other[0]) * (slice(split) / other.slice(1))
            }
        }
    }

    /** flatten parallalel sequences */
    @TraceDsl
    operator fun Traces.div(other: BTrace): Traces = if (other.size == 0) this else flatMap { it.div(other) }
    /** flatten parallalel sequences */
    @TraceDsl
    operator fun Traces.div(other: Traces): Traces = other.flatMap { div(it) }

    /* unordered */
    @TraceDsl
    inline operator fun String.rem(other: String): Traces = this.toTraceElem().rem(other.toTraceElem())
    /* unordered */
    @TraceDsl
    inline operator fun String.rem(other: Identified): Traces = this.toTraceElem().rem(other.toTraceElem())
    /* unordered */
    @TraceDsl
    inline operator fun String.rem(other: TraceElement): Traces = this.toTraceElem().rem(other)
    /* unordered */
    @TraceDsl
    inline operator fun String.rem(other: BTrace): Traces = this.toTraceElem().rem(other)
    /* unordered */
    @TraceDsl
    inline operator fun String.rem(other: Traces): Traces = this.toTraceElem().rem(other)
    /* unordered */
    @TraceDsl
    inline operator fun Identified.rem(other: String): Traces = this.toTraceElem().rem(other.toTraceElem())
    /* unordered */
    @TraceDsl
    inline operator fun Identified.rem(other: Identified): Traces = this.toTraceElem().rem(other.toTraceElem())
    /* unordered */
    @TraceDsl
    inline operator fun Identified.rem(other: TraceElement): Traces = this.toTraceElem().rem(other)
    /* unordered */
    @TraceDsl
    inline operator fun Identified.rem(other: BTrace): Traces = this.toTraceElem().rem(other)
    /* unordered */
    @TraceDsl
    inline operator fun Identified.rem(other: Traces): Traces = this.toTraceElem().rem(other)
    /* unordered */
    @TraceDsl
    inline operator fun TraceElement.rem(other: String): Traces = this.rem(other.toTraceElem())
    /* unordered */
    @TraceDsl
    inline operator fun TraceElement.rem(other: Identified): Traces = this.rem(other.toTraceElem())
    /* unordered */
    @TraceDsl
    operator fun TraceElement.rem(other: TraceElement): Traces = listOf(trace(this, other), trace(other, this))
    /* unordered */
    @TraceDsl
    operator fun TraceElement.rem(other: BTrace): Traces = listOf((trace(this) + other), (other + trace(this)))
    /* unordered */
    @TraceDsl
    operator fun TraceElement.rem(other: Traces): Traces = other.flatMap { (this..it) or (it..this) }


    /* unordered */
    @TraceDsl
    inline operator fun Traces.rem(other: String): Traces = this.rem(trace(other.toTraceElem()))
    /* unordered */
    @TraceDsl
    inline operator fun Traces.rem(other: Identified): Traces = this.rem(trace(other.toTraceElem()))
    /* unordered */
    @TraceDsl
    inline operator fun Traces.rem(other: TraceElement): Traces = this.rem(trace(other))
    /* unordered */
    @TraceDsl
    inline operator fun Traces.rem(other: BTrace): Traces = flatMap { left -> listOf(left + other, other + left) }
    /* unordered */
    @TraceDsl
    operator fun Traces.rem(other: Traces): Traces = flatMap { left ->
        other.flatMap { right ->
            listOf(left + right, right + left)
        }
    }

    /**
     * Create a trace of the given elements
     */
    @TraceDsl
    fun trace(vararg elements: TraceElement): BTrace {
        return BTrace(arrayOf(*elements))
    }


}

/**
 * Build a trace
 */
inline fun trace(builder: TraceBuilder.() -> List<BTrace>): List<Trace> {
    return TraceBuilder().run(builder).map { it.elems }
}

fun List<Trace>.removeInvalid(): List<Trace> {
    fun Trace.isValid(): Boolean {
        return this.isNotEmpty() && run {
            val seen = HashSet<TraceElement>()
            this.asSequence().all { seen.add(it) }
        }
    }

    return filter { it.isValid() }
}

fun trace(vararg trace: String): Trace {
    return trace.map { TraceElement(it) }.toTypedArray()
}

fun trace(vararg elements: Identified): Trace {
    return elements.map { TraceElement(it.id, SINGLEINSTANCE) }.toTypedArray()
}
