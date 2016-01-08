package nl.adaptivity.diagram;


import org.jetbrains.annotations.NotNull;


public final class Point {
  public final double x;
  public final double y;
  
  public Point(final double x, final double y) {
    this.x = x;
    this.y = y;
  }

  @NotNull
  @Override
  public String toString() {
    return "(" + x + ", " + y + ')';
  }
}
