package nl.adaptivity.process.engine.pma.dynamic.scope.templates

import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.AuthScopeTemplate

class ContextScopeTemplate<AIC: ActivityInstanceContext>(
    private val instantiator: AIC.() -> AuthScope?
): AuthScopeTemplate<AIC> {
    override fun instantiateScope(context: AIC): AuthScope? {
        return context.instantiator()
    }

    companion object {
        inline operator fun <T : AuthScopeTemplate<AIC>, AIC : ActivityInstanceContext> invoke(
            permission: T,
            crossinline instantiator: AIC.(T) -> AuthScope?
        ): ContextScopeTemplate<AIC> {
            return ContextScopeTemplate({ this.instantiator(permission) })
        }
    }
}
