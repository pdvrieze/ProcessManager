package nl.adaptivity.android.compat;

import android.annotation.TargetApi;
import android.view.MotionEvent;

@TargetApi(12)
public class Compat12 {

  public static boolean isZoomIn(MotionEvent pEvent) {
    return pEvent.getAxisValue(MotionEvent.AXIS_VSCROLL)<0f;
    
  }

}
