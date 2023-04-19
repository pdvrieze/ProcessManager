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

interface CompositeActivity : Activity {

    val childModel: ChildProcessModel<ProcessNode>?

    override fun builder(): Builder

    interface Builder: Activity.Builder

    interface ReferenceBuilder : Builder {
        var childId: String?

    }

    interface ModelBuilder : Builder, ChildProcessModel.Builder {
        override val idBase: String get() = "sub"

        override fun <R> visit(visitor: ProcessNode.BuilderVisitor<R>) = visitor.visitCompositeActivity(this)
    }

}
