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

import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.dsl.TestBody
import org.spekframework.spek2.dsl.TestContainer

/**
 * Created by pdvrieze on 15/01/17.
 */
abstract class DelegateTestContainer<D: TestContainer, TESTBODY>(val delegate:D)/*: TestContainer*/ {

  abstract fun testBody(base: TestBody): TESTBODY

//  override fun test(description: String, pending: Pending, body: TestBody.() -> Unit) {
//    delegate.test(description, pending, body)
//  }

//  @JvmName("extTest")
  fun test(description: String, skip: Skip = Skip.No, timeout:Long  = delegate.defaultTimeout, extbody: TESTBODY.() -> Unit) {
    delegate.test(description, skip, timeout, { testBody(this).extbody() })
  }

}
