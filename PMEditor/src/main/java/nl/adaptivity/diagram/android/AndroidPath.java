package nl.adaptivity.diagram.android;

import android.graphics.Path;
import nl.adaptivity.diagram.DiagramPath;


public final class AndroidPath implements DiagramPath<AndroidPath> {

  private Path aPath = new Path();

  @Override
  public AndroidPath moveTo(double x, double y) {
    aPath.moveTo((float)x, (float) y);
    return this;
  }

  @Override
  public AndroidPath lineTo(double x, double y) {
    aPath.lineTo((float)x, (float) y);
    return this;
  }

  @Override
  public AndroidPath cubicTo(double x1, double y1, double x2, double y2, double x3, double y3) {
    aPath.cubicTo((float)x1, (float)y1, (float)x2, (float)y2, (float)x3, (float)y3);
    return this;
  }

  @Override
  public AndroidPath close() {
    aPath.close();
    return this;
  }

  public Path getPath() {
    return aPath;
  }

}
