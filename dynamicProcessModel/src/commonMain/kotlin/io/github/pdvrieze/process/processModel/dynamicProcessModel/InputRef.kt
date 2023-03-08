package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.DeserializationStrategy
import nl.adaptivity.process.util.Identified

interface InputRef<T> {
    val nodeRef: Identified?
    val propertyName: String
    val serializer: DeserializationStrategy<T>
}
