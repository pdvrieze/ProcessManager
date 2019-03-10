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
import kotlin.NoSuchElementException


/**
 * This class provides functions that the Collections class does not.
 *
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */
val EMPTYSORTEDSET: SortedSet<*> = EmptySortedSet


private open class CombiningIterable<T, U : MutableIterable<T>>(internal var first: U,
                                                                internal var others: Array<out U>) : MutableIterable<T> {

    override fun iterator(): MutableIterator<T> {
        return CombiningIterator(this)
    }

}

private class ConcatenatedList<T>(first: MutableList<T>, others: Array<out MutableList<T>>) :
    CombiningIterable<T, MutableList<T>>(first, others), MutableList<T> {

    private fun first(): MutableList<T> {
        return first
    }

    private fun others(): Array<out MutableList<T>> {
        return others
    }

    override val size: Int
        get() {
            var size = first().size
            for (other in others()) {
                size += other.size
            }
            return size
        }

    override fun isEmpty(): Boolean {
        if (!first().isEmpty()) {
            return false
        }
        for (other in others()) {
            if (!other.isEmpty()) {
                return false
            }
        }
        return true
    }

    override operator fun contains(element: T): Boolean {
        if (!first().contains(element)) {
            return true
        }
        for (other in others()) {
            if (other.contains(element)) {
                return true
            }
        }
        return false
    }

/*
    override fun toArray(): Array<T> {
        val result = arrayOfNulls<Any>(size)
        return toArrayHelper<Any>(result)
    }

    private fun <V> toArrayHelper(result: Array<V>): Array<V> {
        var i = 0
        for (elem in this) {
            result[i] = elem as V
            ++i
        }
        return result
    }

    override fun <V> toArray(init: Array<V>): Array<V> {
        val size = size
        var result = init
        if (init.size < size) {
            result = Array.newInstance(init.javaClass.getComponentType(), size) as Array<V>
        } else if (init.size > size) {
            result[size] = null
        }
        return toArrayHelper<V>(result)
    }
*/

    override fun add(element: T): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(element: T): Boolean {
        if (first.remove(element)) {
            return true
        }
        for (other in others()) {
            if (other.remove(element)) {
                return true
            }
        }
        return false
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        val all: MutableSet<T> = HashSet(elements)
        all.removeAll(first())
        if (all.isEmpty()) {
            return true
        }
        for (other in others()) {
            all.removeAll(other)
            if (all.isEmpty()) {
                return true
            }
        }
        return all.isEmpty()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var result = first().removeAll(elements)
        for (other in others()) {
            result = other.removeAll(elements) || result
        }
        return result
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        var result = first().retainAll(elements)
        for (other in others()) {
            result = other.retainAll(elements) || result
        }
        return result
    }

    override fun clear() {
        first = mutableListOf()
        others = emptyArray()
    }

    override fun get(index: Int): T {
        var offset = first().size
        if (index < offset) {
            return first()[index]
        }
        for (other in others()) {
            val oldOffset = offset
            offset += other.size
            if (index < offset) {
                return other.get(index - oldOffset)
            }
        }
        throw IndexOutOfBoundsException()
    }

    override operator fun set(index: Int, element: T): T {
        throw UnsupportedOperationException()
        //      int offset = first().size();
        //      if (index<offset) {
        //        return first().set(index, element);
        //      }
        //      for(List<? extends T> other:others()) {
        //        int oldOffset = offset;
        //        offset+=other.size();
        //        if (index<offset) {
        //          return other.set(index - oldOffset, element);
        //        }
        //      }
        //      throw new IndexOutOfBoundsException();
    }

    override fun add(index: Int, element: T) {
        throw UnsupportedOperationException()
    }

    override fun removeAt(index: Int): T {
        var offset = first().size
        if (index < offset) {
            return first().removeAt(index)
        }
        for (other in others()) {
            val oldOffset = offset
            offset += other.size
            if (index < offset) {
                return other.removeAt(index - oldOffset)
            }
        }
        throw IndexOutOfBoundsException()
    }

    override fun indexOf(element: T): Int {
        run {
            val idx = first().indexOf(element)
            if (idx >= 0) {
                return idx
            }
        }
        var offset = first().size
        for (other in others()) {
            val idx = other.indexOf(element)
            if (idx >= 0) {
                return offset + idx
            }
            offset += other.size
        }
        throw IndexOutOfBoundsException()
    }

    override fun lastIndexOf(element: T): Int {
        var offset = size - others()[others.size - 1].size
        for (i in others.indices.reversed()) {
            val other = others()[i]
            offset -= other.size
            val idx = other.lastIndexOf(element)
            if (idx >= 0) {
                return offset + idx
            }
        }
        return first().lastIndexOf(element)
    }

    override fun listIterator(): MutableListIterator<T> {
        return CombiningListIterator(this)
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        return CombiningListIterator(this, index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        throw UnsupportedOperationException()
    }

}

private class CombiningIterator<T>(iterable: CombiningIterable<T, out MutableIterable<T>>) : MutableIterator<T> {

    private var mIteratorIdx = 0
    private val mIterators: List<MutableIterator<T>>

    init {
        mIterators = toIterators(iterable.first, iterable.others)
    }

    private fun <T> toIterators(first: MutableIterable<T>,
                                others: Array<out MutableIterable<T>>): List<MutableIterator<T>> {
        return ArrayList<MutableIterator<T>>(others.size + 1).apply {
            add(first.iterator())
            others.mapTo(this) { it.iterator() }
        }
    }

    override fun hasNext(): Boolean {
        while (mIteratorIdx < mIterators.size) {
            if (mIterators[mIteratorIdx].hasNext()) {
                return true
            }
            ++mIteratorIdx
        }
        return false
    }

    override fun next(): T {
        while (mIteratorIdx < mIterators.size) {
            if (mIterators[mIteratorIdx].hasNext()) {
                return mIterators[mIteratorIdx].next()
            }
            ++mIteratorIdx
        }
        throw NoSuchElementException()
    }

    override fun remove() {
        mIterators[mIteratorIdx].remove()
    }

}

private class CombiningListIterator<T> : MutableListIterator<T> {

    private var iteratorIdx = 0
    private var itemIdx = 0
    private val iterators: List<MutableListIterator<T>>

    constructor(list: ConcatenatedList<T>) {
        iterators = toIterators(list.first, list.others)
    }

    constructor(list: ConcatenatedList<T>, index: Int) {
        val first = list.first
        val others = list.others
        iteratorIdx = -1

        iterators = ArrayList(others.size + 1)
        if (index < first.size) {
            iteratorIdx = 0
            iterators.add(first.listIterator(index))
        } else {
            iterators.add(first.listIterator())
        }

        val offset = first.size
        for (other in others) {
            if (iteratorIdx < 0 && index - offset < other.size) {
                iterators.add(other.listIterator(index - offset))
                iteratorIdx = iterators.size - 1
            } else {
                iterators.add(other.listIterator())
            }
        }
        if (iteratorIdx < 0) {
            throw IndexOutOfBoundsException()
        }
    }

    private fun <T> toIterators(first: MutableList<T>,
                                others: Array<out MutableList<T>>): List<MutableListIterator<T>> {
        val result = ArrayList<MutableListIterator<T>>(others.size + 1)
        result.add(first.listIterator())
        for (other in others) {
            result.add(other.listIterator())
        }
        return result
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

    override fun set(e: T) {
        throw UnsupportedOperationException()
    }

    override fun add(e: T) {
        throw UnsupportedOperationException()
    }

}

private class MonitoringIterator<T>(private val listeners: Collection<CollectionChangeListener<T>>?,
                                    private val original: MutableIterator<T>) : MutableIterator<T> {

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

fun isNullOrEmpty(content: Array<Any>?): Boolean {
    return content == null || content!!.size == 0
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
@Deprecated("Use {@link Collections#emptySortedSet()} when on Java 1.8")
fun <T> emptySortedSet(): SortedSet<T> {
    return EMPTYSORTEDSET as SortedSet<T>
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
 * @param <T> The type of the keys to the hashmap
 * @param <U> The type of the values.
 * @param tupples The elements to put into the map.
 * @return The resulting hashmap.
</U></T> */
@SafeVarargs
fun <T, U> hashMap(vararg tupples: Tupple<out T, out U>): HashMap<T, U> {
    // Make the new hashmap have a capacity 125% of the amount of tuples.
    val result = HashMap<T, U>(tupples.size + (tupples.size shr 2))
    for (t in tupples) {
        result[t.elem1] = t.elem2
    }
    return result
}

@SafeVarargs
fun <T : Enum<T>, U> enumMap(vararg tupples: Tupple<out T, out U>): EnumMap<T, U> {
    if (tupples.size < 1) {
        throw IllegalArgumentException(
            "For an enumeration map simple creator, at least one element must be present")
    }
    val type = Enum::class.java.asSubclass(tupples[0].elem1.javaClass) as Class<T>
    val result = EnumMap<T, U>(type)
    for (t in tupples) {
        result[t.elem1] = t.elem2
    }
    return result
}


@Deprecated("Use {@link Collections#singletonList(Object)}")
fun <T> singletonList(elem: T): List<T> {
    return listOf(elem)
}

fun <T> monitoringIterator(listeners: Collection<CollectionChangeListener<T>>,
                           original: MutableIterator<T>): MutableIterator<T> {
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
            if (i + 1 < base.size && ((base[i + 1].also { next = it }) === replacement || next != null && next == replacement)) {
                base.removeAt(i) // Remove the current item so there is a match, the item was removed in the other
            } else if (i + 1 < other.size && (current === (other[i + 1].also { next = it }) || current != null && current == next)) {
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

