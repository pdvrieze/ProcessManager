package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.DrawingStrategy;
import android.graphics.Paint;

public enum AndroidStrategy implements DrawingStrategy<AndroidStrategy, AndroidPen, AndroidPath>{
  INSTANCE
  ;

  @Override
  public AndroidPen newPen() {
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    return new AndroidPen(paint);
  }

  @Override
  public AndroidPath newPath() {
    return new AndroidPath();
  }

}