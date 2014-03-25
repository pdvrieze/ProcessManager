package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.ThemeItem;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;


public enum AndroidExtraThemeItem implements ThemeItem {
  BLUR {

    @Override
    public <PEN_T extends Pen<PEN_T>> PEN_T createPen(DrawingStrategy<?, PEN_T, ?> pStrategy, int pState) {
      // This should always be an android pen.
      final PEN_T result = pStrategy.newPen();
      AndroidPen pen = (AndroidPen) result;
      final Paint paint = pen.getPaint();
      paint.setAntiAlias(true);
//      paint.setColorFilter(new ColorMatrixColorFilter(BLUE_FILTER));
//      paint.setMaskFilter(new BlurMaskFilter(16f, Blur.SOLID));
      paint.setShadowLayer(16f, 0, 0, SELECTED_SHADE_COLOR);
      return result;
    }

  },
  ;
  public static final float SHADER_RADIUS = 18f;

  public static final int TOUCHED_SHADE_COLOR=Color.argb(0xb0, 0xff, 0xec, 0x1a);
  public static final int SELECTED_SHADE_COLOR=Color.argb(0xb0, 23, 166, 255);

  private static final float[] BLUE_FILTER=new float[] { 0, 0, 0,    0, 23f,
                                                         0, 0, 0,    0, 166f,
                                                         0, 0, 0,    0, 255f,
                                                         0, 0, 0, 0.7f, 0} ;


  @Override
  public int getItemNo() {
    return 100+ordinal();
  }

  @Override
  public int getEffectiveState(int pState) {
    return 0;
  }

}
