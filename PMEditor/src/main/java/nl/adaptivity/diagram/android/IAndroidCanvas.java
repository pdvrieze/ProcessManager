package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Rectangle;
import android.graphics.Bitmap;


public interface IAndroidCanvas extends nl.adaptivity.diagram.Canvas<AndroidStrategy, AndroidPen, AndroidPath>{
  IAndroidCanvas scale(double scale);
  IAndroidCanvas translate(double left, double right);
  @Override
  IAndroidCanvas childCanvas(Rectangle area, double scale);

  void drawBitmap(double left, double top, Bitmap bitmap, AndroidPen pen);
}
