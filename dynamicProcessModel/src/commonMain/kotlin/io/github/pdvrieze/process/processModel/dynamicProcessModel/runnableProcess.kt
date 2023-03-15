@file:OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)

package io.github.pdvrieze.process.processModel.dynamicProcessModel

import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.RootProcessModelBase
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.UUID
import kotlin.contracts.ExperimentalContracts
import kotlin.experimental.ExperimentalTypeInference


fun <AIC : ActivityInstanceContext> runnableProcess(
    name: String,
    owner: PrincipalCompat = SYSTEMPRINCIPAL,
    uuid: UUID = UUID.randomUUID(),
    @ConfigurationDsl
    configureAction: ModelBuilderContext<AIC>.() -> Unit
): ExecutableProcessModel {
    val context = RootModelBuilderContextImpl<AIC>(name, owner, uuid).apply(configureAction)
    return ExecutableProcessModel(context.modelBuilder, true)
}

internal class RootModelBuilderContextImpl<AIC: ActivityInstanceContext>(
    name: String,
    owner: PrincipalCompat,
    uuid: UUID,
) : ModelBuilderContext<AIC>() {
    public override val modelBuilder: RootProcessModel.Builder = RootProcessModelBase.Builder().apply {
        this.name = name
        this.owner = owner
        this.uuid = uuid
    }

    override fun compositeActivityContext(predecessor: Identified): CompositeModelBuilderContext<AIC> {
        return CompositeModelBuilderContextImpl(predecessor, this)
    }
}



