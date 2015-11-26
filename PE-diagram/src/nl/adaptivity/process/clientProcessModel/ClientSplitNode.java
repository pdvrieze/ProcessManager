package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.Split;
import nl.adaptivity.process.processModel.SplitBase;


/**
 * A split node for clients. Note that ClientSplitNodes don't have a compat mode. They have multiple succesors
 * and compatibility concerns their absense.
 *
 * @param <T> The type of ProcessNode used.
 */
public class ClientSplitNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends SplitBase<T, M> implements ClientProcessNode<T,M> {

  public ClientSplitNode(final M ownerModel) {
    super(ownerModel);
  }

  public ClientSplitNode(final M ownerModel, String id) {
    super(ownerModel);
    setId(id);
  }

  protected ClientSplitNode(Split<?, ?> orig) {
    super(orig);
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public boolean isCompat() {
    return false;
  }
}
