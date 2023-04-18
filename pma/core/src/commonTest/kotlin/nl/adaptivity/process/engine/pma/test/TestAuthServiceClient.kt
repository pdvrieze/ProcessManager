package nl.adaptivity.process.engine.pma.test

import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.process.engine.pma.AuthorizationException
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

    override fun isTokenValid(token: AuthorizationInfo.Token): Boolean {
        return true
    }

    override fun requestPmaAuthToken(
        nodeInstanceHandle: PNIHandle,
        serviceId: ServiceId<*>,
        scope: AuthScope
    ): AuthorizationInfo.Token {
        return DummyTokenServiceAuthData(serviceId, listOf(scope))
    }

    override fun exchangeDelegateToken(
        exchangedToken: AuthorizationInfo.Token,
        service: ServiceId<*>,
        requestedScope: AuthScope
    ): AuthorizationInfo.Token {
        if (exchangedToken is DummyTokenServiceAuthData) {
            if(exchangedToken.targetService != service) throw AuthorizationException("Services don't match")
        }
        return exchangedToken
    }

    override fun getAuthTokenDirect(serviceId: ServiceId<Service>, reqScope: AuthScope): AuthorizationInfo.Token {
        return DummyTokenServiceAuthData(serviceId, listOf(reqScope))
    }

    override fun registerGlobalPermission(principal: PrincipalCompat, service: Service, scope: AuthScope) {
        // NO-Op
    }

    override fun registerClient(serviceName: ServiceName<*>, secret: String): AuthorizationInfo {
        return DummyTokenServiceAuthData(ServiceId<Service>(serviceName.serviceName), emptyList())
    }

    override fun registerClient(principal: PrincipalCompat, secret: String): AuthorizationInfo {
        return DummyTokenServiceAuthData(ServiceId<Service>(principal.name), emptyList())
    }

    override fun userHasPermission(
        principal: PrincipalCompat,
        serviceId: ServiceId<*>,
        permission: UseAuthScope
    ): Boolean {
        return true
    }
}

class DummyTokenServiceAuthData(val targetService: ServiceId<*>, val authorizations: List<AuthScope>) :
    AuthorizationInfo.Token {
    override val token: String
        get() = "$targetService:::$authorizations"
}
