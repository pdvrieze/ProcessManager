package nl.adaptivity.process.engine.pma.dynamic.scope.templates

import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.pma.dynamic.scope.DelegatePermissionScope
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.AuthScopeTemplate
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.util.kotlin.arrayMap

class DelegateScopeTemplate<AIC: ActivityInstanceContext>(val targetService: ServiceId, val scopeTemplates: Array<out AuthScopeTemplate<AIC>>) :
    AuthScopeTemplate<AIC> {
    override fun instantiateScope(context: AIC): AuthScope {
        val scopes = scopeTemplates.arrayMap { it.instantiateScope(context) }
        return DelegatePermissionScope(targetService, scopes)
    }
}
