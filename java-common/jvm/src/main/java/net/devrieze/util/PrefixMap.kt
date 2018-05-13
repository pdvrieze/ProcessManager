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


class PrefixMap<V : Any> : AbstractCollection<PrefixMap.Entry<V>>() {

    private var root: Node<V>? = Node("")

    private class NodeIterator<T : Any>(root: Node<T>) : MutableIterator<EntryImpl<T>> {

        internal val stack: Deque<Node<T>> = ArrayDeque()

        init {
            stack.push(root)
            getLeftMost()
        }

        private fun getLeftMost(): Boolean {
            val top = stack.peek()
            if (top.left != null) {
                stack.push(top.left)
                if (getLeftMost()) {
                    return true
                }
            }
            if (top.value != null) {
                return true
            }
            if (top.below != null) {
                stack.push(top.below)
                if (getLeftMost()) {
                    return true
                }
            }
            if (top.right != null) {
                stack.push(top.right)
                if (getLeftMost()) {
                    return true
                }
            }

            stack.pop()
            return false
        }

        private fun getNext(): Boolean {
            var top = stack.peek()
            if (top.below != null) {
                stack.push(top.below)
                if (getLeftMost()) {
                    return true
                }
            }
            if (top.right != null) {
                stack.push(top.right)
                if (getLeftMost()) {
                    return true
                }
            }
            while (stack.size > 1) {
                top = stack.pop()
                val parent = stack.peek()
                if (top == parent.left) {
                    if (parent.value != null) {
                        return true
                    } else {
                        if (parent.below != null) {
                            stack.push(parent.below)
                            if (getLeftMost()) {
                                return true
                            }
                        }
                    }
                }
                if ((top == parent.left || top == parent.below) && parent.right != null) {
                    stack.push(parent.right)
                    if (getLeftMost()) {
                        return true
                    }
                }
            }
            stack.pop()
            return false
        }

        override fun hasNext(): Boolean {
            return !stack.isEmpty()
        }

        override fun next(): EntryImpl<T> {
            val n = stack.peek()
            if (n?.value == null) {
                throw IllegalStateException("Invalid next value in iterator")
            }
            val result = EntryImpl(n).also { getNext() }

            return result
        }

        override fun remove() {
            throw UnsupportedOperationException("Removing is not supported in this iterator")
        }
    }

    private class CompareResult constructor(internal val commonPrefix: String?,
                                            internal val cmp: XCompareResult) {

        internal val isLeft: Boolean
            @Contract(pure = true)
            get() = cmp.isLeft

        internal val isAbove: Boolean
            @Contract(pure = true)
            get() = cmp.isAbove

        internal val isEqual: Boolean
            @Contract(pure = true)
            get() = cmp.isEqual

        internal val isBelow: Boolean
            @Contract(pure = true)
            get() = cmp.isBelow

        internal val isEqOrBelow: Boolean
            @Contract(pure = true)
            get() = cmp.isEqOrBelow

        internal val isEqOrAbove: Boolean
            @Contract(pure = true)
            get() = cmp.isEqOrBelow

        internal val isRight: Boolean
            @Contract(pure = true)
            get() = cmp.isRight

        /**
         * Check whether the comparison corresponds to the given index.
         * @param index The positional index
         * @return `true` when it is, `false` when not.
         */
        @Contract(pure = true)
        fun equals(index: Int): Boolean {
            return cmp.equals(index)
        }

        @Contract(pure = true)
        internal fun isOpposite(index: Int): Boolean {
            return cmp.isOpposite(index)
        }

        @Contract(pure = true)
        internal fun isOpposite(compareResult: XCompareResult): Boolean {
            return cmp.isOpposite(compareResult)
        }

        @Contract(pure = true)
        fun invert(): CompareResult {
            return CompareResult(commonPrefix, cmp.invert())
        }

        @Contract(pure = true)
        override fun toString(): String {
            return if (commonPrefix == null) {
                cmp.toString()
            } else {
                "$cmp[\"$commonPrefix\"]"
            }
        }

        companion object {

            internal val LEFT = CompareResult(null, XCompareResult.XLEFT)

            internal val ABOVE = CompareResult(null, XCompareResult.XABOVE)

            internal val EQUAL = CompareResult(null, XCompareResult.XEQUAL)

            internal val BELOW = CompareResult(null, XCompareResult.XBELOW)

            internal val RIGHT = CompareResult(null, XCompareResult.XRIGHT)
        }

    }

