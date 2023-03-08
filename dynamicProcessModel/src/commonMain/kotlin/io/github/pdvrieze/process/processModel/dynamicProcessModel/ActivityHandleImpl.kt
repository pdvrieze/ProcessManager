package io.github.pdvrieze.process.processModel.dynamicProcessModel

import kotlinx.serialization.KSerializer

internal class ActivityHandleImpl<T>(
    override val id: String,
    override val propertyName: String,
    override val serializer: KSerializer<T>
): ActivityHandle<T>
