package nl.adaptivity.process.processModel

import nl.adaptivity.process.ProcessConsts
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.QName

interface EventNode: IEventNode {
    val predecessor: Identifiable?
    val successor: Identifiable?
    val isThrowing: Boolean
    override val eventType: IEventNode.Type

    override fun builder(): Builder

    interface Builder: IEventNode.Builder {
        var isThrowing: Boolean

        var successor: Identifiable?

        override val successors: Set<Identified>
            get() = setOfNotNull(successor?.identifier)

        var predecessor: Identifiable?

        override val predecessors: Set<Identified>
            get() = setOfNotNull(predecessor?.identifier)


        override fun addSuccessor(identifier: Identifier) {
            val s = successor
            if (s != null) {
                if (s.identifier == identifier) return
                throw IllegalStateException("Successor already set")
            }
            successor = identifier
        }

        override fun removeSuccessor(identifier: Identifiable) {
            if (successor?.id == identifier.id) successor = null
        }

        override fun addPredecessor(identifier: Identifier) {
            val s = predecessor
            if (s != null) {
                if (s.identifier == identifier) return
                throw IllegalStateException("Predecessor already set")
            }
            predecessor = identifier
        }

        override fun removePredecessor(identifier: Identifiable) {
            if (predecessor?.id == identifier.id) predecessor = null
        }

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>): R = visitor.visitEventNode(this)
    }

    companion object {

        const val ELEMENTLOCALNAME = "event"
        val ELEMENTNAME = QName(ProcessConsts.Engine.NAMESPACE, ELEMENTLOCALNAME, ProcessConsts.Engine.NSPREFIX)

    }

}
