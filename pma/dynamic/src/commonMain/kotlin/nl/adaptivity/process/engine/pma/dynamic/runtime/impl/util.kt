package nl.adaptivity.process.engine.pma.dynamic.runtime.impl

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import net.devrieze.util.Tripple
import nl.adaptivity.process.engine.impl.CompactFragment
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.smartStartTag
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.random.Random
import kotlin.random.nextULong


fun Random.nextString() = nextULong().toString(16)
