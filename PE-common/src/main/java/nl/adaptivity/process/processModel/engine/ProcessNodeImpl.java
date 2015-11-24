package nl.adaptivity.process.processModel.engine;

import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import java.util.Collection;


@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "ProcesNode")
@XmlSeeAlso({ JoinImpl.class, SplitImpl.class, JoinSplitImpl.class, ActivityImpl.class, EndNodeImpl.class, StartNodeImpl.class })
public abstract class ProcessNodeImpl extends ProcessNodeBase<ExecutableProcessNode, ProcessModelImpl> implements ExecutableProcessNode {

  public static class ExecutableSplitFactory implements ProcessModelBase.SplitFactory<ExecutableProcessNode, ProcessModelImpl> {

    @Override
    public Split<? extends ExecutableProcessNode, ProcessModelImpl> createSplit(final ProcessModelImpl ownerModel, final Collection<? extends Identifiable> successors) {
      SplitImpl result = new SplitImpl(ownerModel);
      result.setSuccessors(successors);
      return result;
    }
  }

//  private Collection<? extends IXmlImportType> mImports;
//
//  private Collection<? extends IXmlExportType> mExports;

  protected ProcessNodeImpl(@Nullable final ProcessModelImpl ownerModel) {
    super(ownerModel);
    if (ownerModel!=null) {
      mOwnerModel.addNode(this);
    }
  }


  public ProcessNodeImpl(final ProcessModelImpl ownerModel, @NotNull final Collection<? extends Identifiable> predecessors) {
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
  protected static String toString(final ProcessNodeImpl obj) {
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


}
