/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel

import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.processModel.engine.XmlCondition
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.xmlutil.QName


interface Activity : ProcessNode {

    /**
     * The name of this activity. Note that for serialization to XML to work
     * this needs to be unique for the process model at time of serialization, and
     * can not be null or an empty string. While in Java mode other nodes are
     * referred to by reference, not name.
     */
    @Deprecated("Not needed, use id.", ReplaceWith("id"))
    val name: String?

    /**
     * The condition that needs to be true to start this activity. A null value means that the activity can run.
     */
    val condition: Condition?

    /**
     * Get the list of imports. The imports are provided to the message for use as
     * data parameters. Setting will create a copy of the parameter for safety.
     */
    override val results: List<IXmlResultType>

    /**
     * Get the list of exports. Exports will allow storing the response of an
     * activity. Setting will create a copy of the parameter for safety.
     */
    override val defines: List<IXmlDefineType>

    /**
     * The predecessor node for this activity.
     */
    val predecessor: Identifiable?

    val successor: Identifiable?

    override fun builder(): Builder

    interface Builder : ProcessNode.Builder {

        /**
         * The name of this activity. Note that for serialization to XML to work
         * this needs to be unique for the process model at time of serialization, and
         * can not be null or an empty string. While in Java mode other nodes are
         * referred to by reference, not name.
         */
        @Deprecated("Not needed, use id.", ReplaceWith("id"))
        var name: String?

        var condition: Condition?

        var predecessor: Identifiable?

        override val predecessors: Set<Identified>
            get() = setOfNotNull(predecessor?.identifier)

        var successor: Identifiable?

        override val successors: Set<Identified>
            get() = setOfNotNull(successor?.identifier)

        override fun addSuccessor(identifier: Identifier) {
            val s = successor
            if (s != null) {
                if (s.identifier == identifier) return
                throw IllegalStateException("Successor already set")
            }
            successor = identifier
        }

        override fun addPredecessor(identifier: Identifier) {
            val p = predecessor
            if (p != null) {
                if (p.identifier == identifier) return
                throw IllegalStateException("Predecessor already set")
            }
            predecessor = identifier
        }

        override fun removeSuccessor(identifier: Identifiable) {
            if (successor?.id == identifier.id) successor = null
        }

        override fun removePredecessor(identifier: Identifiable) {
            if (predecessor?.id == identifier.id) predecessor = null
        }
    }

    companion object {

        /** The name of the XML element.  */
        const val ELEMENTLOCALNAME = "activity"
        val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)
    }

}

fun Activity.Builder.condition(condition: String) {
    this.condition = XmlCondition(condition)
}
