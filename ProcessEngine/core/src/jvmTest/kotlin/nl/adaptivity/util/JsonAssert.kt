/*
 * Copyright (c) 2021.
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

package nl.adaptivity.util

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.assertEquals

fun assertJsonEquals(expected: String, actual: String, message: String? = null) {
    val expectedElement = Json.parseToJsonElement(expected)
    val actualElement = Json.parseToJsonElement(actual)

    assertJsonEquals(expectedElement, actualElement, message)
}

fun assertJsonEquals(expected: JsonElement, actual: JsonElement, message: String? = null, path: String = "/") {
    when (expected) {
        is JsonPrimitive -> when (actual) {
            is JsonPrimitive -> assertEquals(expected.content, actual.content, message.withPath(path))
            else             -> assertEquals(expected, actual, message.withPath(path))
        }
        is JsonObject    -> when (actual) {
            is JsonObject -> {
                val expectedEntries = expected.entries.sortedBy { it.key }
                val actualEntries = actual.entries.sortedBy { it.key }
                if (expectedEntries.size != actualEntries.size) {
                    assertEquals(
                        expectedEntries.joinToString { it.key },
                        actualEntries.joinToString { it.key },
                        message.withPath(path)
                    )
                }
                val actualIt = actualEntries.iterator()
                for (expectedEntry in expectedEntries) {
                    val actualEntry = actualIt.next()
                    assertEquals(expectedEntry.key, actualEntry.key, message.withPath(path))
                    assertJsonEquals(expectedEntry.value, actualEntry.value, message, path+expectedEntry.key+"/")
                }
            }
            else          -> assertEquals(expected, actual, message)
        }
        is JsonArray     -> when (actual) {
            is JsonArray -> {
                assertEquals(expected.size, actual.size)
                for (i in expected.indices) {
                    val expectedElement = expected[i]
                    val actualElement = actual[i]
                    assertJsonEquals(expectedElement, actualElement, message, path+"[$i]/")
                }
            }
            else         -> assertEquals(expected, actual, message.withPath(path))
        }
    }
}

private fun String?.withPath(path: String) = when (this) {
    null -> "Path: $path"
    else -> "Path: $path – $this"
}
