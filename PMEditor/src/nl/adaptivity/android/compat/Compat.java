package nl.adaptivity.android.compat;

import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.view.MotionEvent;
import android.view.View;


public class Compat {

  public static void postInvalidateOnAnimation(View view) {
    ViewCompat.postInvalidateOnAnimation(view);
  }

  public static boolean isZoomIn(MotionEvent pEvent) {
    if (Build.VERSION.SDK_INT>=12) {
      return Compat12.isZoomIn(pEvent);
    }
    return false;
  }

}
