package nl.adaptivity.android.compat;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class Compat16 {

  public static void postInvalidateOnAnimation(View view) {
    view.postInvalidateOnAnimation();
  }

}
