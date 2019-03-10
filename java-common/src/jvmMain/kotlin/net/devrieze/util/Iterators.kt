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

import org.jetbrains.annotations.Contract

import java.util.*


object Iterators {


    private class AutoCloseIterable<T, V>(private val parent: T) : Iterable<V>, MutableIterator<V>
        where T : Iterable<V>, T : AutoCloseable {
        
        private var iterator: Iterator<V>? = null

        override fun iterator(): MutableIterator<V> {
            val iterator = parent.iterator().also { this.iterator = it }
            if (!iterator.hasNext()) {
                this.iterator
                try {
                    parent.close()
                } catch (ex: Exception) {
                    throw RuntimeException(ex)
                }

            }
            return this
        }

        override fun hasNext(): Boolean {
            return iterator != null && iterator!!.hasNext()
        }

        override fun next(): V {
            try {
                val n = iterator!!.next()
                if (n == null) {
                    iterator = null
                    parent.close()
                }
                return n
            } catch (e: Exception) {
                try {
                    parent.close()
                } catch (ex: Exception) {
                    e.addSuppressed(ex)
                }


                if (e is RuntimeException) {
                    throw e
                } else {
                    throw RuntimeException(e)
                }
            }

        }

        override fun remove() {
            try {
                (iterator as? MutableIterator)?.remove() ?: throw UnsupportedOperationException("Iterator not mutable")
            } catch (e: Exception) {
                try {
                    parent.close()
                } catch (e1: Exception) {
                    e.addSuppressed(e1)
                }

                throw e
            }

        }

    }

    private class EnumIterator<T>(private val mEnumeration: Enumeration<T>) : Iterator<T> {

        override fun hasNext(): Boolean {
            return mEnumeration.hasMoreElements()
        }

        override fun next(): T {
            return mEnumeration.nextElement()
        }

    }

    private class MergedIterable<T>(private val iterables: Array<out Iterable<T>>) : Iterable<T> {

        private inner class MergedIterator : MutableIterator<T> {

            internal var index = 0

            internal var iterator: Iterator<T>? = iterables[0].iterator()

            override fun hasNext(): Boolean {
                if (iterator?.hasNext() == true) {
                    return true
                }
                while (iterator?.hasNext() == false) {
                    ++index
                    if (index < iterables.size) {
                        iterator = iterables[index].iterator()
                    } else {
                        iterator = null
                    }
                }
                return iterator != null
            }

            override fun next(): T {
                if (!hasNext()) {
                    throw NoSuchElementException("Reading past iterator end")
                }
                return iterator!!.next()
            }

            override fun remove() {
                (iterator as? MutableIterator)?.remove() ?: throw UnsupportedOperationException(
                    "Parent iterator not mutable")
            }

        }

        override fun iterator(): Iterator<T> {
            return MergedIterator()
        }

    }

    @SafeVarargs
    fun <T> merge(vararg iterables: Iterable<T>): Iterable<T> {
        if (iterables.isEmpty()) {
            return emptyList()
        } else if (iterables.size == 1) {
            return iterables[0]
        }
        return MergedIterable(iterables)
    }

    @Contract(pure = true)
    fun <T> toIterable(e: Enumeration<T>): Iterable<T> {
        return object : Iterable<T> {

            override fun iterator(): Iterator<T> {
                return EnumIterator(e)
            }


        }
    }

    fun <T> toList(pIterator: Iterator<T>): List<T> {
        if (!pIterator.hasNext()) {
            return emptyList()
        }
        val value = pIterator.next()
        if (!pIterator.hasNext()) {
            return listOf(value)
        }
        val result = ArrayList<T>()
        result.add(value)
        do {
            result.add(pIterator.next())
        } while (pIterator.hasNext())
        return result
    }

    fun <T> toList(pEnumeration: Enumeration<T>): List<T> {
        return toList(EnumIterator(pEnumeration))
    }

    fun <T> toList(pIterable: Iterable<T>): List<T> {
        return toList(pIterable.iterator())
    }

    fun <T, V> autoClose(iterable: T): Iterable<V> where T : Iterable<V>, T : AutoCloseable {
        return AutoCloseIterable(iterable)
    }
}
