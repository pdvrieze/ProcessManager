@file:OptIn(ExperimentalTypeInference::class)

package nl.adaptivity.process.engine.pma.dynamic

import PmaBrowserAction
import PmaServiceAction
import RunnablePmaActivity
import io.github.pdvrieze.process.processModel.dynamicProcessModel.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import net.devrieze.util.collection.replaceBy
import net.devrieze.util.security.SYSTEMPRINCIPAL
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext.BrowserContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAActivityContext
import nl.adaptivity.process.engine.pma.dynamic.scope.templates.DelegateScopeTemplate
import nl.adaptivity.process.engine.pma.models.AuthScopeTemplate
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.IPMAMessageActivity
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.util.Identified
import nl.adaptivity.util.multiplatform.PrincipalCompat
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.xmlutil.Namespace
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KProperty

abstract class PMAModelBuilderContext<AIC : DynamicPMAActivityContext<AIC, BIC>, BIC: BrowserContext<AIC, BIC>> : IModelBuilderContext<AIC> {

    operator fun <I: Any, O: Any> RunnablePmaActivity.Builder<I, O, *>.provideDelegate(
        thisRef: Nothing?,
        property: KProperty<*>
    ): ActivityHandle<O> {
        val nodeBuilder = this
        if (id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) id = property.name
        with(modelBuilder) {
            nodes.add(nodeBuilder.ensureId())
        }
        val outputName = results.singleOrNull()?.name ?: ""
        return ActivityHandleImpl(id!!, outputName, this.outputSerializer as KSerializer<O>)
    }

    @PublishedApi
    internal abstract fun compositeActivityContext(predecessor: Identified): CompositePMAModelBuilderContext<AIC, BIC>

    // TODO taskActivities should not provide the inptus until the task has been accepted
    inline fun  <I : Any, reified O : Any> taskActivity(
        predecessor: NodeHandle<I>,
        permissions: List<AuthScopeTemplate<AIC>> = emptyList(),
        accessRestrictions: RunnableAccessRestriction? = null,
        refNode: Identified? = predecessor.identifier,
        refName: String? = "",
        inputSerializer: DeserializationStrategy<I> = predecessor.serializer,
        @BuilderInference
        noinline action: TaskBuilderContext<AIC, BIC, I>.() -> TaskBuilderContext.AcceptedTask<AIC, BIC, I, O>
    ): RunnablePmaActivity.Builder<I, O, AIC> {
        return taskActivity(
            predecessor, permissions, accessRestrictions, input = InputRefImpl(refNode, refName?:"", inputSerializer), action = action
        )
    }

    inline fun  <I : Any, reified O : Any> taskActivity(
        predecessor: NodeHandle<*>,
        authorizationTemplates: List<AuthScopeTemplate<AIC>> = emptyList(),
        accessRestrictions: RunnableAccessRestriction? = null,
        input: DefineInputCombiner<I>,
        @BuilderInference
        noinline action: TaskBuilderContext<AIC, BIC, I>.() -> TaskBuilderContext.AcceptedTask<AIC, BIC, I, O>
    ): RunnablePmaActivity.Builder<I, O, AIC> {
        return RunnablePmaActivity.Builder<I, O, AIC>(
            predecessor = predecessor,
            inputCombiner = input.combiner,
            outputSerializer = serializer<O>(),
            action = taskListAction<AIC, BIC, I, O>(action),
            accessRestrictions = accessRestrictions,
            authorizationTemplates = authorizationTemplates
        ).apply {
            defines.replaceBy(input.defines)
        }
    }

    inline fun  <I : Any, reified O : Any> taskActivity(
        predecessor: NodeHandle<*>,
        authorizationTemplates: List<AuthScopeTemplate<AIC>> = emptyList(),
        accessRestrictions: RunnableAccessRestriction? = null,
        input: InputRef<I>,
        @BuilderInference
        noinline action: TaskBuilderContext<AIC, BIC, I>.() -> TaskBuilderContext.AcceptedTask<AIC, BIC, I, O>
    ): RunnablePmaActivity.Builder<I, O, AIC> {
        return RunnablePmaActivity.Builder<I, O, AIC>(
            predecessor = predecessor,
            refNode = input.nodeRef,
            refName = input.propertyName,
            inputSerializer = input.serializer,
            outputSerializer = serializer<O>(),
            accessRestrictions = accessRestrictions,
            authorizationTemplates = authorizationTemplates,
            action = taskListAction(action),
        )
    }

    inline fun  <I : Any, reified O : Any, S: AutomatedService> serviceActivity(
        predecessor: NodeHandle<*>,
        authorizationTemplates: List<AuthScopeTemplate<AIC>> = emptyList(),
        service: ServiceName<S>,
        input: DefineInputCombiner<I>,
        @BuilderInference
        noinline action: RunnableAction<I, O, ServiceActivityContext<AIC, S>>
    ): RunnablePmaActivity.Builder<I, O, AIC> {
        return RunnablePmaActivity.Builder<I, O, AIC>(
            predecessor = predecessor,
            inputCombiner = input.combiner,
            outputSerializer = serializer<O>(),
            authorizationTemplates = authorizationTemplates,
            action = serviceAction(service, action)
        ).apply {
            defines.replaceBy(input.defines)
        }
    }

