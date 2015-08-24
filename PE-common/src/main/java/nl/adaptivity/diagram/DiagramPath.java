package nl.adaptivity.diagram;

/**
 * An abstraction for paths that can be drawn on a {@link Canvas}.
 * 
 * @author Paul de Vrieze
 * @param <PATH_T> The type of path. This normally is the type itself.
 */
public interface DiagramPath<PATH_T extends DiagramPath<PATH_T>> {

  /**
   * Move to a new point. This will create a new sub-path.
   * 
   * @param pX The new X coordinate.
   * @param pY The new Y coordinate
   * @return The path itself, to allow for method chaining.
   */
  PATH_T moveTo(double pX, double pY);

  /**
   * Draw a line from the current point to a new point.
   * 
   * @param pX The new X coordinate.
   * @param pY The new Y coordinate
   * @return The path itself, to allow for method chaining.
   */
  PATH_T lineTo(double pX, double pY);


  /**
   * Draw a cubic bezier spline from the current point to a new point.
   * 
   * @param x1 The first control point's x coordinate
   * @param y1 The first control point's y coordinate.
   * @param x2 The second control point's x coordinate
   * @param y2 The second control point's y coordinate.
   * @param x3 The endpoint's x coordinate
   * @param y3 The endpoint's y coordinate.
   * @return The path itself, to allow for method chaining.
   */
  PATH_T cubicTo(double x1, double y1, double x2, double y2, double x3, double y3);

  /**
   * Close the current sub-path by drawing a straight line back to the
   * beginning.
   * 
   * @return The path itself, to allow for method chaining.
   */
  PATH_T close();

}
