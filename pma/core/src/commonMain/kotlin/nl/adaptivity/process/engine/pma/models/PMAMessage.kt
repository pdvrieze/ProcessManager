package nl.adaptivity.process.engine.pma.models

import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.xmlutil.util.ICompactFragment

class PMAMessage(
    override val targetMethod: InvokableMethod,
    @Deprecated("Use targetMethod", replaceWith = ReplaceWith("targetMethod.operation"))
    override val operation: String?,
    override val messageBody: ICompactFragment,
) : IXmlMessage {

    override fun toString(): String {
        TODO("not implemented")
    }
}
