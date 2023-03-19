package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.messaging.InvokableService
import nl.adaptivity.process.processModel.TokenServiceAuthData

interface AuthService {
    fun requestAuthToken(targetService: InvokableService, authorizations: List<AuthScope>): TokenServiceAuthData

}
