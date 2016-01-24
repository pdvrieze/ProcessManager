package android.util;

/**
 * Very simple mock class that is needed because the code will call this on error and it is not implemented in the
 * offline testing.
 *
 * Created by pdvrieze on 14/11/15.
 */
public class Log {

  public static int e (String tag, String msg) {
    return e(tag, msg, null);
  }

  public static int e (String tag, String msg, Throwable tr) {
    System.err.println("ERROR: "+tag + " -- " + msg);
    if (tr!=null) {
      tr.printStackTrace(System.err);
    }
    return 0;
  }

  public static int d(String tag, String msg) {
    return d(tag, msg, null);
  }

  public static int d (String tag, String msg, Throwable tr) {
    System.err.println("DEBUG: "+tag + " -- " + msg);
    if (tr!=null) {
      tr.printStackTrace(System.err);
    }
    return 0;
  }


}
