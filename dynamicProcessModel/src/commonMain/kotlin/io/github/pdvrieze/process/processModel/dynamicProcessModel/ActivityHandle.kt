package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.DeserializationStrategy
import nl.adaptivity.process.util.Identified

interface ActivityHandle<T>: Identified, InputRef<T> {
    override val id: String
    override val nodeRef: Identified
        get() = this
    override val propertyName: String
    override val serializer: DeserializationStrategy<T>
}
