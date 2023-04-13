package nl.adaptivity.process.engine.pma.test

import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.models.ResolvedInvokableMethod
import nl.adaptivity.process.engine.pma.runtime.AuthServiceClient
import nl.adaptivity.process.engine.processModel.PNIHandle
import nl.adaptivity.process.processModel.AuthorizationInfo


class TestAuthServiceClient() : AuthServiceClient {
    override fun requestAuthToken(
        authorizationTarget: ResolvedInvokableMethod,
        authorizations: List<AuthScope>,
        processNodeInstanceHandle: PNIHandle
    ): DummyTokenServiceAuthData {
        return DummyTokenServiceAuthData(authorizationTarget, authorizations)
    }
}

class DummyTokenServiceAuthData(val targetService: ResolvedInvokableMethod, val authorizations: List<AuthScope>) :
    AuthorizationInfo.Token {
    override val token: String
        get() = "$targetService:::$authorizations"
}
