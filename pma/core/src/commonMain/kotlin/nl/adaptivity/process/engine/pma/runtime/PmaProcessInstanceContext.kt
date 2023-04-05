package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.ProcessInstanceContext
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.ResolvedInvokableMethod
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.processModel.AuthorizationInfo

interface PmaProcessInstanceContext<A: PmaActivityContext<A>>: ProcessInstanceContext {

    val contextFactory: PMAProcessContextFactory<A>


    fun requestAuthData(
        targetService: ResolvedInvokableMethod,
        authorizations: List<AuthScope>,
        nodeInstanceHandle: PNIHandle
    ): AuthorizationInfo.Token {
        return contextFactory.engineServiceAuthServiceClient.requestAuthToken(targetService, authorizations, nodeInstanceHandle)
    }

    fun resolveService(targetService: InvokableMethod): ResolvedInvokableMethod? {
        return contextFactory.resolveService(targetService)
    }

}
