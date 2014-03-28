package nl.adaptivity.process.editor.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nl.adaptivity.android.graphics.BackgroundDrawable;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.android.*;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.ProcessThemeItems;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import static nl.adaptivity.diagram.android.RelativeLightView.*;

/**
 * The MyDiagramAdapter to use for the editor.
 * @author Paul de Vrieze
 *
 */
public class MyDiagramAdapter implements DiagramAdapter<LWDrawableView, DrawableProcessNode> {


  private class ConnectorView implements LightView {

    private Paint aPen;

    @Override
    public void setFocussed(boolean pFocussed) { /* ignore */ }

    @Override
    public boolean isFocussed() { return false; }

    @Override
    public void setSelected(boolean pSelected) { /* ignore */ }

    @Override
    public boolean isSelected() { return false; }

    @Override
    public void setTouched(boolean pB) { /* ignore */ }

    @Override
    public boolean isTouched() { return false; }

    @Override
    public void getBounds(RectF pDest) {
      MyDiagramAdapter.this.getBounds(pDest);
    }

    @Override
    public void move(float pX, float pY) { /* ignore */ }

    @Override
    public void setPos(float pX, float pY) { /* ignore */ }

    @Override
    public void draw(Canvas pCanvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> pTheme, double pScale) {
      if (aPen ==null) { aPen = pTheme.getPen(ProcessThemeItems.LINE, nl.adaptivity.diagram.Drawable.STATE_DEFAULT).getPaint(); }
      for(DrawableProcessNode start:aDiagram.getModelNodes()) {
        if (! (Double.isNaN(start.getX())|| Double.isNaN(start.getY()))) {
          for (DrawableProcessNode end: start.getSuccessors()) {
            if (! (Double.isNaN(end.getX())|| Double.isNaN(end.getY()))) {
              final float x1 = (float) ((start.getBounds().right()-DrawableProcessModel.STROKEWIDTH)*pScale);
              final float y1 = (float) (start.getY()*pScale);
              final float x2 = (float) ((end.getBounds().left+DrawableProcessModel.STROKEWIDTH)*pScale);
              final float y2 = (float) (end.getY()* pScale);
              pCanvas.drawLine(x1, y1, x2, y2, aPen);
            }
          }
        }
      }
    }

  }

  private static final double DECORATION_VSPACING = 12d;
  private static final double DECORATION_HSPACING = 16d;

  private DrawableProcessModel aDiagram;
  private List<LWDrawableView> aViewCache;
  private LightView aBackground;
  private LightView aOverlay;
  private RectF aBounds = new RectF();
  private boolean aInvalid = true;
  private AndroidTheme aTheme;
  private Context aContext;
  private RelativeLightView[] aCachedDecorations = new RelativeLightView[3];
  private int aCachedDecorationPos = -1;

  public MyDiagramAdapter(Context pContext, DrawableProcessModel pDiagram) {
    aContext = pContext;
    aDiagram = pDiagram;
    aViewCache = new ArrayList<>(pDiagram.getModelNodes().size());
  }

  @Override
  public int getCount() {
    return aDiagram.getModelNodes().size();
  }

  @Override
  public DrawableProcessNode getItem(int pos) {
    return aDiagram.getModelNodes().get(pos);
  }

  @Override
  public LWDrawableView getView(int pPosition) {
    for(int i=pPosition-aViewCache.size();i>=0;--i) { aViewCache.add(null); }
    LWDrawableView result = aViewCache.get(pPosition);
    if (result!=null) {
      return result;
    }
    result = new LWDrawableView(getItem(pPosition));
    aViewCache.set(pPosition, result);
    return result;
  }



  @Override
  public List<? extends RelativeLightView> getRelativeDecorations(int pPosition, double pScale, boolean pSelected) {
    if (! pSelected) {
      if (pPosition==aCachedDecorationPos) { aCachedDecorationPos=-1; }
      return Collections.emptyList();
    }
    if (aCachedDecorationPos !=pPosition) {
      aCachedDecorationPos = pPosition;
      aCachedDecorations[0] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), pScale), BOTTOM| HGRAVITY);
      aCachedDecorations[1] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_edit), pScale), BOTTOM| HGRAVITY);
      aCachedDecorations[2] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_arrow), pScale), BOTTOM| HGRAVITY);
    }
    final DrawableProcessNode drawableProcessNode = aDiagram.getModelNodes().get(pPosition);
    double centerX = drawableProcessNode.getX();
    double topY = drawableProcessNode.getBounds().bottom()+DECORATION_VSPACING/pScale;
    layoutHorizontal(centerX, topY, pScale, aCachedDecorations);
    return Arrays.asList(aCachedDecorations);
  }

  private static void layoutHorizontal(double pCenterX, double pTop, double pScale, LightView[] pDecorations) {
    if (pDecorations.length==0) { return; }
    double hspacing = DECORATION_HSPACING/pScale;
    double totalWidth = -hspacing;

    RectF bounds = new RectF();
    for(LightView decoration: pDecorations) {
      decoration.getBounds(bounds);
      totalWidth+=bounds.width()+hspacing;
    }
    float left = (float) (pCenterX-totalWidth/2);
    float top = (float) pTop;
    for(LightView decoration: pDecorations) {
      decoration.setPos(left, top);
      decoration.getBounds(bounds);
      left+=bounds.width()+hspacing;
    }
  }

  private Drawable loadDrawable(int pResId) {
    // TODO get the button drawable out of the style.
    return new BackgroundDrawable(aContext, R.drawable.btn_context, pResId);
  }

  @Override
  public LightView getBackground() {
    if (aBackground==null) { aBackground = new ConnectorView(); }
    return aBackground;
  }

  @Override
  public LightView getOverlay() {
    return aOverlay;
  }

  public void notifyDatasetChanged() {
    aInvalid = true;
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
    if (pDecoration==aCachedDecorations[0]) {
      aDiagram.removeNode(pPosition);
      if (aCachedDecorationPos==pPosition) {
        aCachedDecorationPos=-1;
      } else if (aCachedDecorationPos>pPosition) {
        --aCachedDecorationPos;
      }
      pView.invalidate();
    } else if (pDecoration==aCachedDecorations[1]) {
      doEditNode(pPosition);
    }
  }

  private void doEditNode(int pPosition) {
    // TODO Auto-generated method stub
    //
  }

  @Override
  public void onDecorationMove(DiagramView pView, int pPosition, RelativeLightView pDecoration, float pX, float pY) {
    DrawableProcessNode start = getItem(pPosition);
    final float x1 = (float) (start.getBounds().right()-DrawableProcessModel.STROKEWIDTH);
    final float y1 = (float) (start.getY());
    final float x2 = pX;
    final float y2 = pY;

    if (aOverlay instanceof LineView) {
      pView.invalidate(aOverlay); // invalidate both old
      ((LineView) aOverlay).setPos(x1,y1, x2,y2);
    } else {
      aOverlay = new LineView(x1,y1, x2,y2);
    }
    pView.invalidate(aOverlay); // and new bounds
  }

  @Override
  public void onDecorationUp(DiagramView pView, int pPosition, RelativeLightView pDecoration, float pX, float pY) {
    if (aOverlay instanceof LineView) {
      pView.invalidate(aOverlay);
      aOverlay = null;
    }
  }

}
