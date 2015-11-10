package nl.adaptivity.process.editor.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static nl.adaptivity.diagram.android.RelativeLightView.BOTTOM;
import static nl.adaptivity.diagram.android.RelativeLightView.HGRAVITY;

/**
 * The MyDiagramAdapter to use for the editor.
 * @author Paul de Vrieze
 *
 */
public class MyDiagramAdapter extends BaseProcessAdapter {


  private static final double DECORATION_VSPACING = 12d;
  private static final double DECORATION_HSPACING = 12d;

  private LightView mOverlay;
  private Context mContext;
  private RelativeLightView[] mCachedDecorations = new RelativeLightView[3];
  private RelativeLightView[] mCachedStartDecorations = new RelativeLightView[2];
  private RelativeLightView[] mCachedEndDecorations = new RelativeLightView[1];
  private DrawableProcessNode mCachedDecorationItem = null;
  private int mConnectingItem = -1;

  public MyDiagramAdapter(Context context, DrawableProcessModel diagram) {
    super (diagram);
    mContext = context;
  }

  @Override
  public List<? extends RelativeLightView> getRelativeDecorations(int position, double scale, boolean selected) {
    if (! selected) {
//      if (pPosition>=0) {
//        DrawableProcessNode item = getItem(pPosition);
//        if (item.equals(mCachedDecorationItem)) { mCachedDecorationItem = null; }
//      } else {
//        mCachedDecorationItem=null;
//      }
      return Collections.emptyList();
    }

    final DrawableProcessNode drawableProcessNode = getItem(position);

    final RelativeLightView[] decorations;
    if (drawableProcessNode instanceof StartNode) {
      decorations = getStartDecorations(drawableProcessNode, scale);
    } else if (drawableProcessNode instanceof EndNode) {
      decorations = getEndDecorations(drawableProcessNode, scale);
    } else {
      decorations = getDefaultDecorations(drawableProcessNode, scale);
    }

    double centerX = drawableProcessNode.getX();
    double topY = drawableProcessNode.getBounds().bottom()+DECORATION_VSPACING/scale;
    layoutHorizontal(centerX, topY, scale, decorations);
    return Arrays.asList(decorations);
  }

