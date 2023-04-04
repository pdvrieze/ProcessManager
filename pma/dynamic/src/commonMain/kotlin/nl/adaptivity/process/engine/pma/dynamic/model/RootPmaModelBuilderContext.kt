package nl.adaptivity.process.engine.pma.dynamic.model

import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.AbstractDynamicPmaActivityContext
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.RootProcessModelBase
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.UUID

internal class RootPmaModelBuilderContext<AIC : AbstractDynamicPmaActivityContext<AIC, BIC>, BIC: TaskBuilderContext.BrowserContext<AIC, BIC>>(
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
}
