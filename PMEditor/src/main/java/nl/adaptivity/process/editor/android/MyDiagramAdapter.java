package nl.adaptivity.process.editor.android;

import static nl.adaptivity.diagram.android.RelativeLightView.BOTTOM;
import static nl.adaptivity.diagram.android.RelativeLightView.HGRAVITY;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nl.adaptivity.android.graphics.BackgroundDrawable;
import nl.adaptivity.android.graphics.LineView;
import nl.adaptivity.diagram.android.AndroidDrawableLightView;
import nl.adaptivity.diagram.android.DiagramView;
import nl.adaptivity.diagram.android.LightView;
import nl.adaptivity.diagram.android.RelativeLightView;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.IllegalProcessModelException;
import nl.adaptivity.process.processModel.StartNode;
import android.app.Activity;
import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

/**
 * The MyDiagramAdapter to use for the editor.
 * @author Paul de Vrieze
 *
 */
public class MyDiagramAdapter extends BaseProcessAdapter {


  private static final double DECORATION_VSPACING = 12d;
  private static final double DECORATION_HSPACING = 12d;

  private LightView aOverlay;
  private Context aContext;
  private RelativeLightView[] aCachedDecorations = new RelativeLightView[3];
  private RelativeLightView[] aCachedStartDecorations = new RelativeLightView[2];
  private RelativeLightView[] aCachedEndDecorations = new RelativeLightView[1];
  private DrawableProcessNode aCachedDecorationItem = null;
  private int aConnectingItem = -1;

  public MyDiagramAdapter(Context pContext, DrawableProcessModel pDiagram) {
    super (pDiagram);
    aContext = pContext;
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

    final DrawableProcessNode drawableProcessNode = getItem(pPosition);

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
  public LightView getOverlay() {
    return aOverlay;
  }

  public void notifyDatasetChanged() {
    invalidate();
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
    getDiagram().removeNode(pPosition);
    if (item.equals(aCachedDecorationItem)) {
      aCachedDecorationItem=null;
    }
  }

  private void doEditNode(int pPosition) {
    if (aContext instanceof Activity) {
      NodeEditDialogFragment frag = new NodeEditDialogFragment();
      Bundle args = new Bundle(1);
      args.putInt(NodeEditDialogFragment.NODE_POS, pPosition);
      frag.setArguments(args);
      frag.show(((Activity)aContext).getFragmentManager(), "editNode");
    }
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
      for(DrawableProcessNode item: getDiagram().getModelNodes()) {
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
