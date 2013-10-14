package nl.adaptivity.diagram.android;

import android.graphics.Path;
import nl.adaptivity.diagram.DiagramPath;


public final class AndroidPath implements DiagramPath<AndroidPath> {

  private Path aPath = new Path();

  @Override
  public AndroidPath moveTo(double pX, double pY) {
    aPath.moveTo((float)pX, (float) pY);
    return this;
  }

  @Override
  public AndroidPath lineTo(double pX, double pY) {
    aPath.lineTo((float)pX, (float) pY);
    return this;
  }

  @Override
  public void close() {
    aPath.close();
  }

  public Path getPath() {
    return aPath;
  }

}
