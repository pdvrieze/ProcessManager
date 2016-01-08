package nl.adaptivity.android.compat;

import android.annotation.TargetApi;
import android.view.MotionEvent;

@TargetApi(12)
class Compat12 {

  public static boolean isZoomIn(MotionEvent event) {
    return event.getAxisValue(MotionEvent.AXIS_VSCROLL)<0f;
    
  }

}
