/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.ws.doclet

import org.testng.Assert
import org.testng.annotations.Test

/**
 * Created by pdvrieze on 11/04/16.
 */

class TestMDGen {
  @Test
  fun testWordWrap() {
    val input = "aa ab ac ad ae af ag ah ai aja ak al, am an ao ap"
    val expected="""
      aa ab ac
      ad ae af
      ag ah ai
      aja ak al,
      am an ao
      ap
    """.trimIndent()
    Assert.assertEquals(input.wordWrap(10).joinToString("\n"), expected)
  }

  @Test(dependsOnMethods = arrayOf("testWordWrap"))
  fun testFlip() {
    val input = arrayOf("aa ab ac ad ae af ag ah ai aj ak al am an ao",
                        "ba bb bc bdx be. bf bg bh bi bj bk bl bm bn bo",
                        "ca cb cc cd ce cf cg ch ci cj ck cl cm cn co cp cq",
                        "da db dc dd de df dg dh di dj dk dl dm dn do")
    val result = input.asSequence().map{it.wordWrap(10)}. flip().map { it.joinToString(" | ") { "${it?:""}${" ".repeat(10-(it?.length?:0))}"} }.joinToString("\n") {it.trimEnd()}

    val expected = """
      aa ab ac   | ba bb bc   | ca cb cc   | da db dc
      ad ae af   | bdx be. bf | cd ce cf   | dd de df
      ag ah ai   | bg bh bi   | cg ch ci   | dg dh di
      aj ak al   | bj bk bl   | cj ck cl   | dj dk dl
      am an ao   | bm bn bo   | cm cn co   | dm dn do
                 |            | cp cq      |
    """.trimIndent().toString()
    Assert.assertEquals(result, expected)
  }
}
