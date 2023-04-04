package nl.adaptivity.process.engine.pma.dynamic.model

import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.processModel.ActivityBase
import nl.adaptivity.process.util.Identified

internal class CompositePmaModelBuilderContextImpl<AIC : AbstractDynamicPmaActivityContext<AIC, BIC>, BIC: TaskBuilderContext.BrowserContext<AIC, BIC>>(
    predecessor: Identified,
    private val owner: RootPmaModelBuilderContext<AIC, BIC>,
) : CompositePmaModelBuilderContext<AIC, BIC>() {
    override val modelBuilder = ActivityBase.CompositeActivityBuilder(owner.modelBuilder).apply {
        this.predecessor = predecessor
    }

    override fun compositeActivityContext(predecessor: Identified): CompositePmaModelBuilderContext<AIC, BIC> {
        return CompositePmaModelBuilderContextImpl(predecessor, owner)
    }

}
