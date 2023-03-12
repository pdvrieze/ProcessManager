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


fun <C : ActivityInstanceContext> runnableProcess(
    name: String,
    owner: PrincipalCompat = SYSTEMPRINCIPAL,
    uuid: UUID = UUID.randomUUID(),
    @ConfigurationDsl
    configureAction: ModelBuilderContext<C>.() -> Unit
): ExecutableProcessModel {
    val context = RootModelBuilderContextImpl<C>(name, owner, uuid).apply(configureAction)
    return ExecutableProcessModel(context.modelBuilder, true)
}

internal class RootModelBuilderContextImpl<C: ActivityInstanceContext>(
    name: String,
    owner: PrincipalCompat,
    uuid: UUID,
) : ModelBuilderContext<C>() {
    public override val modelBuilder: RootProcessModel.Builder = RootProcessModelBase.Builder().apply {
        this.name = name
        this.owner = owner
        this.uuid = uuid
    }

    override fun compositeActivityContext(predecessor: Identified): CompositeModelBuilderContext<C> {
        return CompositeModelBuilderContextImpl(predecessor, this)
    }
}



