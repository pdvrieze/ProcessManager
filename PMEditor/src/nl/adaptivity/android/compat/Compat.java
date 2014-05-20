package nl.adaptivity.android.compat;

import java.io.IOException;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


public class Compat {


  @TargetApi(Build.VERSION_CODES.KITKAT)
  private static class Compat19 {

    public static void closeWithError(ParcelFileDescriptor pfd, String error) {
      try {
        pfd.closeWithError(error);
      } catch (IOException e) {
        Log.e(Compat.class.getSimpleName(), error, e);
      }
    }

  }

  public static void postInvalidateOnAnimation(View view) {
    ViewCompat.postInvalidateOnAnimation(view);
  }

  public static boolean isZoomIn(MotionEvent pEvent) {
    if (Build.VERSION.SDK_INT>=12) {
      return Compat12.isZoomIn(pEvent);
    }
    return false;
  }

  public static void closeWithError(ParcelFileDescriptor pfd, String error) {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT) {
      Compat19.closeWithError(pfd, error);
    } else {
      try {
        pfd.close();
        Log.e(Compat.class.getSimpleName(), error, new IOException(error));
      } catch (IOException e) {
        Log.e(Compat.class.getSimpleName(), error, e);
      }
    }
  }

}
