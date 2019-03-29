/*
 * Copyright (c) 2019.
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

import org.spekframework.spek2.dsl.GroupBody
import org.spekframework.spek2.dsl.Root
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.dsl.TestBody
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.lifecycle.LifecycleListener
import org.spekframework.spek2.lifecycle.MemoizedValue
import org.spekframework.spek2.style.specification.Suite

/**
 * Spek root object that actually delegates to a suite instead. This allows for one test to be included into another
 */
class InclusionRoot(val group: GroupBody): Root {

    constructor(suite: Suite): this(suite.delegate)

    override val defaultCachingMode: CachingMode
        get() = group.defaultCachingMode

    override fun afterEachTest(callback: () -> Unit) {
        group.afterEachTest(callback)
    }

    override fun afterGroup(callback: () -> Unit) {
        group.afterGroup(callback)
    }

    override fun beforeEachTest(callback: () -> Unit) {
        group.beforeEachTest(callback)
    }

    override fun beforeGroup(callback: () -> Unit) {
        group.beforeGroup(callback)
    }

    override fun group(description: String,
                       skip: Skip,
                       defaultCachingMode: CachingMode,
                       preserveExecutionOrder: Boolean,
                       body: GroupBody.() -> Unit) {
        group.group(description, skip, defaultCachingMode, preserveExecutionOrder, body)
    }

    override fun <T> memoized(mode: CachingMode, factory: () -> T): MemoizedValue<T> {
        return group.memoized(mode, factory)
    }

    override fun <T> memoized(mode: CachingMode, factory: () -> T, destructor: (T) -> Unit): MemoizedValue<T> {
        return group.memoized(mode, factory, destructor)
    }

    override fun <T> memoized(): MemoizedValue<T> {
        return group.memoized()
    }

    override fun registerListener(listener: LifecycleListener) {
        throw UnsupportedOperationException("Included tests cannot register listeners")
    }

    override fun test(description: String, skip: Skip, body: TestBody.() -> Unit) {
        return group.test(description, skip, body)
    }
}
