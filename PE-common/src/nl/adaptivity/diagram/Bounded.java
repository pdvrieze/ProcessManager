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
   * The method will return the most specific element contained.
   * @param aX The X coordinate
   * @param aY The Y coordinate
   * @return <code>null</code> if no item could be found, otherwise the item found.
   */
  public Bounded getItemAt(double pX, double pY);

}