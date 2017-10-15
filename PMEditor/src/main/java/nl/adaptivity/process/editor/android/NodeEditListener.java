/*
 * Copyright (c) 2016.
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

package nl.adaptivity.process.editor.android;

import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.util.Identifiable;


/**
 * Interface to enable interacting with process nodes.
 * Created by pdvrieze on 10/01/16.
 */
public interface NodeEditListener {

  DrawableProcessNode.Builder getNode(int pos);

  void onNodeEdit(int pos);

  void updateNode(int pos, DrawableProcessNode.Builder newValue);

  DrawableProcessNode.Builder getNode(Identifiable id);
}
