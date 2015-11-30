package nl.adaptivity.diagram;


public interface Positioned {

  /**
   * Determine whether the element actually has a real position.
   * @return <code>true</code> if it has, <code>false</code> if not.
   */
  boolean hasPos();

  /**
   * Get the X coordinate of the gravity point of the element. The point is
   * generally the center, but it is element dependent.
   *
   * @return The X coordinate
   */
  double getX();

  /**
   * Get the Y coordinate of the gravity point of the element. The point is
   * generally the center, but it is element dependent.
   *
   * @return The Y coordinate
   */
  double getY();

}
