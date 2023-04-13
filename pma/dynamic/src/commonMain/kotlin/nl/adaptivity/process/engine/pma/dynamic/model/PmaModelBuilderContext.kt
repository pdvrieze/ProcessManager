package nl.adaptivity.process.engine.pma.dynamic.model

import RunnablePmaActivity
import io.github.pdvrieze.process.processModel.dynamicProcessModel.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import net.devrieze.util.collection.replaceBy
import nl.adaptivity.process.engine.pma.dynamic.ServiceActivityContext
import nl.adaptivity.process.engine.pma.dynamic.TaskBuilderContext
import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.scope.templates.DelegateScopeTemplate
import nl.adaptivity.process.engine.pma.models.AuthScopeTemplate
import nl.adaptivity.process.engine.pma.models.AutomatedService
import nl.adaptivity.process.engine.pma.models.IPMAMessageActivity
import nl.adaptivity.process.engine.pma.models.ServiceName
import nl.adaptivity.process.processModel.ActivityBase
import nl.adaptivity.process.processModel.configurableModel.ConfigurationDsl
import nl.adaptivity.process.processModel.name
import nl.adaptivity.process.util.Identified
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KProperty

@OptIn(ExperimentalTypeInference::class)
abstract class PmaModelBuilderContext<
    AIC : DynamicPmaActivityContext<AIC, BIC>,
    BIC : TaskBuilderContext.BrowserContext<AIC, BIC>
    > : IModelBuilderContext<AIC> {

    operator fun <I : Any, O : Any> RunnablePmaActivity.Builder<I, O, *>.provideDelegate(
        thisRef: Nothing?,
        property: KProperty<*>
    ): DataNodeHandle<O> {
        val nodeBuilder = this
        if (id == null && modelBuilder.nodes.firstOrNull { it.id == property.name } == null) id = property.name
        with(modelBuilder) {
            nodes.add(nodeBuilder.ensureId())
        }
        val outputName = results.singleOrNull()?.name ?: ""
        return DataNodeHandleImpl(id!!, outputName, this.outputSerializer as KSerializer<O>)
    }

    @PublishedApi
    internal abstract fun compositeActivityContext(predecessor: Identified): CompositePmaModelBuilderContext<AIC, BIC>

    // TODO taskActivities should not provide the inptus until the task has been accepted
    inline fun <I : Any, reified O : Any> taskActivity(
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
            predecessor,
            permissions,
            accessRestrictions,
            input = InputRefImpl(refNode, refName ?: "", inputSerializer),
            action = action
        )
    }

    inline fun <I : Any, reified O : Any> taskActivity(
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

    inline fun <I : Any, reified O : Any> taskActivity(
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

    inline fun <I : Any, reified O : Any, S : AutomatedService> serviceActivity(
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

    inline fun <I : Any, reified O : Any, S : AutomatedService> serviceActivity(
        predecessor: NodeHandle<I>,
        authorizationTemplates: List<AuthScopeTemplate<AIC>> = emptyList(),
        service: ServiceName<S>,
        @BuilderInference
        noinline action: RunnableAction<I, O, ServiceActivityContext<AIC, S>>
    ): RunnablePmaActivity.Builder<I, O, AIC> {
        return serviceActivity(predecessor, authorizationTemplates, service, InputRefImpl(predecessor.identifier, "", predecessor.serializer), action)
    }

    inline fun <I : Any, reified O : Any, S : AutomatedService> serviceActivity(
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

    @OptIn(ExperimentalContracts::class)
    inline fun compositeActivity(
        predecessor: Identified,
        @ConfigurationDsl configure: CompositePmaModelBuilderContext<AIC, BIC>.() -> Unit
    ): ActivityBase.CompositeActivityBuilder {
        contract {
            callsInPlace(configure, InvocationKind.EXACTLY_ONCE)
        }
        val context = compositeActivityContext(predecessor).apply(configure)
        return context.modelBuilder
    }

    fun <AIC : DynamicPmaActivityContext<AIC, *>> delegatePermissions(
        targetService: ServiceName<*>,
        vararg permissions: AuthScopeTemplate<AIC>
    ): AuthScopeTemplate<AIC> {
        return DelegateScopeTemplate(targetService, permissions)
    }

    companion object {
        @PublishedApi
        internal fun <AIC : DynamicPmaActivityContext<AIC, *>, I : Any, O : Any, S : AutomatedService> serviceAction(
            serviceId: ServiceName<S>,
            action: RunnableAction<I, O, ServiceActivityContext<AIC, S>>
        ): PmaAction.ServiceAction<I, O, AIC, S> {
            return PmaAction.ServiceAction<I, O, AIC, S>(serviceId) { input: I ->
                val service: S = processContext.contextFactory.resolveService(serviceId)
                val scope = (node as IPMAMessageActivity<AIC>).authorizationTemplates
                    .mapNotNull { it.instantiateScope(this) }
                    .reduce { left, right -> left.union(right) }

                val engineService = processContext.engineService
                val authToken = engineService.createAuthToken(
                    engineService.serviceInstanceId.serviceId, nodeInstanceHandle, service, scope,
                )

                val serviceContext = ServiceActivityContext(this, service, authToken)
                serviceContext.action(input)
            }
        }

        @PublishedApi
        internal fun <AIC : DynamicPmaActivityContext<AIC, BIC>, BIC : TaskBuilderContext.BrowserContext<AIC, BIC>, I : Any, O : Any>
            taskListAction(action: TaskBuilderContext<AIC, BIC, I>.() -> TaskBuilderContext.AcceptedTask<AIC, BIC, I, O>): PmaAction.BrowserAction<I, O, AIC, BIC> {
            return PmaAction.BrowserAction<I, O, AIC, BIC>(TaskBuilderContext<AIC, BIC, I>().action())
        }

    }
}
