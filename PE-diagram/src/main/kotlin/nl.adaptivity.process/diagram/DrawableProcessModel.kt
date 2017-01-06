/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.diagram

import nl.adaptivity.diagram.Diagram
import nl.adaptivity.process.clientProcessModel.ClientProcessModel
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.RootProcessModel

/**
 * Created by pdvrieze on 05/01/17.
 */
interface DrawableProcessModel : ClientProcessModel<DrawableProcessNode, DrawableProcessModel?>, Diagram {
  interface Builder : ClientProcessModel.Builder<DrawableProcessNode, DrawableProcessModel?> {
    override fun childModelBuilder(): Activity.ChildModelBuilder<DrawableProcessNode, DrawableProcessModel?> {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
  }

  fun notifyNodeChanged(node: DrawableProcessNode) = Unit
}