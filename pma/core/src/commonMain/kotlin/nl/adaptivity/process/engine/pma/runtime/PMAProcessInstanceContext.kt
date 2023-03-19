package nl.adaptivity.process.engine.pma.runtime

import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessInstanceContext
import nl.adaptivity.process.engine.pma.models.AuthScope
import nl.adaptivity.process.messaging.InvokableService
import nl.adaptivity.process.processModel.TokenServiceAuthData

interface PMAProcessInstanceContext<A: PMAActivityContext<A>>: ProcessInstanceContext {

    val contextFactory: PMAProcessContextFactory<A>


    fun <MSG_T> requestAuthData(
        messageService: IMessageService<MSG_T, A>,
        targetService: InvokableService,
        authorizations: List<AuthScope>
    ): TokenServiceAuthData {
        return contextFactory.authService.requestAuthToken(targetService, authorizations)
    }

}
