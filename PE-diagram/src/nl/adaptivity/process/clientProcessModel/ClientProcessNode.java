package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.ProcessNode;
import nl.adaptivity.process.util.Identifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;


public interface ClientProcessNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ProcessNode<T, M> {

  boolean isCompat();

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
  Set<? extends Identifiable> getSuccessors();

  void setOwnerModel(M owner);

  void setId(String id);

  @Nullable
  M getOwnerModel();

}
