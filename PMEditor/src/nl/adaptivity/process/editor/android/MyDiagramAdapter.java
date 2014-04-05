package nl.adaptivity.process.editor.android;

import static nl.adaptivity.diagram.android.RelativeLightView.BOTTOM;
import static nl.adaptivity.diagram.android.RelativeLightView.HGRAVITY;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.adaptivity.android.graphics.AbstractLightView;
import nl.adaptivity.android.graphics.BackgroundDrawable;
import nl.adaptivity.android.graphics.LineView;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.android.*;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.ProcessThemeItems;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.StartNode;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

/**
 * The MyDiagramAdapter to use for the editor.
 * @author Paul de Vrieze
 *
 */
public class MyDiagramAdapter implements DiagramAdapter<LWDrawableView, DrawableProcessNode> {


  private class ConnectorView extends AbstractLightView {

    private Paint aPen;

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

  private static final double DECORATION_VSPACING = 12d;
  private static final double DECORATION_HSPACING = 12d;

  private DrawableProcessModel aDiagram;
  private Map<DrawableProcessNode, LWDrawableView> aViewCache;
  private LightView aBackground;
  private LightView aOverlay;
  private RectF aBounds = new RectF();
  private boolean aInvalid = true;
  private AndroidTheme aTheme;
  private Context aContext;
  private RelativeLightView[] aCachedDecorations = new RelativeLightView[3];
  private RelativeLightView[] aCachedStartDecorations = new RelativeLightView[2];
  private RelativeLightView[] aCachedEndDecorations = new RelativeLightView[1];
  private DrawableProcessNode aCachedDecorationItem = null;
  private int aConnectingItem = -1;

  public MyDiagramAdapter(Context pContext, DrawableProcessModel pDiagram) {
    aContext = pContext;
    aDiagram = pDiagram;
    aViewCache = new HashMap<>();
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
  public double getGravityX(int pos) {
    return getItem(pos).getX();
  }

  @Override
  public double getGravityY(int pos) {
    return getItem(pos).getY();
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
    if (! pSelected) {
//      if (pPosition>=0) {
//        DrawableProcessNode item = getItem(pPosition);
//        if (item.equals(aCachedDecorationItem)) { aCachedDecorationItem = null; }
//      } else {
//        aCachedDecorationItem=null;
//      }
      return Collections.emptyList();
    }

    final DrawableProcessNode drawableProcessNode = aDiagram.getModelNodes().get(pPosition);

    final RelativeLightView[] decorations;
    if (drawableProcessNode instanceof StartNode) {
      decorations = getStartDecorations(drawableProcessNode, pScale);
    } else if (drawableProcessNode instanceof EndNode) {
      decorations = getEndDecorations(drawableProcessNode, pScale);
    } else {
      decorations = getDefaultDecorations(drawableProcessNode, pScale);
    }

    double centerX = drawableProcessNode.getX();
    double topY = drawableProcessNode.getBounds().bottom()+DECORATION_VSPACING/pScale;
    layoutHorizontal(centerX, topY, pScale, decorations);
    return Arrays.asList(decorations);
  }

  private RelativeLightView[] getDefaultDecorations(DrawableProcessNode item, double pScale) {
    if (! item.equals(aCachedDecorationItem)) {
      aCachedDecorationItem = item;
      aCachedDecorations[0] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), pScale), BOTTOM| HGRAVITY);
      aCachedDecorations[1] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_edit), pScale), BOTTOM| HGRAVITY);
      aCachedDecorations[2] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_arrow), pScale), BOTTOM| HGRAVITY);
    }
    return aCachedDecorations;
  }

  private RelativeLightView[] getStartDecorations(DrawableProcessNode item, double pScale) {
    if (! item.equals(aCachedDecorationItem)) {
      aCachedDecorationItem = item;
      // Assign to both caches to allow click to remain working.
      aCachedDecorations[0] = aCachedStartDecorations[0] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), pScale), BOTTOM| HGRAVITY);
      aCachedDecorations[2] = aCachedStartDecorations[1] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_arrow), pScale), BOTTOM| HGRAVITY);
    }
    return aCachedStartDecorations;
  }

  private RelativeLightView[] getEndDecorations(DrawableProcessNode item, double pScale) {
    if (! item.equals(aCachedDecorationItem)) {
      aCachedDecorationItem = item;
      // Assign to both caches to allow click to remain working.
      aCachedDecorations[0] = aCachedEndDecorations[0] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), pScale), BOTTOM| HGRAVITY);
    }
    return aCachedEndDecorations;
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
      removeNode(pPosition);
      pView.invalidate();
    } else if (pDecoration==aCachedDecorations[1]) {
      doEditNode(pPosition);
    } else if (pDecoration==aCachedDecorations[2]) {
      if (aOverlay instanceof LineView) {
        pView.invalidate(aOverlay);
        aOverlay = null;
      } else {
        pDecoration.setActive(true);
        aConnectingItem  = pPosition;
      }
    }
  }

  private void removeNode(int pPosition) {
    final DrawableProcessNode item = getItem(pPosition);
    aViewCache.remove(item);
    aDiagram.removeNode(pPosition);
    if (item.equals(aCachedDecorationItem)) {
      aCachedDecorationItem=null;
    }
  }

  private void doEditNode(int pPosition) {
    // TODO Auto-generated method stub
    //
  }

  @Override
  public void onDecorationMove(DiagramView pView, int pPosition, RelativeLightView pDecoration, float pX, float pY) {
    if (pDecoration==aCachedDecorations[2]) {
      DrawableProcessNode start = aCachedDecorationItem;
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
  }

  @Override
  public void onDecorationUp(DiagramView pView, int pPosition, RelativeLightView pDecoration, float pX, float pY) {
    if (pDecoration==aCachedDecorations[2]) {
      if (aOverlay instanceof LineView) {
        pView.invalidate(aOverlay);
        aOverlay = null;
      }

      DrawableProcessNode next=null;
      for(DrawableProcessNode item: aDiagram.getModelNodes()) {
        if(item.getItemAt(pX, pY)!=null) {
          next = item;
          break;
        }
      }
      if (next!=null) {
        tryAddSuccessor(getItem(pPosition), next);
      }
    }
  }

  @Override
  public boolean onNodeClickOverride(DiagramView pDiagramView, int pTouchedElement, MotionEvent pE) {
    if (aConnectingItem>=0) {
      DrawableProcessNode prev = getItem(aConnectingItem);
      DrawableProcessNode next = getItem(pTouchedElement);
      if (prev.isPredecessorOf(next)) {
        prev.removeSuccessor(next);
      } else {
        tryAddSuccessor(prev, next);
      }
      aConnectingItem=-1;
      aCachedDecorations[2].setActive(false);
      pDiagramView.invalidate();
      return true;
    }
    return false;
  }

  public void tryAddSuccessor(DrawableProcessNode prev, DrawableProcessNode next) {
    if (prev.getSuccessors().size()<prev.getMaxSuccessorCount() &&
        next.getPredecessors().size()<next.getMaxPredecessorCount()) {
      try {
        prev.addSuccessor(next);
      } catch (IllegalProcessModelException e) {
        Log.w(MyDiagramAdapter.class.getName(), e.getMessage(), e);
        Toast.makeText(aContext, "These can not be connected", Toast.LENGTH_LONG).show();
      }
    } else {
      Toast.makeText(aContext, "These can not be connected", Toast.LENGTH_LONG).show();
      // TODO Better errors
    }
  }

}
