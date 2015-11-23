package nl.adaptivity.process.processModel;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;


/**
 * Base class for activity implementations
 * Created by pdvrieze on 23/11/15.
 */
public abstract class ActivityBase<T extends ProcessNode<T>> extends ProcessNodeBase<T> implements Activity<T> {

  public ActivityBase(@Nullable final ProcessModelBase<T> ownerModel) {
    super(ownerModel);
  }

  @Override
  public void setResults(@Nullable final Collection<? extends IXmlResultType> imports) { super.setResults(imports); }

  @Override
  public void setDefines(@Nullable final Collection<? extends IXmlDefineType> exports) { super.setDefines(exports); }
}
