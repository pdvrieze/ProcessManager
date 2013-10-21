package nl.adaptivity.diagram;


public interface Bounded {

  /**
   * Get a rectangle containing the bounds of the object. Objects should not normally
   * be drawn only inside their bounds. And the bounds are expected to be as small as possible.
   * @return The bounds of the object.
   */
  public Rectangle getBounds();

  /**
   * Determine whether the given coordinate lies within the object. As objects may be
   * shaped, this may mean that some points are not part even though they look to be.
   * @param aX The X coordinate
   * @param aY The Y coordinate
   * @return <code>true</code> if in bounds, <code>false</code> if not.
   */
  public boolean isInBounds(double aX, double aY);

}