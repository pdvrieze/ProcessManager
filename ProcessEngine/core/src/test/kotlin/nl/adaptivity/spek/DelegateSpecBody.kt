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

package nl.adaptivity.spek

import org.spekframework.spek2.dsl.GroupBody
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.dsl.TestBody
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.lifecycle.MemoizedValue

abstract class DelegateSpecBody<out SPECBODY, out ACTIONBODY, TESTBODY, FIXTUREBODY: Any>(delegate: GroupBody): DelegateTestContainer<GroupBody, TESTBODY>(delegate)/*, LifecycleAware*/ {
  abstract fun actionBody(base: TestBody):ACTIONBODY
  abstract fun specBody(base: GroupBody):SPECBODY
  abstract fun otherBody():FIXTUREBODY

//  override fun action(description: String, pending: Pending, body: ActionBody.() -> Unit) {
//    delegate.action(description, pending, body)
//  }

//  @JvmName("extAction")
  fun action(description: String, skip: Skip = Skip.No, extbody: ACTIONBODY.() -> Unit) {
    delegate.test(description, skip) {actionBody(this).extbody()}
}

//  override fun group(description: String, pending: Pending, body: SpecBody.() -> Unit) {
//    delegate.group(description, pending, body)
//  }

//  @JvmName("ExtGroup")
  fun group(description: String, skip: Skip = Skip.No, extbody: SPECBODY.() -> Unit) {
    delegate.group(description, skip) {specBody(this).extbody()}
}

//  override fun afterEachTest(callback: () -> Unit) = delegate.afterEachTest(callback)

  fun afterEachTest(callback: FIXTUREBODY.() -> Unit) {
    delegate.afterEachTest { otherBody().callback() }
  }

//  override fun afterGroup(callback: () -> Unit) = delegate.afterGroup(callback)

  fun afterGroup(callback: FIXTUREBODY.() -> Unit) {
    delegate.afterGroup { otherBody().callback() }
  }

//  override fun beforeEachTest(callback: () -> Unit) {
//    delegate.beforeEachTest(callback)
//  }

  fun beforeEachTest(callback: FIXTUREBODY.() -> Unit) {
    delegate.beforeEachTest { otherBody().callback() }
  }

//  override fun beforeGroup(callback: () -> Unit) {
//    delegate.beforeGroup(callback)
//  }

  fun beforeGroup(callback: FIXTUREBODY.() -> Unit) {
    delegate.beforeGroup { otherBody().callback() }
  }

  fun <T> memoized(mode: CachingMode = delegate.defaultCachingMode,
                   factory: () -> T,
                   destructor: (T) -> Unit): MemoizedValue<T> {
    return delegate.memoized(mode, factory, destructor)
  }

  fun <T> memoized(mode: CachingMode = delegate.defaultCachingMode,
                   factory: () -> T): MemoizedValue<T> {
    return delegate.memoized(mode, factory)
  }
}
