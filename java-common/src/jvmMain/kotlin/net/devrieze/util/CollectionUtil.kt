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
 * Created on Dec 1, 2003
 *
 */
@file:JvmName("CollectionUtil")

package net.devrieze.util

import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


/**
 * This class provides functions that the Collections class does not.
 *
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */
val EMPTYSORTEDSET: SortedSet<*> = EmptySortedSet


private open class CombiningIterable<T, U : MutableIterable<T>>(
    internal  var lists: List<U>
) : MutableIterable<T> {

    constructor(
        first: U,
        others: Array<out U>
    ) : this (listOf(first, *others))

    override fun iterator(): MutableIterator<T> {
        return CombiningIterator(this)
    }

}

private class ConcatenatedList<T> :
    CombiningIterable<T, MutableList<T>>, MutableList<T> {

    constructor(first: MutableList<T>, others: Array<out MutableList<T>>) : super(first, others)
    constructor(lists: List<MutableList<T>>) : super(lists)

    private fun first(): MutableList<T> {
        return lists.first()
    }

    private fun others(): List<MutableList<T>> {
        return lists.subList(0, lists.size)
    }

    override val size: Int
        get() = lists.sumOf { it.size }

    override fun isEmpty(): Boolean {
        return lists.all { it.isEmpty() }
    }

    override operator fun contains(element: T): Boolean {
        return lists.any { element in it }
    }

    override fun add(element: T): Boolean {
        return lists.last().add(element) // will always have 1 element
    }

    override fun remove(element: T): Boolean {
        return lists.any { it.remove(element) } // any shortcircuits
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        val stillToFind: MutableSet<T> = elements.toMutableSet()
        return lists.firstOrNull { stillToFind.removeAll(it); stillToFind.isEmpty() } != null
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return lists.last().addAll(elements)
    }

    internal data class IntPair(val listIdx: Int, val indexInList: Int)

    internal fun listNoAtIndex(index: Int): IntPair {
        var idx = index
        for (listIdx in lists.indices) {
            val list = lists[listIdx]
            when {
                idx < list.size -> return IntPair(listIdx, idx)
                else -> idx -= list.size
            }
        }
        throw IndexOutOfBoundsException("The index $index is beyond the bounds of the contained lists (size: $size)")
    }

    internal fun listNoBeforeIndex(index: Int): IntPair {
        if (index==0) return IntPair(0,0)
        return listNoAtIndex(index-1).let { (l, i) -> IntPair(l, i + 1)}
    }

    internal fun listAtIndex(index: Int): Pair<MutableList<T>, Int> {
        var idx = index
        for (list in lists) {
            when {
                idx < list.size -> return Pair(list, idx)
                else -> idx -= list.size
            }
        }
        throw IndexOutOfBoundsException("The index $index is beyond the bounds of the contained lists (size: $size)")
    }

    internal fun listBeforeIndex(index: Int): Pair<MutableList<T>, Int> {
        if (index == 0) return Pair(lists.first(), 0)
        val (l, i) = listAtIndex(index - 1)
        return Pair(l, i + 1)
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val (l, i) = listBeforeIndex(index)
        return l.addAll(i, elements)
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return lists.fold(false) { prev, list ->
            list.removeAll(elements) ||prev // don't shortcircuit
        }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        return lists.fold(false) { prev, list ->
            list.retainAll(elements) || prev // don't shortcircuit
        }
    }

    override fun clear() {
        lists = listOf(mutableListOf())
    }

    override fun get(index: Int): T {
        val (l, i) = listAtIndex(index)
        return l[i]
    }

    override operator fun set(index: Int, element: T): T {
        val (l, i) = listAtIndex(index)
        return l.set(i, element)
    }

    override fun add(index: Int, element: T) {
        val (l, i) = listBeforeIndex(index)
        l.add(index, element)
    }

    override fun removeAt(index: Int): T {
        val (l, i) = listAtIndex(index)
        return l.removeAt(i)
    }

    override fun indexOf(element: T): Int {
        var offset = 0
        for (list in lists) {
            val idx = list.indexOf(element)
            if (idx>=0) return offset + idx
            offset += list.size
        }
        return -1
    }

    override fun lastIndexOf(element: T): Int {
        var offset = size
        for (list in lists.reversed()) {
            offset -= list.size
            val idx = list.lastIndexOf(element)
            if (idx >=0) return offset + idx
        }
        return -1
    }

    override fun listIterator(): MutableListIterator<T> {
        return CombiningListIterator(this)
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        return CombiningListIterator(this, index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        require(toIndex>=0 && fromIndex in 0 until toIndex)

        val (firstListNo, firstListIdx) = listNoAtIndex(fromIndex)
        val (lastListNo, lastListIdx) = listNoBeforeIndex(toIndex)
        return when {
            firstListNo == lastListNo -> lists[firstListNo].subList(firstListIdx, lastListIdx)
            else -> {
                val newLists = mutableListOf<MutableList<T>>()

                newLists.add(lists[firstListNo].run { subList(firstListNo, size) })
                newLists.addAll(lists.subList(firstListNo+1, lastListNo-1))
                newLists.add(lists[lastListNo].subList(0, lastListIdx))

                ConcatenatedList(newLists)
            }
        }
    }

}

private class CombiningIterator<T>(iterable: CombiningIterable<T, out MutableIterable<T>>) : MutableIterator<T> {

    private var iteratorIdx = 0
    private val iterators: List<MutableIterator<T>>

    init {
        iterators = iterable.lists.map { it.iterator() }
    }

    override fun hasNext(): Boolean {
        while (iteratorIdx < iterators.size) {
            if (iterators[iteratorIdx].hasNext()) {
                return true
            }
            ++iteratorIdx
        }
        return false
    }

    override fun next(): T {
        while (iteratorIdx < iterators.size) {
            if (iterators[iteratorIdx].hasNext()) {
                return iterators[iteratorIdx].next()
            }
            ++iteratorIdx
        }
        throw NoSuchElementException()
    }

    override fun remove() {
        iterators[iteratorIdx].remove()
    }

}

private class CombiningListIterator<T> : MutableListIterator<T> {

    private var iteratorIdx = 0
    private var itemIdx = 0
    private val iterators: List<MutableListIterator<T>>

    constructor(list: ConcatenatedList<T>) {
        iterators = list.lists.map { it.listIterator() }
    }

    constructor(list: ConcatenatedList<T>, index: Int) {
        val (positionedList, offset) = list.listNoAtIndex(index)
        iterators = list.lists.mapIndexed { idx, elemList ->
            when {
                positionedList > idx -> elemList.listIterator(elemList.size) // move to end
                positionedList == idx -> elemList.listIterator(offset)
                else -> elemList.listIterator()
            }
        }
        iteratorIdx = positionedList
        itemIdx = offset
    }

    override fun hasNext(): Boolean {
        while (iteratorIdx < iterators.size) {
            if (iterators[iteratorIdx].hasNext()) {
                return true
            }
            ++iteratorIdx
        }
        return false
    }

    override fun next(): T {
        while (iteratorIdx < iterators.size) {
            if (iterators[iteratorIdx].hasNext()) {
                itemIdx++
                return iterators[iteratorIdx].next()
            }
            ++iteratorIdx
        }
        throw NoSuchElementException()
    }

    override fun remove() {
        itemIdx--
        iterators[iteratorIdx].remove()
    }

    override fun hasPrevious(): Boolean {
        while (iteratorIdx >= 0) {
            if (iterators[iteratorIdx].hasPrevious()) {
                return true
            }
            if (iteratorIdx == 0) {
                break
            }
            --iteratorIdx
        }
        return false
    }

    override fun previous(): T {
        while (iteratorIdx >= 0) {
            if (iterators[iteratorIdx].hasPrevious()) {
                itemIdx--
                return iterators[iteratorIdx].previous()
            }
            if (iteratorIdx == 0) {
                break
            }
            --iteratorIdx
        }
        throw NoSuchElementException()
    }

    override fun nextIndex(): Int {
        return itemIdx + 1
    }

    override fun previousIndex(): Int {
        return itemIdx - 1
    }

    override fun set(element: T) {
        throw UnsupportedOperationException()
    }

    override fun add(element: T) {
        throw UnsupportedOperationException()
    }

}

private class MonitoringIterator<T>(
    private val listeners: Collection<CollectionChangeListener<T>>?,
    private val original: MutableIterator<T>
) : MutableIterator<T> {

    private var last: T? = null

    override fun hasNext(): Boolean {
        return original.hasNext()
    }

    override fun next(): T {
        last = original.next()
        return last ?: throw NoSuchElementException()
    }

    override fun remove() {
        original.remove()
        fireElementRemoved()
        last = null
    }

    private fun fireElementRemoved() {
        if (listeners != null) {
            var error: RuntimeException? = null
            for (listener in listeners) {
                try {
                    listener.elementRemoved(last!!)
                } catch (e: RuntimeException) {
                    if (error == null) {
                        error = e
                    }
                }

            }
            if (error != null) {
                throw error
            }
        }
    }

}

private object EmptySortedSet : AbstractMutableSet<Any?>(), SortedSet<Any?> {

    override fun comparator(): Comparator<in Any?>? = null

    override fun add(element: Any?): Boolean {
        throw UnsupportedOperationException("empty lists have no subsets")
    }

    override fun subSet(fromElement: Any?, toElement: Any?): SortedSet<Any?> {
        throw UnsupportedOperationException("empty lists have no subsets")
    }

    override fun headSet(toElement: Any?): SortedSet<Any?> {
        throw UnsupportedOperationException("empty lists have no subsets")
    }

    override fun tailSet(fromElement: Any?): SortedSet<Any?> {
        throw UnsupportedOperationException("empty lists have no subsets")
    }

    override fun first(): Any? {
        throw UnsupportedOperationException("empty lists have no subsets")
    }

    override fun last(): Any? {
        throw UnsupportedOperationException("empty lists have no subsets")
    }

    override fun iterator(): MutableIterator<Any?> {
        // Use this roundabout way as Android does not have Collections.emptyIterator
        // It does have emptySet().iterator() though which has the same effect.
        return java.util.Collections.emptySet<Any?>().iterator()
    }

    override val size: Int get() = 0

}

fun hasNull(objects: Collection<*>): Boolean {
    for (o in objects) {
        if (o == null) return true
    }
    return false
}

fun isNullOrEmpty(content: ByteArray?): Boolean {
    return content == null || content.size == 0
}

fun isNullOrEmpty(content: ShortArray?): Boolean {
    return content == null || content.size == 0
}

fun isNullOrEmpty(content: BooleanArray?): Boolean {
    return content == null || content.size == 0
}

fun isNullOrEmpty(content: CharArray?): Boolean {
    return content == null || content.size == 0
}

fun isNullOrEmpty(content: IntArray?): Boolean {
    return content == null || content.size == 0
}

fun isNullOrEmpty(content: LongArray?): Boolean {
    return content == null || content.size == 0
}

fun isNullOrEmpty(content: FloatArray?): Boolean {
    return content == null || content.size == 0
}

fun isNullOrEmpty(content: DoubleArray?): Boolean {
    return content == null || content.size == 0
}

@OptIn(ExperimentalContracts::class)
fun isNullOrEmpty(content: Array<Any>?): Boolean {
    contract {
        returns(false) implies (content != null)
    }
    return content == null || content.isEmpty()
}

fun <T> toArrayList(values: Iterable<T>): ArrayList<T> {
    if (values is ArrayList<*>) {
        return values as ArrayList<T>
    }
    if (values is Collection<*>) {
        return ArrayList(values as Collection<T>)
    }
    val result = ArrayList<T>()
    for (value in values) {
        result.add(value)
    }
    return result
}


@Suppress("UNCHECKED_CAST")
fun <T> emptyLinkedHashSet(): LinkedHashSet<T> {
    return EmptyLinkedHashSet as LinkedHashSet<T>
}

private object EmptyLinkedHashSet : LinkedHashSet<Any?>() {

    override fun iterator(): MutableIterator<Any?> {
        // Use this roundabout way as Android does not have Collections.emptyIterator
        // It does have emptySet().iterator() though which has the same effect.
        return mutableSetOf<Any?>().iterator()
    }

    override val size: Int get() = 0

    override fun isEmpty(): Boolean {
        return true
    }

    override operator fun contains(element: Any?): Boolean {
        return false
    }

    override fun add(element: Any?): Boolean {
        throw UnsupportedOperationException("Not mutable")
    }

    override fun remove(element: Any?): Boolean {
        return false
    }

    override fun clear() { /* noop */
    }

    override fun clone(): EmptyLinkedHashSet = this

    private const val serialVersionUID = 979867754186273651L
}

/**
 * Check whether the collection contains only elements assignable to specified
 * class. This should allways hold when generics are used by the compiler
 *
 * @param T The type of the elements that returned collection should contain.
 * @param X The type of the elements of the checked collection.
 * @param clazz The class of which the elements need to be in the collection.
 * @param collection The collection to be checked
 * @return the checked collection. This allows faster assignment.
 * @throws ClassCastException When the element is not of the right class.
 */
@Throws(ClassCastException::class)
fun <T, X : T> checkClass(clazz: Class<T>, collection: Collection<X>): Collection<X> {
    for (x in collection) {
        if (!clazz.isInstance(x)) {
            throw ClassCastException("An element of the collection is not of the required type: " + clazz.name)
        }
    }

    return collection
}

/**
 * Create an iterable that combines the given iterables.
 * @param first The first iterable.
 * @param others The other iterables.
 * @return An iterable that combines both when iterating.
 */
@SafeVarargs
fun <T> combine(first: MutableIterable<T>, vararg others: MutableIterable<T>): MutableIterable<T> {
    return CombiningIterable(first, others)
}

/**
 * Create an iterable that combines the given iterables.
 * @param first The first iterable.
 * @param other The other iterable.
 * @return An iterable that combines both when iterating.
 */
fun <T> combine(first: MutableIterable<T>, other: MutableIterable<T>): MutableIterable<T> {
    return CombiningIterable(first, arrayOf(other))
}

fun <T> concatenate(first: MutableList<T>, second: MutableList<T>): MutableList<T> {
    return ConcatenatedList(first, arrayOf(second))
}

@SafeVarargs
fun <T> concatenate(first: MutableList<T>, vararg others: MutableList<T>): MutableList<T> {
    return ConcatenatedList(first, others)
}

fun <T : Comparable<T>> sortedList(collection: Collection<T>): List<T> {
    val result = ArrayList(collection)
    Collections.sort(result)
    return result
}

fun <T, U> createMap(key: T, value: U): HashMap<T, U> {
    val map = HashMap<T, U>()
    map[key] = value
    return map
}

/**
 * Create a hashmap from a set of key-value pairs.
 *
 * @param T The type of the keys to the hashmap
 * @param U The type of the values.
 * @param tupples The elements to put into the map.
 * @return The resulting hashmap.
 */
@SafeVarargs
@Deprecated("Use hashmapOf")
@Suppress("DEPRECATION")
fun <T, U> hashMap(vararg tupples: Tupple<out T, out U>): HashMap<T, U> {
    // Make the new hashmap have a capacity 125% of the amount of tuples.
    val result = HashMap<T, U>(tupples.size + (tupples.size shr 2))
    for (t in tupples) {
        result[t.elem1] = t.elem2
    }
    return result
}

@SafeVarargs
@Deprecated("Use the version enumMapOf taking pairs")
@Suppress("DEPRECATION")
fun <T : Enum<T>, U> enumMap(vararg tupples: Tupple<out T, out U>): EnumMap<T, U> {
    if (tupples.isEmpty()) {
        throw IllegalArgumentException("For an enumeration map simple creator, at least one element must be present")
    }
    @Suppress("UNCHECKED_CAST")
    val type = Enum::class.java.asSubclass(tupples[0].first.javaClass) as java.lang.Class<T>
    val result = EnumMap<T, U>(type)
    for (t in tupples) {
        result[t.first] = t.second
    }
    return result
}

fun <T : Enum<T>, U> enumMapOf(vararg tupples: Pair<T, U>): EnumMap<T, U> {
    if (tupples.size < 1) {
        throw IllegalArgumentException("For an enumeration map simple creator, at least one element must be present")
    }
    @Suppress("UNCHECKED_CAST")
    val type: Class<T> = Enum::class.java.asSubclass(tupples[0].first.javaClass) as java.lang.Class<T>
    val result = EnumMap<T, U>(type)
    for (t in tupples) {
        result[t.first] = t.second
    }
    return result
}


@Deprecated("Use {@link Collections#singletonList(Object)}")
fun <T> singletonList(elem: T): List<T> {
    return listOf(elem)
}

fun <T> monitoringIterator(
    listeners: Collection<CollectionChangeListener<T>>,
    original: MutableIterator<T>
): MutableIterator<T> {
    return MonitoringIterator(listeners, original)
}

fun <T> mergeLists(base: MutableList<T>, other: List<T>) {
    for (i in other.indices) {
        if (i >= base.size) {
            for (j in i until other.size) {
                base.add(other[i])
            }
            break
        }
        val current = base[i]
        val replacement = other[i]
        if (if (current == null) replacement != null else current != replacement) {
            // not equal
            var next: Any?
            if (i + 1 < base.size && ((base[i + 1].also {
                    next = it
                }) === replacement || next != null && next == replacement)) {
                base.removeAt(i) // Remove the current item so there is a match, the item was removed in the other
            } else if (i + 1 < other.size && (current === (other[i + 1].also {
                    next = it
                }) || current != null && current == next)) {
                base.add(i, replacement) // Insert the item here. The item was added in the other list
            } else {
                base[i] = replacement// In other cases, just set the value
            }
        }
    }
    while (base.size > other.size) {
        base.removeAt(base.size - 1) // Remove the last item
    }

}

/**
 * Create a collection wrapper that allows for monitoring of changes to the collection. For this to work the changes
 * need to be made through the monitor.
 * @param collection The collection to monitor
 * @param <T> The type contained in the collection
 * @return The collection that can be monitored.
</T> */
fun <T> monitorableCollection(collection: MutableCollection<T>): MonitorableCollection<T> {
    return MonitoringCollectionAdapter(collection)
}

fun containsInstances(collection: Iterable<*>, vararg classes: Class<*>): Boolean {
    for (element in collection) {
        for (c in classes) {
            if (c.isInstance(element)) {
                return true
            }
        }
    }
    return false
}

/**
 * Add instances of any of the target classes to the given collection.
 * @param target The receiving collection
 * @param source The source iterable
 * @param verifiers The classes to check. Only one needs to match.
 * @param <T> The type contained in the collection
 * @param <V> The resulting collection type.
 * @return This returns `target`
</V></T> */
@SafeVarargs
fun <T, V : MutableCollection<T>> addInstancesOf(target: V, source: Iterable<*>, vararg verifiers: Class<out T>): V {
    for (element in source) {
        for (c in verifiers) {
            if (c.isInstance(element)) {
                val e = c.cast(element)
                target.add(e)
                break
            }
        }
    }
    return target
}


fun <T, V : MutableCollection<T>> addNonInstancesOf(target: V, source: Iterable<T>, vararg verifiers: Class<*>): V {
    for (element in source) {
        for (c in verifiers) {
            if (!c.isInstance(element)) {
                target.add(element)
                break
            }
        }
    }
    return target
}

fun <T> copy(orig: Collection<T>?): List<T>? {
    if (orig == null) {
        return null
    }
    if (orig.size == 1) {
        return listOf(orig.iterator().next())
    }
    val result = ArrayList<T>(orig.size)
    result.addAll(orig)
    return result
}

