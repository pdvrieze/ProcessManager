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

package net.devrieze.util

internal class MonitoringCollectionAdapter<V>(private val delegate: MutableCollection<V>) : MonitorableCollection<V> {
    private var _listeners: MutableSet<CollectionChangeListener<V>>? = null


    private val listeners: MutableSet<CollectionChangeListener<V>>
        get() = _listeners ?: hashSetOf<CollectionChangeListener<V>>().also { _listeners = it }

    override fun addCollectionChangeListener(listener: CollectionChangeListener<V>) {
        listeners.add(listener)
    }

    override fun removeCollectionChangeListener(listener: CollectionChangeListener<V>) {
        _listeners?.remove(listener)
    }

    private inline fun fireEvent(trigger: CollectionChangeListener<V>.()->Unit) {
        @Suppress("RemoveExplicitTypeArguments")
        val error = _listeners?.fold<CollectionChangeListener<V>,MultiException?>(null) { e: MultiException?, listener ->
            try {
                listener.trigger()
                e
            } catch (f: Exception) {
                MultiException.add(e,f)
            }
        }
        MultiException.throwIfError(error)

    }

    private fun fireAddEvent(element: V) {
        fireEvent { elementAdded(element) }
    }

    private fun fireClearEvent() {
        fireEvent { collectionCleared() }
    }

    private fun fireRemoveEvent(element: V) {
        fireEvent { elementRemoved(element) }
    }

    override fun add(element: V): Boolean {
        return delegate.add(element).also {
            if (it) fireAddEvent(element)
        }
    }

    override fun addAll(elements: Collection<V>): Boolean {
        var result = false
        for (elem in elements) {
            result = result or add(elem)
        }
        return result
    }

    override fun clear() {
        delegate.clear()
        fireClearEvent()
    }

    override fun contains(element: V): Boolean = delegate.contains(element)

    override fun containsAll(elements: Collection<V>): Boolean = delegate.containsAll(elements)


    override fun equals(other: Any?): Boolean {
        return delegate == other
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun isEmpty(): Boolean {
        return delegate.isEmpty()
    }

    override fun iterator(): MutableIterator<V> {
        return CollectionUtil.monitoringIterator(listeners, delegate.iterator())
    }

    override fun remove(element: V): Boolean {
        return delegate.remove(element).also {
            if (it) fireRemoveEvent(element)
        }
    }


    override fun removeAll(elements: Collection<V>): Boolean {
        return elements.fold(false) { changed, element -> changed or remove(element) }
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        return removeIf { elements.contains(it) }
    }

    override val size: Int get() = delegate.size


}