    private enum class XCompareResult(val index: Int) {
        XLEFT(LEFTIDX) {
            override fun <T : Any> getChild(parent: Node<T>) = parent.left

            override fun <T : Any> setChild(parent: Node<T>, child: Node<T>?) = parent.setLeft(child)
        },
        XABOVE(-1) {
            override fun <T : Any> getChild(parent: Node<T>) =
                throw UnsupportedOperationException("Getting parents is impossible")

            override fun <T : Any> setChild(parent: Node<T>, child: Node<T>?) =
                throw UnsupportedOperationException("Setting parents is impossible")
        },
        XEQUAL(BELOWIDX) {
            override fun <T : Any> getChild(parent: Node<T>) = parent.below

            override fun <T : Any> setChild(parent: Node<T>, child: Node<T>?) = parent.setBelow(child)
        },
        XBELOW(BELOWIDX) {
            override fun <T : Any> getChild(parent: Node<T>) = parent.below

            override fun <T : Any> setChild(parent: Node<T>, child: Node<T>?) = parent.setBelow(child)
        },
        XRIGHT(RIGHTIDX) {
            override fun <T : Any> getChild(parent: Node<T>) = parent.right

            override fun <T : Any> setChild(parent: Node<T>, child: Node<T>?) = parent.setRight(child)
        };

        internal val isLeft: Boolean
            @Contract(pure = true)
            get() = this == XLEFT

        internal val isAbove: Boolean
            @Contract(pure = true)
            get() = this == XABOVE

        internal val isEqual: Boolean
            @Contract(pure = true)
            get() = this == XEQUAL

        internal val isBelow: Boolean
            @Contract(pure = true)
            get() = this == XBELOW

        internal val isEqOrBelow: Boolean
            @Contract(pure = true)
            get() = this == XEQUAL || this == XBELOW

        internal val isRight: Boolean
            @Contract(pure = true)
            get() = this == XRIGHT

        fun invert() = when (this) {
            XLEFT  -> XRIGHT
            XABOVE -> XBELOW
            XEQUAL -> XEQUAL
            XBELOW -> XABOVE
            XRIGHT -> XLEFT
        }

        @Contract(pure = true)
        internal fun equals(index: Int): Boolean {
            return index == this.index
        }

        @Contract(pure = true)
        internal fun isOpposite(index: Int): Boolean {
            return 2 - index == this.index
        }

        @Contract(pure = true)
        internal fun isOpposite(other: XCompareResult): Boolean {
            return 2 - other.index == this.index
        }

        abstract fun <T : Any> getChild(parent: Node<T>): Node<T>?
        abstract fun <T : Any> setChild(parent: Node<T>, child: Node<T>?): Node<T>?
    }

