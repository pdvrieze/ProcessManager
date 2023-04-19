/*
 * Copyright (c) 2019.
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

interface MessageActivity : Activity {

    /**
     * The message of this activity. This provides all the information to be
     * able to actually invoke the service.
     */
    val message: IXmlMessage?

    val accessRestrictions: AccessRestriction?

    override fun builder(): Builder

    interface Builder : Activity.Builder, ProcessNode.Builder {
        var message: IXmlMessage?
        @Deprecated("Names are not used anymore")
        override var name: String?

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>) = visitor.visitMessageActivity(this)

        val accessRestrictions: AccessRestriction?

    }

    interface RWBuilder: Builder {
        override var accessRestrictions: AccessRestriction?
    }

}
