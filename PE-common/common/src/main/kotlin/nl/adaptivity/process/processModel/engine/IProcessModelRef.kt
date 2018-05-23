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

package nl.adaptivity.process.processModel.engine

import net.devrieze.util.Handle
import nl.adaptivity.process.processModel.ProcessModel
import nl.adaptivity.process.processModel.ProcessNode
import nl.adaptivity.process.processModel.RootProcessModel
import nl.adaptivity.util.multiplatform.UUID
import nl.adaptivity.xml.XmlSerializable


interface IProcessModelRef<NodeT : ProcessNode<NodeT, ModelT>,
                           ModelT : ProcessModel<NodeT, ModelT>?,
                           out ObjectT : RootProcessModel<NodeT, ModelT>> :
    Handle<ObjectT>, XmlSerializable  {

    val name: String?

    val uuid: UUID?

}