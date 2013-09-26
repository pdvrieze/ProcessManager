package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;


public interface IClientProcessNode<T extends IClientProcessNode<T>> extends ProcessNode<T> {

  double layout(final double pX, final double pY, final IClientProcessNode<?> pSource, final boolean pForward);

  void setX(double pX);

  void setY(double pY);
}
