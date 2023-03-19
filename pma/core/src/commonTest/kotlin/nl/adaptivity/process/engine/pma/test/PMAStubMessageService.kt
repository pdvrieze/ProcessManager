package nl.adaptivity.process.engine.pma.test

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.MessageSendingResult
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.StubMessageService
import nl.adaptivity.process.engine.pma.runtime.PMAActivityContext
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.process.processModel.ServiceAuthData

class PMAStubMessageService<C: PMAActivityContext<C>>(localEndpoint: EndpointDescriptor) : StubMessageService<C>(localEndpoint) {
    override fun sendMessage(
        engineData: ProcessEngineDataAccess<C>,
        protoMessage: IXmlMessage,
        activityInstanceContext: C,
        authData: ServiceAuthData?
    ): MessageSendingResult {
        return super.sendMessage(engineData, protoMessage, activityInstanceContext, authData)
    }
}
