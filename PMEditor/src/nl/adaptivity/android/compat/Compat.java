package nl.adaptivity.android.compat;

import android.os.Build;
import android.view.View;


public class Compat {
  
  public static void postInvalidateOnAnimation(View view) {
    if (Build.VERSION.SDK_INT>16) {
      Compat16.postInvalidateOnAnimation(view);
    } else {
      view.postInvalidateDelayed(10);
    }
  }

}
