package nl.adaptivity.process.engine.pma.test

import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.engine.pma.runtime.AuthServiceClient
import nl.adaptivity.process.messaging.InvokableService
import nl.adaptivity.process.processModel.TokenServiceAuthData


class TestAuthServiceClient() : AuthServiceClient {
    override fun requestAuthToken(
        targetService: InvokableService,
        authorizations: List<AuthScope>
    ): DummyTokenServiceAuthData {
        return DummyTokenServiceAuthData(targetService, authorizations)
    }
}

class DummyTokenServiceAuthData(val targetService: InvokableService, val authorizations: List<AuthScope>) :
    TokenServiceAuthData {
    override val token: String
        get() = "$targetService:::$authorizations"
}
