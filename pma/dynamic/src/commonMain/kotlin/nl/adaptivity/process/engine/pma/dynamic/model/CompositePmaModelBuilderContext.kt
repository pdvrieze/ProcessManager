package nl.adaptivity.process.engine.pma.dynamic.model

import io.github.pdvrieze.process.processModel.dynamicProcessModel.ICompositeModelBuilderContext
import io.github.pdvrieze.process.processModel.dynamicProcessModel.InputRef
import io.github.pdvrieze.process.processModel.dynamicProcessModel.InputRefImpl
import kotlinx.serialization.DeserializationStrategy
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.process.processModel.ActivityBase
import nl.adaptivity.process.processModel.XmlDefineType
import nl.adaptivity.process.processModel.XmlResultType
import nl.adaptivity.process.util.Identified
import nl.adaptivity.xmlutil.Namespace

abstract class CompositePmaModelBuilderContext<
    AIC : DynamicPmaActivityContext<AIC, BIC>,
    BIC: TaskBuilderContext.BrowserContext<AIC, BIC>
    > : PmaModelBuilderContext<AIC, BIC>(), ICompositeModelBuilderContext<AIC> {

    abstract override val modelBuilder: ActivityBase.CompositeActivityBuilder


    override fun <T> input(
        name: String,
        refNode: Identified,
        refName: String?,
        path: String?,
        content: CharArray?,
        nsContext: Iterable<Namespace>,
        deserializer: DeserializationStrategy<T>,
    ): InputRef<T> {
        modelBuilder.defines.add(XmlDefineType(name, refNode, refName, path, content, nsContext))
        modelBuilder.imports.add(XmlResultType(name, "/$name/node()"))
        return InputRefImpl(name, deserializer)
    }

}
