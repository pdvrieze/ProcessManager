package nl.adaptivity.process.engine.pma.models

import nl.adaptivity.process.messaging.InvokableService
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.xmlutil.util.ICompactFragment

class PMAMessage(
    override val targetService: InvokableService,
    override val operation: String?,
    override val messageBody: ICompactFragment,
) : IXmlMessage {
    override fun setType(type: String) {
        TODO("not implemented")
    }

    override fun toString(): String {
        TODO("not implemented")
    }
}