    private class Node<T : Any>(val prefix: String,
                                var value: T? = null,
                                left: Node<T>? = null,
                                below: Node<T>? = null,
                                right: Node<T>? = null) {

        var count: Int = (if (value == null) 0 else 1) + (left?.count ?: 0) + (below?.count ?: 0) + (right?.count ?: 0)
            private set

        var left: Node<T>? = left
            set(value) {
                val oldValue = field
                if (oldValue != value) {
                    if (oldValue != null) count -= oldValue.count

                    assert(value == null || value.prefix < prefix)

                    field = value

                    if (value != null) count += value.count
                }
            }

        var right: Node<T>? = right
            set(value) {
                val oldValue = field
                if (oldValue != value) {
                    if (oldValue != null) count -= oldValue.count

                    assert(value == null || value.prefix > prefix)

                    field = value

                    if (value != null) count += value.count
                }
            }

        var below: Node<T>? = below
            set(value) {
                val oldChild = field
                if (oldChild != value) {
                    if (oldChild != null) count -= oldChild.count

                    assert(value == null || value.prefix.length >= prefix.length)

                    field = value

                    if (value != null) count += value.count
                }
            }

        fun setLeft(left: Node<T>?): Node<T>? {
            return this.left.also { this.left = left }
        }

        fun setRight(right: Node<T>?): Node<T>? {
            return this.right.also { this.right = right }
        }

        fun setBelow(below: Node<T>?): Node<T>? {
            return this.below.also { this.below = below }
        }

        fun copy(prefix: String = this.prefix,
                 value: T? = this.value,
                 left: Node<T>? = this.left,
                 below: Node<T>? = this.below,
                 right: Node<T>? = this.right): Node<T> =
            Node(prefix, value, left?.copy(), below?.copy(), right?.copy())

        fun add(compareResult: CompareResult, child: Node<T>): Int {
            val expectedCount = count + child.count
            assert(expectedCount != Integer.MIN_VALUE)
            assert(compareResult.cmp == prefixCompare(prefix, child.prefix).cmp) { "Accuracy of cached comparisons" }
            assert(!compareResult.isAbove) { "Don't add a child that should be a parent" }

            return addChild(compareResult.cmp, child).also {
                assert(count == expectedCount) { "Correct child counts" }
            }
        }

        private fun addChild(posResult: XCompareResult, child: Node<T>): Int {
            var current: Node<T>? = posResult.getChild(this)
            if (current == null) {
                posResult.setChild(this, child)
                return child.count
            } else {
                var cmp = prefixCompare(0, current.prefix, child.prefix)
                if (!posResult.isEqOrBelow && cmp.isOpposite(posResult) || cmp.isAbove) {
                    val result = child.count
                    interpose(posResult, cmp.invert(), child)
                    rebalance(posResult)
                    return result
                } else {

                    val commonPrefix = cmp.commonPrefix
                    if (posResult.isEqOrBelow && commonPrefix != null && commonPrefix.length > prefix.length && commonPrefix.length < below!!.prefix.length) {
                        // introduce a new intermediate parent
                        current = Node(commonPrefix)
                        cmp = CompareResult.BELOW
                        interpose(posResult, CompareResult.BELOW, current)
                        assert(
                            current.count == current.below!!.count + (if (current.left == null) 0 else current.left!!.count) + if (current.right == null) 0 else current.right!!.count)
                    }

                    val addcnt = current.add(cmp, child)
                    count += addcnt
                    rebalance(posResult)
                    return addcnt
                }
            }
        }

        fun remove(compare: CompareResult, entry: EntryImpl<*>): Boolean {

            assert(compare.cmp == prefixCompare(prefix, entry.prefix).cmp)
            assert(!compare.isAbove)
            assert(!(compare.isEqual && entry.value == value))

            val oldCount = count
            val result = removeChild(compare.cmp, compare, entry)

            assert(count == oldCount - if (result) 1 else 0)
            return result
        }

        private fun removeChild(posResult: XCompareResult, compare: CompareResult, entry: EntryImpl<*>): Boolean {
            val current = posResult.getChild(this) ?: return false
            val cmp = prefixCompare(0, current.prefix, entry.prefix)
            if (cmp.isEqual && entry.value == current.value) {
                val oldNode = posResult.setChild(this, null)!!
                oldNode.value = null
                addIndividualElements(compare, oldNode)
                return true
            } else {
                return reduceCount(current.remove(cmp, entry))
            }
        }

        private fun reduceCount(pDoReduce: Boolean): Boolean {
            if (pDoReduce) {
                --count
            }
            return pDoReduce
        }

        private fun rebalance(idx: XCompareResult) {
            var originalCount = 0
            val balanceFactor = idx.getChild(this)?.run {
                originalCount = count
                balanceFactor
            } ?: return
            val origParentCount = count

            if (balanceFactor <= -2) {
                val originalRoot = idx.setChild(this, null)!!
                idx.setChild(this, rotateRight(originalRoot, true))
            } else if (balanceFactor >= 2) {
                val originalRoot = idx.setChild(this, null)!!
                idx.setChild(this, rotateLeft(originalRoot, true))
            }
            assert(idx.getChild(this)?.count == originalCount) { "Stable child counts" }
            assert(origParentCount == count) { "Stable parent counts" }
        }

        private val balanceFactor: Int
            get() {
                val leftCnt = left?.count ?: 0
                val rightCnt = right?.count ?: 0
                assert(leftCnt + rightCnt + (if (value == null) 0 else 1) + (below?.count ?: 0) == count)

                return rightCnt - leftCnt
            }


        private fun <U : Any> rotateLeft(originalRoot: Node<U>, testPivotBalance: Boolean): Node<U> {
            val cnt = originalRoot.count
            var pivot = originalRoot.setRight(null) ?: throw NullPointerException(
                "Pivot points can logically never be null")

            if (testPivotBalance && pivot.balanceFactor <= -1) {
                pivot = rotateRight(pivot, false)
            }

            originalRoot.setRight(pivot.setLeft(null))

            pivot.setLeft(originalRoot)
            return pivot.apply { assert(cnt == count) }
        }

        private fun <U : Any> rotateRight(originalRoot: Node<U>, testPivotBalance: Boolean): Node<U> {
            val cnt = originalRoot.count
            var pivot = originalRoot.setLeft(null) ?: throw NullPointerException(
                "Pivot points can logically never be null")
            if (testPivotBalance && pivot.balanceFactor >= 1) {
                pivot = rotateLeft(pivot, false)
            }

            val newLeft = pivot.setRight(null)
            originalRoot.setLeft(newLeft)
            pivot.setRight(originalRoot)
            return pivot.apply { assert(cnt == count) }
        }

        private fun interpose(posResult: XCompareResult, compareResult: CompareResult, newChild: Node<T>) {
            // Remove the old child from this node.
            val oldChild = posResult.setChild(this, null)

            // First add the old child to the new one (so newChild has the correct node count)
            if (oldChild != null) newChild.addIndividualElements(compareResult, oldChild)

            // Then set the new left.
            posResult.setChild(this, newChild)
        }

        private fun addIndividualElements(pCompareResult: CompareResult, pSource: Node<T>) {
            val sourceLeft = pSource.setLeft(null)
            val sourceRight = pSource.setRight(null)

            if (pSource.value != null) {
                // We can ignore the below nodes as they would remain under the source
                add(pCompareResult, pSource)
            } else if (pSource.below != null) {
                // The source is a placeholder, and can be removed. In this case
                // the below nodes must also be added (these could not exist if remove just demoted this node)
                add(pCompareResult, pSource.below!!)
            }
            if (sourceLeft != null) {
                add(prefixCompare(prefix, sourceLeft.prefix), sourceLeft)
            }
            if (sourceRight != null) {
                add(prefixCompare(prefix, sourceRight.prefix), sourceRight)
            }
        }

        override fun toString(): String {
            val result = StringBuilder()
            appendTo(result, "", "")
            return result.toString()
        }

        fun appendTo(b: StringBuilder, strprefix: String, labelLine: String) {
            val paddingLength = maxOf(0, labelLine.length - strprefix.length + prefix.length - 2)
            val basePrefix = StringBuilder().append(strprefix).append(' '.repeat(paddingLength))
            val left = left
            val right = right
            val below = below

            val inlineValue = below != null || left != null || right != null

            if (left == null) {
                b.append(basePrefix).append("     [===== (")
            } else {
                b.append(basePrefix).append("[===== (")
            }
            b.append(count).append(')')
            if (inlineValue && value != null) {
                b.append(" value=\"").append(value).append('"')
            }
            b.append('\n')

            val belowPrefix = StringBuilder().append(basePrefix).append("     ")
            belowPrefix.append("|")

            val siblingPrefix = StringBuilder(basePrefix.length + 1).append(basePrefix).append('|').toString()


            if (left != null) {
                left.appendTo(b, siblingPrefix, "$siblingPrefix l=")
                b.append('\n').append(basePrefix).append("\\----\\\n")
            }
            if (!inlineValue) {
                b.append(labelLine).append('"').append(prefix).append('\"')
                b.append(' '.repeat((2 - prefix.length - strprefix.length).coerceAtLeast(0)))
                b.append(" ] ").append("value=\"").append(value).append("\"\n")
            } else if (below != null) {
                //        b.append(belowPrefix).append('\n');
                below.appendTo(b, belowPrefix.toString(), labelLine + '"'.toString() + prefix + '"'.toString() +
                                                          ' '.repeat(
                                                              maxOf(0, 2 - prefix.length - strprefix.length)) + " ] b=")
                b.append('\n')
            } else {
                b.append(labelLine).append('"').append(prefix).append('\"')
                b.append(' '.repeat(maxOf(0, 2 - prefix.length - strprefix.length)))
                b.append(" ]\n")
            }

            if (right != null) {
                b.append(basePrefix).append("/----/\n")
                right.appendTo(b, siblingPrefix, "$siblingPrefix r=")
                b.append('\n')
                b.append(basePrefix).append("[=====")
            } else {
                b.append(basePrefix).append("     [=====")
            }
        }

        fun fixCount(): Int {
            return ((left?.fixCount() ?: 0) +
                    (below?.fixCount() ?: 0) +
                    (right?.fixCount() ?: 0) +
                    (if (value == null) 0 else 1)).also { count = it }
        }
    }

