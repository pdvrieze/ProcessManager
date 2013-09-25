package nl.adaptivity.diagram;


public interface Positioned {

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
