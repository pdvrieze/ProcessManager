package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessInstanceContext
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.processModel.TokenServiceAuthData

interface PmaProcessInstanceContext<A: PmaActivityContext<A>>: ProcessInstanceContext {

    val contextFactory: PMAProcessContextFactory<A>


    fun <MSG_T> requestAuthData(
        messageService: IMessageService<MSG_T>,
        targetService: InvokableMethod,
        authorizations: List<AuthScope>
    ): TokenServiceAuthData {
        return contextFactory.authServiceClient.requestAuthToken(targetService, authorizations)
    }

}
