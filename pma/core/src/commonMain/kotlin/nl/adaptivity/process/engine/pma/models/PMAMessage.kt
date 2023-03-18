package nl.adaptivity.process.engine.pma.models

import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.process.processModel.ServiceAuthData
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.util.ICompactFragment

class PMAMessage(
    override val serviceName: String?,
    override val serviceNS: String?,
    override val service: QName?,
    override val endpoint: String?,
    override val endpointDescriptor: EndpointDescriptor?,
    override val operation: String?,
    override val messageBody: ICompactFragment,
    override val serviceAuthData: ServiceAuthData,
    override val url: String?,
    override val method: String?,
    override val contentType: String
) : IXmlMessage {
    override fun setType(type: String) {
        TODO("not implemented")
    }

    override fun toString(): String {
        TODO("not implemented")
    }
}
