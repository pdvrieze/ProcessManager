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

import nl.adaptivity.process.engine.ProcessTestingDslMarker
import org.spekframework.spek2.dsl.Skip

fun <SPECBODY> DelegateSpecBody<SPECBODY, *, *, *>.describe(description: String, body: SPECBODY.() -> Unit) {
  group("describe $description", extbody = body)
}

fun <SPECBODY> DelegateSpecBody<SPECBODY, *, *, *>.context(description: String, body: SPECBODY.() -> Unit) {
  group("context $description", extbody = body)
}

fun <SPECBODY> DelegateSpecBody<SPECBODY, *, *, *>.given(description: String, body: SPECBODY.() -> Unit) {
  group("given $description", extbody = body)
}

fun <ACTIONBODY> DelegateSpecBody<*, ACTIONBODY, *, *>.on(description: String, body: ACTIONBODY.() -> Unit) {
  action("on $description", extbody = body)
}

fun <TESTBODY> DelegateTestContainer<*, TESTBODY>.it(description: String, body: TESTBODY.() -> Unit) {
  test("it $description", extbody = body)
}

fun <SPECBODY> DelegateSpecBody<SPECBODY, *, *, *>.xdescribe(description: String, reason: String? = null, body: SPECBODY.() -> Unit) {
  group("describe $description", Skip.Yes(reason), extbody = body)
}

fun <SPECBODY> DelegateSpecBody<SPECBODY, *, *, *>.xcontext(description: String, reason: String? = null, body: SPECBODY.() -> Unit) {
  group("context $description", Skip.Yes(reason), extbody = body)
}

fun <SPECBODY> DelegateSpecBody<SPECBODY, *, *, *>.xgiven(description: String, reason: String? = null, body: SPECBODY.() -> Unit) {
  group("given $description", Skip.Yes(reason), extbody = body)
}

fun <ACTIONBODY> DelegateSpecBody<*, ACTIONBODY, *, *>.xon(description: String, reason: String? = null, body: ACTIONBODY.() -> Unit = {}) {
  action("on $description", Skip.Yes(reason), extbody = body)
}

fun <TESTBODY> DelegateTestContainer<*, TESTBODY>.xit(description: String, reason: String? = null, body: TESTBODY.() -> Unit = {}) {
  test("it $description", Skip.Yes(reason), extbody = body)
}

@ProcessTestingDslMarker
inline fun <SPECBODY, R> DelegateSpecBody<SPECBODY, *, *, *>.rgroup(description: String, skip: Skip = Skip.No, noinline body: SPECBODY.() -> R):R {
  var result: R? = null
  group(description, skip, extbody = { result = body() })
  return result!!
}
