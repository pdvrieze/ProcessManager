package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.processModel.TokenServiceAuthData

interface AuthServiceClient {
    fun requestAuthToken(targetService: InvokableMethod, authorizations: List<AuthScope>): TokenServiceAuthData

}
