package nl.adaptivity.diagram;

public interface Drawable extends Bounded {

  /**
   * Draw the drawable to the given canvas. The drawing will use a top left of (0,0).
   * The canvas will translate coordinates.
   * @param pCanvas The canvas to draw on.
   * @param pClipBounds The part of the drawing to draw. Outside no drawing is needed.
   */
  <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds);

  @Override
  public Drawable getItemAt(double pX, double pY);
}
