package nl.adaptivity.process.engine.pma.models

import nl.adaptivity.process.messaging.InvokableMethod

interface ResolvedInvokableMethod {
    val serviceId: ServiceId<*>
    val method: InvokableMethod
}
