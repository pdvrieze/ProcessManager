package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.DeserializationStrategy
import nl.adaptivity.process.util.Identified

interface OutputRef<T>: ProcessResultRef<T> {
    override val nodeRef: Identified
}

class DelayedOutputRef<T>: OutputRef<T> {
    private var _delegate: OutputRef<T>? = null

    private val delegate: OutputRef<T> get() = _delegate ?: throw NullPointerException("Delayed ouput reference not set")

    override val propertyName: String get() = delegate.propertyName
    override val nodeRef: Identified get() = delegate.nodeRef
    override val serializer: DeserializationStrategy<T> get() = delegate.serializer

    fun set(delegate: OutputRef<T>) {
        check(_delegate == null) { "Delayed ouput reference already set" }
        _delegate = delegate
    }
}
