package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.KSerializer

class DataNodeHandleImpl<T>(
    override val id: String,
    override val propertyName: String,
    override val serializer: KSerializer<T>
): DataNodeHandle<T>
