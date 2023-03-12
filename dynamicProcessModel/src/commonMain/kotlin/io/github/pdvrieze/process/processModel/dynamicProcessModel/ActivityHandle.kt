package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.KSerializer
import nl.adaptivity.process.util.Identified

interface ActivityHandle<T>: NodeHandle<T>, InputRef<T> {
    override val id: String
    override val nodeRef: Identified
        get() = this
    override val propertyName: String
    override val serializer: KSerializer<T>
}
