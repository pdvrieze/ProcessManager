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

import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public abstract class XmlProcessNodeBase extends ProcessNodeBase<XmlProcessNode, ProcessModelImpl> implements XmlProcessNode {

  public static class XmlSplitFactory implements ProcessModelBase.SplitFactory<XmlProcessNode, ProcessModelImpl> {

    @Override
    public Split<XmlProcessNode, ProcessModelImpl> createSplit(final ProcessModelImpl ownerModel, final Collection<? extends Identifiable> successors) {
      XmlSplit result = new XmlSplit(ownerModel);
      result.setSuccessors(successors);
      return result;
    }
  }

//  private Collection<? extends IXmlImportType> mImports;
//
//  private Collection<? extends IXmlExportType> mExports;

  protected XmlProcessNodeBase(@Nullable final ProcessModelImpl ownerModel) {
    super(ownerModel);
  }


  public XmlProcessNodeBase(final ProcessModelImpl ownerModel, @NotNull final Collection<? extends Identifiable> predecessors) {
    this(ownerModel);
    if ((predecessors.size() < 1) && (!(this instanceof StartNode))) {
      throw new IllegalProcessModelException("Process nodes, except start nodes must connect to preceding elements");
    }
    if ((predecessors.size() > 1) && (!(this instanceof Join))) {
      throw new IllegalProcessModelException("Only join nodes may have multiple predecessors");
    }
    setPredecessors(predecessors);
  }

  @NotNull
  @Override
  public String toString() {
    return toString(this);
  }

  @NotNull
  protected static String toString(final XmlProcessNode obj) {
    final StringBuilder result = new StringBuilder();
    result.append(obj.getClass().getSimpleName()).append(" (").append(obj.getId());
    if ((obj.getPredecessors() == null) || (obj.getMaxPredecessorCount()==0)) {
      result.append(')');
    } else {
      final int predCount = obj.getPredecessors().size();
      if (predCount != 1) {
        result.append(", pred={");
        for (final Identifiable pred : obj.getPredecessors()) {
          result.append(pred.getId()).append(", ");
        }
        if (result.charAt(result.length() - 2) == ',') {
          result.setLength(result.length() - 2);
        }
        result.append("})");
      } else {
        result.append(", pred=").append(obj.getPredecessors().iterator().next().getId());
        result.append(')');
      }
    }
    return result.toString();
  }

  @Override
  public void setOwnerModel(@NotNull final ProcessModelImpl ownerModel) {
    super.setOwnerModel(ownerModel);
  }

  @Override
  public void resolveRefs() {
    super.resolveRefs();
  }

  @Override
  public void setPredecessors(final Collection<? extends Identifiable> predecessors) {
    super.setPredecessors(predecessors);
  }

  @Override
  public void removePredecessor(final Identifiable node) {
    super.removePredecessor(node);
  }

  @Override
  public void addPredecessor(final Identifiable node) {
    super.addPredecessor(node);
  }

  @Override
  public void addSuccessor(final Identifiable node) {
    super.addSuccessor(node);
  }

  @Override
  public void removeSuccessor(final Identifiable node) {
    super.removeSuccessor(node);
  }

  @Override
  public void setSuccessors(final Collection<? extends Identifiable> successors) {
    super.setSuccessors(successors);
  }
}
