package nl.adaptivity.process.engine.pma.test

import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.processModel.TokenServiceAuthData

class ScopeAuthData(private val scope: AuthScope) : TokenServiceAuthData {
    override val token: String
        get() = scope.description
}
