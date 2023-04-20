package nl.adaptivity.process.engine.pma.dynamic.model

import io.github.pdvrieze.process.processModel.dynamicProcessModel.DefineHolder
import io.github.pdvrieze.process.processModel.dynamicProcessModel.InputRef
import io.github.pdvrieze.process.processModel.dynamicProcessModel.InputRefImpl
import io.github.pdvrieze.process.processModel.dynamicProcessModel.RunnableActivity
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.serializer
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.RootProcessModelBase
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.UUID

class RootPmaModelBuilderContext<AIC : DynamicPmaActivityContext<AIC, BIC>, BIC: TaskBuilderContext.BrowserContext<AIC, BIC>>(
    name: String,
    owner: PrincipalCompat,
    uuid: UUID,
) : PmaModelBuilderContext<AIC, BIC>() {
    public override val modelBuilder: RootProcessModel.Builder = RootProcessModelBase.Builder().apply {
        this.name = name
        this.owner = owner
        this.uuid = uuid
    }

    override fun compositeActivityContext(predecessor: Identified): CompositePmaModelBuilderContext<AIC, BIC> {
        return CompositePmaModelBuilderContextImpl(predecessor, this)
    }

    inline fun <reified T: Any> input(name: String, path: String? = null): InputRef<T> {
        return input(name, serializer<T>(), path)
    }

    fun <T> input(
        name: String,
        deserializer: DeserializationStrategy<T>,
        path: String? = null,
    ): InputRef<T> {
        modelBuilder.imports.add(XmlResultType(name, path))
        return InputRefImpl(name, deserializer)
    }

    override fun <T> InputRef<T>.named(name: String): DefineHolder<T> {
        val defineType = RunnableActivity.DefineType(name, nodeRef, propertyName, null, serializer)
        return DefineHolder(defineType)
    }

}
