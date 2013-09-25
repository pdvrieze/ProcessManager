package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Color;
import android.graphics.Paint;


public class AndroidColor implements Color {

  private Paint aPaint;

  public AndroidColor(Paint pPaint) {
    aPaint = pPaint;
  }

  public Paint getPaint() {
    return aPaint;
  }

}
