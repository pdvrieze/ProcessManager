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

package nl.adaptivity.process.processModel;

import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.Collection;


public interface EndNode<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> extends ProcessNode<T, M> {

  String ELEMENTLOCALNAME = "end";
  QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  void setDefines(Collection<? extends IXmlDefineType> exports);

  @Nullable
  Identifiable getPredecessor();

  void setPredecessor(Identifiable predecessor);

}