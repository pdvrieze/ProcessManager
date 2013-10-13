package nl.adaptivity.diagram.android;

import android.graphics.Path;
import nl.adaptivity.diagram.DiagramPath;


public class AndroidPath implements DiagramPath {

  private Path aPath = new Path();

  @Override
  public DiagramPath moveTo(double pX, double pY) {
    aPath.moveTo((float)pX, (float) pY);
    return this;
  }

  @Override
  public DiagramPath lineTo(double pX, double pY) {
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
