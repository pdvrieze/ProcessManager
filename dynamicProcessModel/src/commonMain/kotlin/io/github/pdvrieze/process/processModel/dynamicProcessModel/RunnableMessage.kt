package io.github.pdvrieze.process.processModel.dynamicProcessModel

import nl.adaptivity.process.messaging.InvokableMethod
import nl.adaptivity.process.processModel.IXmlMessage
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

interface RunnableMessage<S>: IXmlMessage {
    fun run(target: S)
}

class GenericRunnableMessage<S>(override val targetMethod: InvokableMethod, val action: S.() -> Unit): RunnableMessage<S> {

    override val messageBody: ICompactFragment get() = CompactFragment("")

    operator fun component1(): InvokableMethod = targetMethod
    operator fun component2(): (S.() -> Unit) = action

    override fun run(target: S) {
        target.action()
    }

    override fun toString(): String {
        return "<runnable message>"
    }
}
