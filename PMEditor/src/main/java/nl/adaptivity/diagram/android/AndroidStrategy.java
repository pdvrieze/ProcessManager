package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.DrawingStrategy;
import android.graphics.Paint;

public enum AndroidStrategy implements DrawingStrategy<AndroidStrategy, AndroidPen, AndroidPath>{
  INSTANCE
  ;

  @Override
  public AndroidPen newPen() {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setStrokeCap(Paint.Cap.SQUARE);
    paint.setAntiAlias(true);
    return new AndroidPen(paint);
  }

  @Override
  public AndroidPath newPath() {
    return new AndroidPath();
  }

}