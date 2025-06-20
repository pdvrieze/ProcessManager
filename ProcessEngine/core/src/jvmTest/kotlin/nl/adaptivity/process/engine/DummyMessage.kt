package nl.adaptivity.process.engine

import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.messaging.RESTMethodDesc
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment
import javax.xml.namespace.QName

object DummyMessage: IXmlMessage {
    override val targetMethod: InvokableMethod
        get() = RESTMethodDesc(QName("dummy"), "POST", "/dummy", "application/x-dummy")
    override val operation: String?
        get() = null
    override val messageBody: ICompactFragment
        get() = CompactFragment("")
    override val url: String?
        get() = "/dummy"

    override fun toString(): String {
        return "DummyMessage"
    }

    val JSON = "\"message\":{\"content\":{\"namespaces\":[], \"content\":\"\"},\"type\":\"application/x-dummy\",\"url\":\"/dummy\",\"method\":\"POST\"}"
    val XML = "<pe:message type=\"application/x-dummy\" url=\"/dummy\" method=\"POST\"/>"


    private val hashCode = XmlMessage.from(this).hashCode()

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        return other == DummyMessage ||
            (XmlMessage.from(this) == other)
    }
}
