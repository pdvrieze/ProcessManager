package nl.adaptivity.diagram;


public final class Point {
  public final double x;
  public final double y;
  
  public Point(double pX, double pY) {
    x = pX;
    y = pY;
  }

  @Override
  public String toString() {
    return "(" + x + ", " + y + ')';
  }
}
