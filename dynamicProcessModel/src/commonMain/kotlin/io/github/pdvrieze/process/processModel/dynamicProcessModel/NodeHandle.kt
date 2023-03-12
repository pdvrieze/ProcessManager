package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import nl.adaptivity.process.util.Identified

interface NodeHandle<T>: Identified {
    val serializer: KSerializer<T>
}

internal class NonActivityNodeHandleImpl(override val id: String) : NodeHandle<Unit> {
    override val serializer: KSerializer<Unit>
        get() = Unit.serializer()
}
