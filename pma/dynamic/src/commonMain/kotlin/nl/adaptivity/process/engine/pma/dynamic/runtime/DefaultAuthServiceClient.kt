package nl.adaptivity.process.engine.pma.dynamic.runtime

import nl.adaptivity.process.engine.pma.AuthService
import nl.adaptivity.process.engine.pma.PmaAuthInfo
import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.ResolvedInvokableMethod
import nl.adaptivity.process.engine.pma.runtime.AuthServiceClient
import nl.adaptivity.process.engine.processModel.PNIHandle

class DefaultAuthServiceClient(private val originatingClientAuth: PmaAuthInfo, private val authService: AuthService) : AuthServiceClient {
    override fun requestAuthToken(
        authorizationTarget: ResolvedInvokableMethod,
        authorizations: List<AuthScope>,
        processNodeInstanceHandle: PNIHandle
    ): PmaAuthToken {
        val authorization = authorizations.reduce { l, r -> l.union(r) }


        val authCode = authService.getAuthorizationCode(originatingClientAuth, authorizationTarget.serviceId, authorization)
        return authService.getAuthToken(originatingClientAuth, authCode)
    }
}
