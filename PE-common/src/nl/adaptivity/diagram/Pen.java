package nl.adaptivity.diagram;


public interface Pen {
  Pen setColor(int red, int green, int blue);
  Pen setColor(int red, int green, int blue, int alpha);
  Pen setStrokeWidth(double strokeWidth);
}