    inline fun  <I : Any, reified O : Any, S: AutomatedService> serviceActivity(
        predecessor: NodeHandle<*>,
        authorizationTemplates: List<AuthScopeTemplate<AIC>> = emptyList(),
        service: ServiceName<S>,
        input: InputRef<I>,
        @BuilderInference
        noinline action: RunnableAction<I, O, ServiceActivityContext<AIC, S>>
    ): RunnablePmaActivity.Builder<I, O, AIC> {
        return RunnablePmaActivity.Builder<I, O, AIC>(
            predecessor = predecessor,
            refNode = input.nodeRef,
            refName = input.propertyName,
            inputSerializer = input.serializer,
            outputSerializer = serializer<O>(),
            authorizationTemplates = authorizationTemplates,
            action = serviceAction(service, action)
        )
    }

    fun <AIC: DynamicPMAActivityContext<AIC, *>> delegatePermissions(targetService: ServiceName<*>, vararg permissions: AuthScopeTemplate<AIC>): AuthScopeTemplate<AIC> {
        return DelegateScopeTemplate(targetService, permissions)
    }
}

@PublishedApi
internal fun <AIC : DynamicPMAActivityContext<AIC, BIC>, BIC : BrowserContext<AIC, BIC>, I : Any, O : Any>
    taskListAction(action: TaskBuilderContext<AIC, BIC, I>.() -> TaskBuilderContext.AcceptedTask<AIC, BIC, I, O>): PmaBrowserAction<I, O, AIC, BIC> {
    return PmaBrowserAction(TaskBuilderContext<AIC, BIC, I>().action())
}

@PublishedApi
internal fun <AIC: DynamicPMAActivityContext<AIC, *>, I: Any, O: Any, S: AutomatedService> serviceAction(
    serviceId: ServiceName<S>,
    action: RunnableAction<I, O, ServiceActivityContext<AIC, S>>
): PmaServiceAction<I, O, AIC, S> {
    return PmaServiceAction(serviceId) { input ->
        val service: S = processContext.contextFactory.resolveService(serviceId)
        val scope = (node as IPMAMessageActivity<AIC>).authorizationTemplates
            .mapNotNull { it.instantiateScope(this) }
            .reduce { left, right -> left.union(right)}

        val engineService = processContext.engineService
        val authToken = engineService.createAuthToken(
            engineService.serviceInstanceId.serviceId, nodeInstanceHandle, service, scope,
        )

        val serviceContext = ServiceActivityContext(this, service, authToken)
        serviceContext.action(input)
    }
}

abstract class CompositePMAModelBuilderContext<AIC : DynamicPMAActivityContext<AIC, BIC>, BIC: BrowserContext<AIC, BIC>> : PMAModelBuilderContext<AIC, BIC>(), ICompositeModelBuilderContext<AIC> {
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
        modelBuilder.imports.add(XmlResultType(name, "/$name/*"))
        return InputRefImpl(name, deserializer)
    }

}

internal class CompositePMAModelBuilderContextImpl<AIC : DynamicPMAActivityContext<AIC, BIC>, BIC: BrowserContext<AIC, BIC>>(
    predecessor: Identified,
    private val owner: RootPMAModelBuilderContext<AIC, BIC>,
) : CompositePMAModelBuilderContext<AIC, BIC>() {
    override val modelBuilder = ActivityBase.CompositeActivityBuilder(owner.modelBuilder).apply {
        this.predecessor = predecessor
    }

    override fun compositeActivityContext(predecessor: Identified): CompositePMAModelBuilderContext<AIC, BIC> {
        return CompositePMAModelBuilderContextImpl(predecessor, owner)
    }

}


fun <AIC : DynamicPMAActivityContext<AIC, BIC>, BIC: BrowserContext<AIC, BIC>> runnablePmaProcess(
    name: String,
    owner: PrincipalCompat = SYSTEMPRINCIPAL,
    uuid: UUID = UUID.randomUUID(),
    @ConfigurationDsl
    configureAction: PMAModelBuilderContext<AIC, BIC>.() -> Unit
): ExecutableProcessModel {
    val context = RootPMAModelBuilderContext<AIC, BIC>(name, owner, uuid).apply(configureAction)
    return ExecutableProcessModel(context.modelBuilder, true)
}

internal class RootPMAModelBuilderContext<AIC : DynamicPMAActivityContext<AIC, BIC>, BIC: BrowserContext<AIC, BIC>>(
    name: String,
    owner: PrincipalCompat,
    uuid: UUID,
) : PMAModelBuilderContext<AIC, BIC>() {
    public override val modelBuilder: RootProcessModel.Builder = RootProcessModelBase.Builder().apply {
        this.name = name
        this.owner = owner
        this.uuid = uuid
    }

    override fun compositeActivityContext(predecessor: Identified): CompositePMAModelBuilderContext<AIC, BIC> {
        return CompositePMAModelBuilderContextImpl(predecessor, this)
    }
}


@OptIn(ExperimentalContracts::class)
inline fun <AIC : DynamicPMAActivityContext<AIC, BIC>, BIC : BrowserContext<AIC, BIC>> PMAModelBuilderContext<AIC, BIC>.compositeActivity(
    predecessor: Identified,
    @ConfigurationDsl configure: CompositePMAModelBuilderContext<AIC, BIC>.() -> Unit
): ActivityBase.CompositeActivityBuilder {
    contract {
        callsInPlace(configure, InvocationKind.EXACTLY_ONCE)
    }
    val context = compositeActivityContext(predecessor).apply(configure)
    return context.modelBuilder
}
