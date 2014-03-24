package nl.adaptivity.process.editor.android;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.android.AndroidPath;
import nl.adaptivity.diagram.android.AndroidPen;
import nl.adaptivity.diagram.android.AndroidStrategy;
import nl.adaptivity.diagram.android.AndroidTheme;
import nl.adaptivity.diagram.android.DiagramAdapter;
import nl.adaptivity.diagram.android.LWDrawableView;
import nl.adaptivity.diagram.android.LightView;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.ProcessThemeItems;

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
    public boolean getFocussed() { return false; }

    @Override
    public void setSelected(boolean pSelected) { /* ignore */ }

    @Override
    public boolean getSelected() { return false; }

    @Override
    public void setTouched(boolean pB) { /* ignore */ }

    @Override
    public boolean getTouched() { return false; }

    @Override
    public void getBounds(RectF pDest) {
      MyDiagramAdapter.this.getBounds(pDest);
    }

    @Override
    public <S extends DrawingStrategy<S, AndroidPen, AndroidPath>> void draw(Canvas pCanvas, Theme<S, AndroidPen, AndroidPath> pTheme, double pScale) {
      if (aPen ==null) { aPen = pTheme.getPen(ProcessThemeItems.LINE, Drawable.STATE_DEFAULT).getPaint(); }
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

  private DrawableProcessModel aDiagram;
  private List<LWDrawableView> aViewCache;
  private LightView aBackground;
  private LightView aOverlay;
  private RectF aBounds = new RectF();
  private boolean aInvalid = true;
  private AndroidTheme aTheme;

  public MyDiagramAdapter(DrawableProcessModel pDiagram) {
    aDiagram = pDiagram;
    aViewCache = new ArrayList<LWDrawableView>(pDiagram.getModelNodes().size());
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

}