    interface Entry<T : Any> : MutableMap.MutableEntry<CharSequence, T> {
        val prefix get() = key

        companion object {
            internal operator fun <T : Any> invoke(key: CharSequence, value: T): Entry<T> = EntryImpl(key.toString(),
                                                                                                      value)
        }
    }

    private class EntryImpl<T : Any> constructor(private val node: Node<T>) : Entry<T> {

        override val prefix: String get() = node.prefix

        constructor(key: String, value: T) : this(Node(key, value))

        override val key: String get() = node.prefix

        override fun setValue(newValue: T): T {
            return value.also { value = newValue }
        }

        override var value: T
            get() {
                return node.value as T
            }
            set(value) {
                node.value = value
            }


        override fun toString(): String {
            return "[\"" + node.prefix + "\"->" + node.value + "]"
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + node.prefix.hashCode()
            result = prime * result + if (node.value == null) 0 else node.value!!.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null) {
                return false
            }
            if (javaClass != other.javaClass) {
                return false
            }
            other as EntryImpl<*>

            if (node.prefix != other.node.prefix) {
                return false
            }
            if (node.value == null) {
                if (other.node.value != null) {
                    return false
                }
            } else if (node.value != other.node.value) {
                return false
            }
            return true
        }
    }

    override fun iterator(): MutableIterator<Entry<V>> {
        return root?.let { NodeIterator(it) } ?: emptyList<EntryImpl<V>>().iterator() as MutableIterator<EntryImpl<V>>
    }

    fun toList(): List<Map.Entry<CharSequence, V>> {
        val list = toList(ArrayList(root!!.count), root)
        assert(root == null && list.isEmpty() || list.size == root!!.count)
        return list
    }

    private fun toList(destination: MutableList<EntryImpl<V>>, node: Node<V>?): List<EntryImpl<V>> {
        if (node == null) {
            return destination
        }
        toList(destination, node.left)
        if (node.value != null) {
            destination.add(EntryImpl(node))
        }
        toList(destination, node.below)
        return toList(destination, node.right)
    }

    override val size: Int
        get() = root?.count ?: 0

    override fun clear() {
        root = null
    }

    fun put(prefix: CharSequence, value: V) {
        val cnt = put(Node(prefix.toString(), value))
        assert(cnt == 1)
    }

    private fun put(n: Node<V>): Int {
        val root = this.root
        if (root == null) {
            this.root = n
            return 1
        } else {
            val cmp = prefixCompare(0, root.prefix, n.prefix)
            return root.add(cmp, n)
        }
    }

    override fun add(element: Entry<V>): Boolean {
        put(element.key, element.value)
        return true
    }

    override fun contains(element: Entry<V>): Boolean {
        val root = this.root ?: return false
        if (element !is EntryImpl<*>) {
            return false
        }
        var candidate = getNodeForPrefix(root, element.prefix)
        while (candidate != null && candidate.prefix == element.prefix) {
            if (candidate.value == element.value) {
                return true
            }
            candidate = candidate.below
        }
        return false
    }

    fun containsKey(key: Any): Boolean {
        val root = this.root ?: return false
        if (key !is CharSequence) {
            return false
        }
        val strKey = key.toString()
        return getNodeForPrefix(root, strKey)?.prefix == strKey
    }

    fun containsValue(pKey: Any?): Boolean {
        if (pKey == null) {
            return false
        }
        for (entry in this) {
            if (pKey == entry.value) {
                return true
            }
        }
        return false
    }

    private fun getNodeForPrefix(pNode: Node<V>, prefix: String): Node<V>? {
        val comparison = prefixCompare(pNode.prefix, prefix)
        if (comparison.isEqual) {
            return pNode
        }
        return if (comparison.isLeft) {
            if (pNode.left == null) {
                null
            } else getNodeForPrefix(pNode.left!!, prefix)
        } else if (comparison.isBelow) {
            if (pNode.below == null) {
                null
            } else getNodeForPrefix(pNode.below!!, prefix)
        } else if (comparison.isRight) {
            if (pNode.right == null) {
                null
            } else getNodeForPrefix(pNode.right!!, prefix)
        } else { // above
            pNode
        }
    }

    override fun remove(element: Entry<V>): Boolean {
        val root = root ?: return false

        if (element !is EntryImpl<*>) return false

        return root.remove(CompareResult.BELOW, element)
    }

    override fun removeAll(elements: Collection<Entry<V>>): Boolean {
        return elements.fold(false) { result, entry ->
            result or remove(entry)
        }
    }

    override fun retainAll(elements: Collection<Entry<V>>): Boolean {
        // Implement by creating a new map with items present in both collections,
        // and then swaping the contents to this one
        val tmp = PrefixMap<V>()
        for (o in elements) {
            if (contains(o)) {
                val entry = o as EntryImpl<V>
                tmp.add(entry)
            }
        }
        val result = size > tmp.size
        root = tmp.root
        return result
    }

    /**
     * Get a list of the entries whose prefix starts with the parameter.
     * @param prefix The shared prefix.
     * @return The resulting set of entries.
     */
    fun getPrefixes(prefix: String): Collection<Map.Entry<CharSequence, V>> {
        val newRoot = getPrefixes(root, prefix, 0)
        return PrefixMap<V>().apply { root = newRoot }
    }

    private fun getPrefixes(node: Node<V>?, string: String, offset: Int): Node<V>? {
        if (node == null) return null

        val cmp = prefixCompare(offset, node.prefix, string)
        return when {
            cmp.isEqOrBelow -> {
                // update the children
                node.copy(left = getPrefixes(node.left?.copy(), string, offset),
                          below = getPrefixes(node.below, string, node.prefix.length),
                          right = getPrefixes(node.right?.copy(), string, offset))
            }

            cmp.isLeft      -> getPrefixes(node.left, string, offset)

            cmp.isRight     -> getPrefixes(node.right, string, offset)

            else            -> null // Node prefix is longer than string
        }
    }

    /**
     * Get a collection with all values whose keys start with the given prefix.
     * @param prefix The prefix
     * @return The resulting collection of values.
     */
    fun getPrefixValues(prefix: String): Collection<V> {
        return ValueCollection(getPrefixes(prefix))
    }

    /**
     * Get a collection of all entries longer than or equal to the given prefix
     *
     * @param prefix the prefix
     * @return the collection
     */
    fun getLonger(prefix: String): Collection<Entry<V>> {
        val root = root ?: return PrefixMap()
        val baseNode = getNodeForPrefix(root, prefix) ?: return emptyList()
        val copy = baseNode.copy()

        val left = copy.setLeft(null)
        val right = copy.setRight(null)

        val resultNode: Node<V>
        if (copy.prefix.length == prefix.length) {
            resultNode = copy
        } else {
            resultNode = Node(prefix)
            resultNode.add(CompareResult.BELOW, copy)

            if (left != null) {
                val cmpLeft = prefixCompare(prefix, left.prefix)
                if (cmpLeft.isBelow) {
                    resultNode.add(cmpLeft, left)
                }
            }
            if (right != null) {
                val cmpRight = prefixCompare(prefix, right.prefix)
                if (cmpRight.isBelow) {
                    resultNode.add(cmpRight, right)
                }
            }

        }

        return PrefixMap<V>().apply {
            this.root = Node<V>("").apply {
                add(CompareResult.BELOW, resultNode)
            }
        }
    }

    fun getLongerValues(pPrefix: String): Collection<V> {
        val entryResult = getLonger(pPrefix) as PrefixMap<V>
        return ValueCollection(entryResult)
    }

    /** Get all values with the given key  */
    operator fun get(prefix: String): Collection<Entry<V>> {
        val root = root ?: return PrefixMap()
        val baseNode = getNodeForPrefix(root, prefix)
        if (baseNode == null || baseNode.prefix != prefix) {
            return emptyList()
        }
        val resultNode = baseNode.copy()
        resultNode.setLeft(null)
        resultNode.setRight(null)


        var v: Node<V> = resultNode
        while (v.below != null && v.below!!.prefix == prefix) {
            v.setBelow(v.below!!.copy())
            v.below!!.setLeft(null)
            v.below!!.setRight(null)
            v = v.below!!
        }
        resultNode.fixCount()

        val resultMap = PrefixMap<V>()
        resultMap.put(resultNode)
        return resultMap
    }

    fun toTestString(): String {
        return root!!.toString()
    }

    companion object {

        private val LEFTIDX = 0
        private val BELOWIDX = 1
        private val RIGHTIDX = 2

        private fun prefixCompare(s1: CharSequence, s2: CharSequence): CompareResult {
            return prefixCompare(0, s1.toString(), s2.toString())
        }

        private fun prefixCompare(ignoreOffset: Int, s1: String, s2: String): CompareResult {
            assert(s1.length >= ignoreOffset && s2.length >= ignoreOffset && s2.startsWith(
                s1.subSequence(0, ignoreOffset).toString()))
            if (s2.length < s1.length) {
                return prefixCompare(ignoreOffset, s2, s1).invert()
            }
            for (i in ignoreOffset until s1.length) {
                val c = s1[i]
                val d = s2[i]
                if (d < c) {
                    return if (i == ignoreOffset) CompareResult.LEFT else CompareResult(s1.substring(0, i),
                                                                                        XCompareResult.XLEFT)
                } else if (d > c) {
                    return if (i == ignoreOffset) CompareResult.RIGHT else CompareResult(s1.substring(0, i),
                                                                                         XCompareResult.XRIGHT)
                }
            }
            return if (s1.length == s2.length) {
                CompareResult.EQUAL
            } else {
                CompareResult.BELOW
            }
        }

    }

}
