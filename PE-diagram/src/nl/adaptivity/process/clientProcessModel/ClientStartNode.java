package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.StartNodeBase;

public class ClientStartNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends StartNodeBase<T, M> implements ClientProcessNode<T, M> {

  private final boolean mCompat;

  public ClientStartNode(final M ownerModel, final boolean compat) {
    super(ownerModel);
    mCompat = compat;
  }

  public ClientStartNode(final M ownerModel, final String id, final boolean compat) {
    super(ownerModel);
    setId(id);
    mCompat = compat;
  }

  protected ClientStartNode(final ClientStartNode<T, M> orig, final boolean compat) {
    super(orig);
    mCompat = compat;
  }

  @Override
  public int getMaxSuccessorCount() {
    return isCompat() ? Integer.MAX_VALUE : 1;
  }

  @Override
  public boolean isCompat() {
    return mCompat;
  }
}
