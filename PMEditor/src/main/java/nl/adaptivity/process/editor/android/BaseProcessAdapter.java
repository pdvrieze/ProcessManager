package nl.adaptivity.process.editor.android;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.adaptivity.android.graphics.AbstractLightView;
import nl.adaptivity.android.graphics.LineView;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.android.AndroidPath;
import nl.adaptivity.diagram.android.AndroidPen;
import nl.adaptivity.diagram.android.AndroidStrategy;
import nl.adaptivity.diagram.android.AndroidTheme;
import nl.adaptivity.diagram.android.DiagramAdapter;
import nl.adaptivity.diagram.android.DiagramView;
import nl.adaptivity.diagram.android.LWDrawableView;
import nl.adaptivity.diagram.android.LightView;
import nl.adaptivity.diagram.android.RelativeLightView;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.ProcessThemeItems;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

public class BaseProcessAdapter implements DiagramAdapter<LWDrawableView, DrawableProcessNode> {

  protected static class ConnectorView extends AbstractLightView {

    private Paint aPen;
    private RectF aBounds = new RectF();
    private DrawableProcessModel aDiagram;

    public ConnectorView(BaseProcessAdapter pParent) {
      pParent.getBounds(aBounds);
      aDiagram = pParent.getDiagram();
    }

    @Override
    public void getBounds(RectF pDest) {
      pDest.set(aBounds);
    }

    @Override
    public void move(float pX, float pY) { /* ignore */ }

    @Override
    public void setPos(float pX, float pY) { /* ignore */ }

    @Override
    public void draw(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, double pScale) {
      if (aPen ==null) { aPen = pTheme.getPen(ProcessThemeItems.LINE, nl.adaptivity.diagram.Drawable.STATE_DEFAULT).getPaint(); }
      if (aDiagram==null) { return; }
      for(DrawableProcessNode start:aDiagram.getModelNodes()) {
        if (! (Double.isNaN(start.getX())|| Double.isNaN(start.getY()))) {
          for (DrawableProcessNode end: start.getSuccessors()) {
            if (! (Double.isNaN(end.getX())|| Double.isNaN(end.getY()))) {
              final float x1 = (float) ((start.getBounds().right()/*-DrawableProcessModel.STROKEWIDTH*/-aBounds.left)*pScale);
              final float y1 = (float) ((start.getY()-aBounds.top)*pScale);
              final float x2 = (float) ((end.getBounds().left/*+DrawableProcessModel.STROKEWIDTH*/-aBounds.left)*pScale);
              final float y2 = (float) ((end.getY()-aBounds.top)* pScale);
//              pCanvas.drawLine(x1, y1, x2, y2, aPen);
              LineView.drawArrow(pCanvas, pTheme, x1, y1, x2, y2, pScale);
            }
          }
        }
      }
    }

  }

  private DrawableProcessModel aDiagram;
  protected Map<DrawableProcessNode, LWDrawableView> aViewCache = new HashMap<>();
  private LightView aBackground;
  private RectF aBounds = new RectF();
  private boolean aInvalid = true;
  private AndroidTheme aTheme;

  public BaseProcessAdapter(DrawableProcessModel pDiagram) {
    aDiagram = pDiagram;
  }

  @Override
  public int getCount() {
    if (aDiagram==null) { return 0; }
    return aDiagram.getModelNodes().size();
  }

  @Override
  public DrawableProcessNode getItem(int pos) {
    return aDiagram.getModelNodes().get(pos);
  }

  @Override
  public LWDrawableView getView(int pPosition) {
    final DrawableProcessNode item = getItem(pPosition);
    LWDrawableView result = aViewCache.get(item);
    if (result!=null) {
      return result;
    }
    result = new LWDrawableView(item);
    aViewCache.put(item, result);
    return result;
  }

  @Override
  public List<? extends RelativeLightView> getRelativeDecorations(int pPosition, double pScale, boolean pSelected) {
    return Collections.emptyList();
  }

  @Override
  public LightView getBackground() {
    if (aBackground==null) { aBackground = new ConnectorView(this); }
    return aBackground;
  }

  @Override
  public LightView getOverlay() {
    return null;
  }

  @Override
  public void getBounds(RectF pDiagramBounds) {
    if (aInvalid) {
      final int len = getCount();
      if (len==0) {
        pDiagramBounds.set(0f, 0f, 0f, 0f);
        return;
      }
      DrawableProcessNode item = getItem(0);
      Rectangle bounds = item.getBounds();
      aBounds.set((float) bounds.left, (float) bounds.top, bounds.rightf(), bounds.bottomf());
      for(int i=1; i<len; ++i) {
        item = getItem(i);
        bounds = item.getBounds();
        aBounds.left=Math.min(aBounds.left, (float) bounds.left);
        aBounds.top=Math.min(aBounds.top, (float) bounds.top);
        aBounds.right=Math.max(aBounds.right, bounds.rightf());
        aBounds.bottom=Math.max(aBounds.bottom, bounds.bottomf());

      }
      aInvalid = false;
    }
    pDiagramBounds.set(aBounds);
  }

  @Override
  public AndroidTheme getTheme() {
    if (aTheme ==null) { aTheme = new AndroidTheme(AndroidStrategy.INSTANCE); }
    return aTheme;
  }

  @Override
  public void onDecorationClick(DiagramView pView, int pPosition, LightView pDecoration) {
    // ignore
  }

  @Override
  public void onDecorationMove(DiagramView pView, int pPosition, RelativeLightView pDecoration, float pX, float pY) {
    //ignore
  }

  @Override
  public void onDecorationUp(DiagramView pView, int pPosition, RelativeLightView pDecoration, float pX, float pY) {
    //ignore
  }

  @Override
  public boolean onNodeClickOverride(DiagramView pDiagramView, int pTouchedElement, MotionEvent pE) {
    //ignore
    return false;
  }

  @Override
  public double getGravityX(int pos) {
    return getItem(pos).getX();
  }

  @Override
  public double getGravityY(int pos) {
    return getItem(pos).getY();
  }

  public boolean isInvalid() {
    return aInvalid;
  }


  public void invalidate() {
    aInvalid = true;
  }

  public DrawableProcessModel getDiagram() {
    return aDiagram;
  }

}