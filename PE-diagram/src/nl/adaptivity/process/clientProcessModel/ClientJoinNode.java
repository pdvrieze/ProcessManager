package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.Join;
import nl.adaptivity.process.processModel.JoinBase;


public class ClientJoinNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends JoinBase<T, M> implements ClientProcessNode<T, M> {

  private final boolean mCompat;

  public ClientJoinNode(final M ownerModel, final boolean compat) {
    super(ownerModel);
    mCompat = compat;
  }

  public ClientJoinNode(final M ownerModel, String id, final boolean compat) {
    super(ownerModel);
    setId(id);
    mCompat = compat;
  }

  protected ClientJoinNode(Join<?,?> orig, final boolean compat) {
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
