package nl.adaptivity.process.engine.pma.test

import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.pma.models.*
import nl.adaptivity.process.engine.pma.runtime.AuthServiceClient
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.processModel.AuthorizationInfo
import nl.adaptivity.util.multiplatform.PrincipalCompat


class TestAuthServiceClient() : AuthServiceClient<AuthorizationInfo, AuthorizationInfo.Token, AuthorizationInfo.Token> {
    override val principal: PrincipalCompat = SimplePrincipal("TestClient")

    override fun validateAuthInfo(
        authInfoToCheck: AuthorizationInfo,
        serviceId: ServiceId<Service>,
        scope: UseAuthScope
    ) {
        // Do nothing
    }

    override fun requestAuthToken(
        authorizationTarget: ResolvedInvokableMethod,
        authorizations: List<AuthScope>,
        processNodeInstanceHandle: PNIHandle
    ): DummyTokenServiceAuthData {
        return DummyTokenServiceAuthData(authorizationTarget.serviceId, authorizations)
    }

    override fun requestPmaAuthCode(
        client: ServiceId<*>,
        nodeInstanceHandle: PNIHandle,
        serviceId: ServiceId<*>,
        requestedScope: AuthScope
    ): AuthorizationInfo.Token {
        return DummyTokenServiceAuthData(serviceId, listOf(requestedScope))
    }

    override fun requestPmaAuthCode(
        identifiedUser: PrincipalCompat,
        nodeInstanceHandle: PNIHandle,
        authorizedService: ServiceId<*>,
        tokenTargetService: ServiceId<*>,
        requestedScope: AuthScope
    ): AuthorizationInfo.Token {
        return DummyTokenServiceAuthData(tokenTargetService, listOf(requestedScope))
    }

    override fun invalidateActivityTokens(hNodeInstance: PNIHandle) {
        // Doesn't do anything
    }

    override fun exchangeAuthCode(authorizationCode: AuthorizationInfo.Token): AuthorizationInfo.Token {
        return authorizationCode
    }
}

class DummyTokenServiceAuthData(val targetService: ServiceId<*>, val authorizations: List<AuthScope>) :
    AuthorizationInfo.Token {
    override val token: String
        get() = "$targetService:::$authorizations"
}
