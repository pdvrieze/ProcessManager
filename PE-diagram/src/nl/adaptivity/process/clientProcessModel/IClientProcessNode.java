package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.processModel.ProcessNodeSet;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;


public interface IClientProcessNode<T extends IClientProcessNode<T>> extends ProcessNode<T> {

  /**
   * Set the X coordinate of the reference point of the element. This is
   * normally the center.
   *
   * @param x The x coordinate
   */
  void setX(double x);

  /**
   * Set the Y coordinate of the reference point of the element. This is
   * normally the center of the symbol (excluding text).
   *
   * @param y
   */
  void setY(double y);

  @NotNull
  @Override
  Set<? extends Identifiable> getPredecessors();

  @NotNull
  @Override
  ProcessNodeSet<? extends T> getSuccessors();

  void setOwner(ClientProcessModel<T> owner);

  @Nullable
  ClientProcessModel<T> getOwner();
  
  void disconnect();
  
  void serialize(SerializerAdapter out);
}
