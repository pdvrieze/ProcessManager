package nl.adaptivity.process.engine.pma.dynamic.scope.templates

import nl.adaptivity.process.engine.pma.dynamic.runtime.DynamicPmaActivityContext
import nl.adaptivity.process.engine.pma.dynamic.scope.CommonPMAPermissions
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.AuthScopeTemplate
import nl.adaptivity.process.engine.pma.models.ServiceId
import nl.adaptivity.process.engine.pma.models.ServiceName

class NameDelegateScopeTemplate<AIC: DynamicPmaActivityContext<AIC, *>>(val targetServiceName: ServiceName<*>, val scopeTemplates: Array<out AuthScopeTemplate<AIC>>) :
    AuthScopeTemplate<AIC> {
    override fun instantiateScope(context: AIC): AuthScope? {
        val scopes = scopeTemplates.mapNotNull { it.instantiateScope(context) }.toTypedArray()
        val scope = scopes.reduce { left, right -> left.union(right)}
        val targetService: ServiceId<*> = context.processContext.contextFactory.serviceResolver.resolveService(targetServiceName).serviceInstanceId

        return CommonPMAPermissions.DELEGATED_PERMISSION.restrictTo(serviceId = targetService, scope = scope)
    }
}

class IdDelegateScopeTemplate<AIC: DynamicPmaActivityContext<AIC, *>>(val targetServiceId: ServiceId<*>, val scopeTemplates: Array<out AuthScopeTemplate<AIC>>) :
    AuthScopeTemplate<AIC> {
    override fun instantiateScope(context: AIC): AuthScope? {
        val scopes = scopeTemplates.mapNotNull { it.instantiateScope(context) }.toTypedArray()
        val scope = scopes.reduce { left, right -> left.union(right)}

        return CommonPMAPermissions.DELEGATED_PERMISSION.restrictTo(serviceId = targetServiceId, scope = scope)
    }
}
