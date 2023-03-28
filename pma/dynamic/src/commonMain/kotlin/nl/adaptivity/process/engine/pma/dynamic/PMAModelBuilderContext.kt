@file:OptIn(ExperimentalTypeInference::class)

package nl.adaptivity.process.engine.pma.dynamic

import io.github.pdvrieze.process.processModel.dynamicProcessModel.*
import kotlinx.serialization.serializer
import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.process.engine.pma.dynamic.scope.templates.DelegateScopeTemplate
import nl.adaptivity.process.engine.pma.models.AuthScopeTemplate
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.runtime.PMAActivityContext
import nl.adaptivity.process.processModel.ActivityBase
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.process.processModel.RootProcessModelBase
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.UUID
import kotlin.experimental.ExperimentalTypeInference

abstract class PMAModelBuilderContext<AIC : PMAActivityContext<AIC>> : IModelBuilderContext<AIC> {

    protected abstract fun compositeActivityContext(predecessor: Identified): CompositePMAModelBuilderContext<AIC>

    inline fun  <I : Any, reified O : Any> taskActivity(
        predecessor: NodeHandle<I>,
        permissions: List<AuthScopeTemplate<AIC>> = emptyList(),
        accessRestrictions: RunnableAccessRestriction? = null,
        @BuilderInference
        noinline action: RunnableAction<I, O, AIC>
    ): RunnableActivity.Builder<I, O, AIC> {
        return RunnableActivity.Builder(
            predecessor,
            predecessor.identifier,
            "",
            predecessor.serializer,
            serializer(),
            action
        ).apply {
            this.accessRestrictions = accessRestrictions
        }
    }

    fun delegatePermissions(targetService: ServiceId, vararg permissions: AuthScopeTemplate<AIC>): AuthScopeTemplate<AIC> {
        return DelegateScopeTemplate(targetService, permissions)

    }
}

abstract class CompositePMAModelBuilderContext<AIC : PMAActivityContext<AIC>> : PMAModelBuilderContext<AIC>() {

}

internal class CompositePMAModelBuilderContextImpl<AIC : PMAActivityContext<AIC>>(
    predecessor: Identified,
    private val owner: RootPMAModelBuilderContext<AIC>,
) : CompositePMAModelBuilderContext<AIC>() {
    override val modelBuilder = ActivityBase.CompositeActivityBuilder(owner.modelBuilder).apply {
        this.predecessor = predecessor
    }

    override fun compositeActivityContext(predecessor: Identified): CompositePMAModelBuilderContext<AIC> {
        return CompositePMAModelBuilderContextImpl(predecessor, owner)
    }

}


fun <AIC : PMAActivityContext<AIC>> runnablePmaProcess(
    name: String,
    owner: PrincipalCompat = SYSTEMPRINCIPAL,
    uuid: UUID = UUID.randomUUID(),
    @ConfigurationDsl
    configureAction: PMAModelBuilderContext<AIC>.() -> Unit
): ExecutableProcessModel {
    val context = RootPMAModelBuilderContext<AIC>(name, owner, uuid).apply(configureAction)
    return ExecutableProcessModel(context.modelBuilder, true)
}

internal class RootPMAModelBuilderContext<AIC : PMAActivityContext<AIC>>(
    name: String,
    owner: PrincipalCompat,
    uuid: UUID,
) : PMAModelBuilderContext<AIC>() {
    public override val modelBuilder: RootProcessModel.Builder = RootProcessModelBase.Builder().apply {
        this.name = name
        this.owner = owner
        this.uuid = uuid
    }

    override fun compositeActivityContext(predecessor: Identified): CompositePMAModelBuilderContext<AIC> {
        return CompositePMAModelBuilderContextImpl(predecessor, this)
    }
}
