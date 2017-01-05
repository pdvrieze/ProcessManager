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

import nl.adaptivity.diagram.Drawable.STATE_DEFAULT
import nl.adaptivity.diagram.ItemCache


class DrawableJoinSplitDelegate {

  val itemCache = ItemCache()
  var state: Int = 0

  constructor() {
    this.state = STATE_DEFAULT
  }

  constructor(orig: DrawableJoinSplitDelegate) {
    state = orig.state
  }

}