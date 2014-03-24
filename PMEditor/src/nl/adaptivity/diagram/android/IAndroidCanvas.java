package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Rectangle;
import android.graphics.Bitmap;


public interface IAndroidCanvas extends nl.adaptivity.diagram.Canvas<AndroidStrategy, AndroidPen, AndroidPath>{
  IAndroidCanvas scale(double pScale);
  @Override
  IAndroidCanvas childCanvas(Rectangle pArea, double pScale);
  void drawBitmap(double pLeft, double pTop, Bitmap pBitmap, AndroidPen pPen);
}