  private RelativeLightView[] getDefaultDecorations(DrawableProcessNode item, double scale) {
    if (! item.equals(mCachedDecorationItem)) {
      mCachedDecorationItem = item;
      mCachedDecorations[0] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), scale), BOTTOM| HGRAVITY);
      mCachedDecorations[1] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_edit), scale), BOTTOM| HGRAVITY);
      mCachedDecorations[2] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_arrow), scale), BOTTOM| HGRAVITY);
    }
    return mCachedDecorations;
  }

  private RelativeLightView[] getStartDecorations(DrawableProcessNode item, double scale) {
    if (! item.equals(mCachedDecorationItem)) {
      mCachedDecorationItem = item;
      // Assign to both caches to allow click to remain working.
      mCachedDecorations[0] = mCachedStartDecorations[0] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), scale), BOTTOM| HGRAVITY);
      mCachedDecorations[2] = mCachedStartDecorations[1] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_arrow), scale), BOTTOM| HGRAVITY);
    }
    return mCachedStartDecorations;
  }

  private RelativeLightView[] getEndDecorations(DrawableProcessNode item, double scale) {
    if (! item.equals(mCachedDecorationItem)) {
      mCachedDecorationItem = item;
      // Assign to both caches to allow click to remain working.
      mCachedDecorations[0] = mCachedEndDecorations[0] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), scale), BOTTOM| HGRAVITY);
    }
    return mCachedEndDecorations;
  }

  private static void layoutHorizontal(double centerX, double top, double scale, LightView[] decorations) {
    if (decorations.length==0) { return; }
    double hspacing = DECORATION_HSPACING/scale;
    double totalWidth = -hspacing;

    RectF bounds = new RectF();
    for(LightView decoration: decorations) {
      decoration.getBounds(bounds);
      totalWidth+=bounds.width()+hspacing;
    }
    float leftF = (float) (centerX-totalWidth/2);
    float topF = (float) top;
    for(LightView decoration: decorations) {
      decoration.setPos(leftF, topF);
      decoration.getBounds(bounds);
      leftF+=bounds.width()+hspacing;
    }
  }

  private Drawable loadDrawable(int resId) {
    // TODO get the button drawable out of the style.
    return new BackgroundDrawable(mContext, R.drawable.btn_context, resId);
  }

  @Override
  public LightView getOverlay() {
    return mOverlay;
  }

  public void notifyDatasetChanged() {
    invalidate();
  }

  @Override
  public void onDecorationClick(DiagramView view, int position, LightView decoration) {
    if (decoration==mCachedDecorations[0]) {
      removeNode(position);
      view.invalidate();
    } else if (decoration==mCachedDecorations[1]) {
      doEditNode(position);
    } else if (decoration==mCachedDecorations[2]) {
      if (mOverlay instanceof LineView) {
        view.invalidate(mOverlay);
        mOverlay = null;
      } else {
        decoration.setActive(true);
        mConnectingItem  = position;
      }
    }
  }

  private void removeNode(int position) {
    final DrawableProcessNode item = getItem(position);
    mViewCache.remove(item);
    getDiagram().removeNode(position);
    if (item.equals(mCachedDecorationItem)) {
      mCachedDecorationItem=null;
    }
  }

  private void doEditNode(int position) {
    if (mContext instanceof Activity) {
      NodeEditDialogFragment frag = new NodeEditDialogFragment();
      Bundle args = new Bundle(1);
      args.putInt(NodeEditDialogFragment.NODE_POS, position);
      frag.setArguments(args);
      frag.show(((Activity)mContext).getFragmentManager(), "editNode");
    }
    // TODO Auto-generated method stub
    //
  }

  @Override
  public void onDecorationMove(DiagramView view, int position, RelativeLightView decoration, float x, float y) {
    if (decoration==mCachedDecorations[2]) {
      DrawableProcessNode start = mCachedDecorationItem;
      final float x1 = (float) (start.getBounds().right()-DrawableProcessModel.STROKEWIDTH);
      final float y1 = (float) (start.getY());
      final float x2 = x;
      final float y2 = y;

      if (mOverlay instanceof LineView) {
        view.invalidate(mOverlay); // invalidate both old
        ((LineView) mOverlay).setPos(x1,y1, x2,y2);
      } else {
        mOverlay = new LineView(x1,y1, x2,y2);
      }
      view.invalidate(mOverlay); // and new bounds
    }
  }

  @Override
  public void onDecorationUp(DiagramView view, int position, RelativeLightView decoration, float x, float y) {
    if (decoration==mCachedDecorations[2]) {
      if (mOverlay instanceof LineView) {
        view.invalidate(mOverlay);
        mOverlay = null;
      }

      DrawableProcessNode next=null;
      for(DrawableProcessNode item: getDiagram().getModelNodes()) {
        if(item.getItemAt(x, y)!=null) {
          next = item;
          break;
        }
      }
      if (next!=null) {
        tryAddSuccessor(getItem(position), next);
      }
    }
  }

  @Override
  public boolean onNodeClickOverride(DiagramView diagramView, int touchedElement, MotionEvent e) {
    if (mConnectingItem>=0) {
      DrawableProcessNode prev = getItem(mConnectingItem);
      DrawableProcessNode next = getItem(touchedElement);
      if (prev.isPredecessorOf(next)) {
        prev.removeSuccessor(next);
      } else {
        tryAddSuccessor(prev, next);
      }
      mConnectingItem=-1;
      mCachedDecorations[2].setActive(false);
      diagramView.invalidate();
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
        Toast.makeText(mContext, "These can not be connected", Toast.LENGTH_LONG).show();
      }
    } else {
      Toast.makeText(mContext, "These can not be connected", Toast.LENGTH_LONG).show();
      // TODO Better errors
    }
  }

}
