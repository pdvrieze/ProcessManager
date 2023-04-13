package nl.adaptivity.process.engine.pma.test

import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.processModel.AuthorizationInfo

class ScopeAuthData(private val scope: AuthScope) : AuthorizationInfo.Token {
    override val token: String
        get() = scope.description
}
