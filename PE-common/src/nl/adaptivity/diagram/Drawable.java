package nl.adaptivity.diagram;

public interface Drawable extends Bounded {

  /**
   * Draw the drawable to the given canvas. The drawing will use a top left of (0,0).
   * The canvas will translate coordinates.
   * @param pCanvas The canvas to draw on.
   * @param pClipBounds The part of the drawing to draw. Outside no drawing is needed.
   */
  void draw(Canvas pCanvas, Rectangle pClipBounds);

}
