package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.ThemeItem;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
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
      paint.setColorFilter(new ColorMatrixColorFilter(BLUE_FILTER));
      paint.setMaskFilter(new BlurMaskFilter(15f, Blur.NORMAL));
      return result;
    }

  },
  ;

  private static final float[] BLUE_FILTER=new float[] { 0, 0, 0, 0.2f, 0,
                                                         0, 0, 0, 0.2f, 0,
                                                         0, 0, 0, 0.8f, 0,
                                                         0, 0, 0, 1, 0} ;


  @Override
  public int getItemNo() {
    return 100+ordinal();
  }

  @Override
  public int getEffectiveState(int pState) {
    return 0;
  }

}
