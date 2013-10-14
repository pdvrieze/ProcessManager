package nl.adaptivity.diagram;


public interface Pen<S extends DrawingStrategy<S>> {
  Pen<S> setColor(int red, int green, int blue);
  Pen<S> setColor(int red, int green, int blue, int alpha);
  Pen<S> setStrokeWidth(double strokeWidth);
}
