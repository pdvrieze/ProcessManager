package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.ProcessNodeSet;


public interface IClientProcessNode<T extends IClientProcessNode<T>> extends ProcessNode<T> {

  /**
   * Set the X coordinate of the reference point of the element. This is
   * normally the center.
   * 
   * @param pX The x coordinate
   */
  void setX(double pX);

  /**
   * Set the Y coordinate of the reference point of the element. This is
   * normally the center of the symbol (excluding text).
   * 
   * @param pY
   */
  void setY(double pY);

  @Override
  public ProcessNodeSet<? extends T> getPredecessors();

  @Override
  public ProcessNodeSet<? extends T> getSuccessors();

  void setOwner(ClientProcessModel<T> pOwner);

  ClientProcessModel<T> getOwner();
}
