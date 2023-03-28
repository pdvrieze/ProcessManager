package nl.adaptivity.process.engine.pma.dynamic.scope


import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.ServiceId

class DelegatePermissionScope(targetService: ServiceId, scopes: Array<AuthScope>) : AuthScope {

}
