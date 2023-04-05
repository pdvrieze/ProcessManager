package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.engine.pma.models.ResolvedInvokableMethod
import nl.adaptivity.process.processModel.AuthorizationInfo

interface AuthServiceClient {
    fun requestAuthToken(
        authorizationTarget: ResolvedInvokableMethod,
        authorizations: List<AuthScope>,
        processNodeInstanceHandle: PNIHandle
    ): AuthorizationInfo.Token

}

