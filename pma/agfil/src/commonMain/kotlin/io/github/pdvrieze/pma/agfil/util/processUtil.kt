package io.github.pdvrieze.pma.agfil.util

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.serializer
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.xmlutil.serialization.XML

inline fun <reified T: Any> ProcessData.get(): T = get(serializer<T>())

fun <T: Any> ProcessData.get(deserializer: DeserializationStrategy<T>): T {
    return contentStream.use { XML.decodeFromReader(deserializer, it) }
}
