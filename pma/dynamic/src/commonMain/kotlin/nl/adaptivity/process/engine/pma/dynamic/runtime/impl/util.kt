package nl.adaptivity.process.engine.pma.dynamic.runtime.impl

import kotlin.random.Random
import kotlin.random.nextULong


fun Random.nextString() = nextULong().toString(16)
