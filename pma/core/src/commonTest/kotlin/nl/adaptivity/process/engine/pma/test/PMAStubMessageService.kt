package nl.adaptivity.process.engine.pma.test

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.MessageSendingResult
import nl.adaptivity.process.engine.ActivityInstanceContext
import nl.adaptivity.process.engine.ProcessEngineDataAccess
import nl.adaptivity.process.engine.StubMessageService
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.process.processModel.ServiceAuthData

class PMAStubMessageService(localEndpoint: EndpointDescriptor) : StubMessageService(localEndpoint) {
    override fun sendMessage(
        engineData: ProcessEngineDataAccess<*>,
        protoMessage: IXmlMessage,
        activityInstanceContext: ActivityInstanceContext,
        authData: ServiceAuthData?
    ): MessageSendingResult {
        return super.sendMessage(engineData, protoMessage, activityInstanceContext, authData)
    }
}
