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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel.engine;

import net.devrieze.util.Handle;
import nl.adaptivity.process.processModel.ProcessModel;
import nl.adaptivity.process.processModel.MutableProcessNode;
import nl.adaptivity.process.processModel.ProcessNode;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;


public interface IProcessModelRef<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> extends Handle<M>{

  String getName();

  @Nullable
  UUID getUuid();

}