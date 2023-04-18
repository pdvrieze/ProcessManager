package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.ServiceId

interface TokenPrincipal {
    val serviceId: ServiceId<*>?
    val scope: AuthScope
}
