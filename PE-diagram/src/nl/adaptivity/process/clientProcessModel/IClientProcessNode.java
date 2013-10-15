package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public interface IClientProcessNode<T extends IClientProcessNode<T>> extends ProcessNode<T> {

  void setX(double pX);

  void setY(double pY);

  @Override
  public ProcessNodeSet<? extends T> getPredecessors();

  @Override
  public ProcessNodeSet<? extends T> getSuccessors();

  void setOwner(ClientProcessModel<T> pOwner);

  ClientProcessModel<T> getOwner();
}
