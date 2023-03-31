package nl.adaptivity.process.engine.pma.dynamic.scope.templates

import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.AuthScopeTemplate

class ContextScopeTemplate<T: AuthScopeTemplate<AIC>, AIC: ActivityInstanceContext>(
    private val permission: T,
    private val instantiator: AIC.(T) -> AuthScope?
): AuthScopeTemplate<AIC> {
    override fun instantiateScope(context: AIC): AuthScope? {
        return context.instantiator(permission)
    }
}
