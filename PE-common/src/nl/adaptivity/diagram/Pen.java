package nl.adaptivity.diagram;


public interface Pen<PEN_T extends Pen<PEN_T>> {
  PEN_T setColor(int red, int green, int blue);
  PEN_T setColor(int red, int green, int blue, int alpha);
  PEN_T setStrokeWidth(double strokeWidth);
}
