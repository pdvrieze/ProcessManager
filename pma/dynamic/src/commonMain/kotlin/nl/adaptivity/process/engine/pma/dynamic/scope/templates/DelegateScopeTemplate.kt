package nl.adaptivity.process.engine.pma.dynamic.scope.templates

import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPMAActivityContext
import nl.adaptivity.process.engine.pma.dynamic.scope.DelegatePermissionScope
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.AuthScopeTemplate
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName

class DelegateScopeTemplate<AIC: DynamicPMAActivityContext<AIC, *>>(val targetServiceName: ServiceName<*>, val scopeTemplates: Array<out AuthScopeTemplate<AIC>>) :
    AuthScopeTemplate<AIC> {
    override fun instantiateScope(context: AIC): AuthScope? {
        val scopes = scopeTemplates.mapNotNull { it.instantiateScope(context) }.toTypedArray()
        val targetService: ServiceId<*> = context.processContext.contextFactory.resolveService(targetServiceName).serviceInstanceId

        return DelegatePermissionScope(targetService, scopes)
    }
}
