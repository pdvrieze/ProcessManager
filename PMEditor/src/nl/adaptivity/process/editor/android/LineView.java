package nl.adaptivity.process.editor.android;

import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.android.AndroidPath;
import nl.adaptivity.diagram.android.AndroidPen;
import nl.adaptivity.diagram.android.AndroidStrategy;
import nl.adaptivity.diagram.android.LightView;
import nl.adaptivity.process.diagram.ProcessThemeItems;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;


public class LineView implements LightView {

  private float aX1;
  private float aY1;
  private float aX2;
  private float aY2;
  private boolean aFocussed;
  private boolean aSelected;
  private boolean aTouched;

  public LineView(float x1, float y1, float x2, float y2) {
    aX1 = x1;
    aY1 = y1;
    aX2 = x2;
    aY2 = y2;
  }

  @Override
  public void setFocussed(boolean pFocussed) {
    aFocussed = pFocussed;
  }

  @Override
  public boolean isFocussed() {
    return aFocussed;
  }

  @Override
  public void setSelected(boolean pSelected) {
    aSelected = pSelected;
  }

  @Override
  public boolean isSelected() {
    return aSelected;
  }

  @Override
  public void setTouched(boolean pTouched) {
    aTouched = pTouched;
  }

  @Override
  public boolean isTouched() {
    return aTouched;
  }

  @Override
  public void getBounds(RectF pTarget) {
    if (aX1<=aX2) {
      pTarget.left =aX1;
      pTarget.right=aX2;
    } else {
      pTarget.left =aX2;
      pTarget.right=aX1;
    }
    if (aY1<=aY2) {
      pTarget.top =aY1;
      pTarget.bottom=aY2;
    } else {
      pTarget.top =aY2;
      pTarget.bottom=aY1;
    }
  }

  @Override
  public void draw(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, double pScale) {
    Paint paint = pTheme.getPen(ProcessThemeItems.LINE, 0).getPaint();

    float x1, x2, y1, y2;
    if (aX1<=aX2) { x1=0; x2=(float) ((aX2-aX1)*pScale); } else { x2=0; x1=(float) ((aX1-aX2)*pScale); }
    if (aY1<=aY2) { y1=0; y2=(float) ((aY2-aY1)*pScale); } else { y2=0; y1=(float) ((aY1-aY2)*pScale); }
    pCanvas.drawLine(x1, y1, x2, y2, paint);
  }

  @Override
  public void move(float pX, float pY) {
    aX1+=pX;
    aX2+=pX;
    aY1+=pY;
    aY2+=pY;
  }

  @Override
  public void setPos(float pLeft, float pTop) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setPos(float pX1, float pY1, float pX2, float pY2) {
    aX1 = pX1;
    aY1 = pY1;
    aX2 = pX2;
    aY2 = pY2;
  }

}
