/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.editor.android;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
import nl.adaptivity.android.graphics.BackgroundDrawable;
import nl.adaptivity.android.graphics.LineView;
import nl.adaptivity.diagram.android.AndroidDrawableLightView;
import nl.adaptivity.diagram.android.DiagramView;
import nl.adaptivity.diagram.android.LightView;
import nl.adaptivity.diagram.android.RelativeLightView;
import nl.adaptivity.process.diagram.*;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identified;

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

  private       LightView mOverlay;
  private final Context   mContext;
  private final RelativeLightView[] mCachedDecorations      = new RelativeLightView[3];
  private final RelativeLightView[] mCachedStartDecorations = new RelativeLightView[2];
  private final RelativeLightView[] mCachedEndDecorations   = new RelativeLightView[1];
  private       DrawableProcessNode.Builder mCachedDecorationItem   = null;
  private       int                 mConnectingItem         = -1;

  public MyDiagramAdapter(final Context context, final DrawableProcessModel.Builder diagram) {
    super (diagram);
    mContext = context;
  }

  @Override
  public List<? extends RelativeLightView> getRelativeDecorations(final int position, final double scale, final boolean selected) {
    if (! selected) {
//      if (pPosition>=0) {
//        DrawableProcessNode item = getItem(pPosition);
//        if (item.equals(mCachedDecorationItem)) { mCachedDecorationItem = null; }
//      } else {
//        mCachedDecorationItem=null;
//      }
      return Collections.emptyList();
    }

    final DrawableProcessNode.Builder drawableProcessNode = getItem(position);

    final RelativeLightView[] decorations;
    if (drawableProcessNode instanceof StartNode) {
      decorations = getStartDecorations(drawableProcessNode, scale);
    } else if (drawableProcessNode instanceof EndNode) {
      decorations = getEndDecorations(drawableProcessNode, scale);
    } else {
      decorations = getDefaultDecorations(drawableProcessNode, scale);
    }

    final double centerX = drawableProcessNode.getX();
    final double topY = drawableProcessNode.getBounds().bottom() + DECORATION_VSPACING / scale;
    layoutHorizontal(centerX, topY, scale, decorations);
    return Arrays.asList(decorations);
  }

  private RelativeLightView[] getDefaultDecorations(final DrawableProcessNode.Builder item, final double scale) {
    if (! item.equals(mCachedDecorationItem)) {
      mCachedDecorationItem = item;
      mCachedDecorations[0] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), scale), BOTTOM| HGRAVITY);
      mCachedDecorations[1] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_edit), scale), BOTTOM| HGRAVITY);
      mCachedDecorations[2] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_arrow), scale), BOTTOM| HGRAVITY);
    }
    return mCachedDecorations;
  }

  private RelativeLightView[] getStartDecorations(final DrawableProcessNode.Builder item, final double scale) {
    if (! item.equals(mCachedDecorationItem)) {
      mCachedDecorationItem = item;
      // Assign to both caches to allow click to remain working.
      mCachedDecorations[0] = mCachedStartDecorations[0] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), scale), BOTTOM| HGRAVITY);
      mCachedDecorations[2] = mCachedStartDecorations[1] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_arrow), scale), BOTTOM| HGRAVITY);
    }
    return mCachedStartDecorations;
  }

  private RelativeLightView[] getEndDecorations(final DrawableProcessNode.Builder item, final double scale) {
    if (! item.equals(mCachedDecorationItem)) {
      mCachedDecorationItem = item;
      // Assign to both caches to allow click to remain working.
      mCachedDecorations[0] = mCachedEndDecorations[0] = new RelativeLightView(new AndroidDrawableLightView(loadDrawable(R.drawable.ic_cont_delete), scale), BOTTOM| HGRAVITY);
    }
    return mCachedEndDecorations;
  }

  private static void layoutHorizontal(final double centerX, final double top, final double scale, final RelativeLightView[] decorations) {
    if (decorations.length==0) { return; }
    final double hspacing = DECORATION_HSPACING / scale;
    double totalWidth = -hspacing;

    final RectF bounds = new RectF();
    for(final LightView decoration: decorations) {
      decoration.getBounds(bounds);
      totalWidth+=bounds.width()+hspacing;
    }
    float leftF = (float) (centerX-totalWidth/2);
    final float topF = (float) top;
    for(final RelativeLightView decoration: decorations) {
      decoration.setPos(leftF, topF);
      decoration.getBounds(bounds);
      leftF+=bounds.width()+hspacing;
    }
  }

  private Drawable loadDrawable(final int resId) {
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
  public void onDecorationClick(final DiagramView view, final int position, final LightView decoration) {
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

  private void removeNode(final int position) {
    final DrawableProcessNode.Builder item = getItem(position);
    mViewCache.remove(item);
    getDiagram().getNodes().remove(item);
    if (item.equals(mCachedDecorationItem)) {
      mCachedDecorationItem=null;
    }
  }

  private void doEditNode(final int position) {
    if (mContext instanceof Activity) {
      final DrawableProcessNode.Builder node = getDiagram().getChildElements().get(position);
      final DialogFragment fragment;
      if (node instanceof DrawableJoinSplit) {
        fragment = JoinSplitNodeEditDialogFragment.newInstance(position);
      } else if (node instanceof DrawableActivity) {
        fragment = ActivityEditDialogFragment.newInstance(position);
      } else {
        fragment = NodeEditDialogFragment.newInstance(position);
      }
      fragment.show(((Activity)mContext).getFragmentManager(), "editNode");
    }
  }

  @Override
  public void onDecorationMove(final DiagramView view, final int position, final RelativeLightView decoration, final float x, final float y) {
    if (decoration==mCachedDecorations[2]) {
      final DrawableProcessNode.Builder start = mCachedDecorationItem;
      final float x1 = (float) (start.getBounds().right() - RootDrawableProcessModel.STROKEWIDTH);
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
  public void onDecorationUp(final DiagramView view, final int position, final RelativeLightView decoration, final float diagX, final float diagY) {
    if (decoration==mCachedDecorations[2]) {
      if (mOverlay instanceof LineView) {
        view.invalidate(mOverlay);
        mOverlay = null;
      }

      DrawableProcessNode.Builder next=null;
      for(final DrawableProcessNode.Builder item: getDiagram().getChildElements()) {
        if(item.getItemAt(diagX, diagY) != null) {
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
  public boolean onNodeClickOverride(final DiagramView diagramView, final int touchedElement, final MotionEvent e) {
    if (mConnectingItem>=0) {
      final DrawableProcessNode.Builder prev = getItem(mConnectingItem);
      final DrawableProcessNode.Builder next = getItem(touchedElement);
      tryAddSuccessor(prev, next);
      mConnectingItem=-1;
      mCachedDecorations[2].setActive(false);
      diagramView.invalidate();
      return true;
    }
    return false;
  }

  public void tryAddSuccessor(final DrawableProcessNode.Builder prev, final DrawableProcessNode.Builder next) {
    if (prev.getSuccessors().contains(next)) {
      prev.getSuccessors().remove(next);
    } else {

      if (prev.getSuccessors().size() < prev.getMaxSuccessorCount() && next.getPredecessors().size() < next.getMaxPredecessorCount()) {
        try {
          if (prev instanceof Split) {
            final Split split = (Split) prev;
            if (split.getMin() >= split.getMax()) {
              split.setMin(split.getMin()+1);
            }
            if (split.getMax() >= prev.getSuccessors().size()) {
              split.setMax(split.getMax() + 1);
            }
          }
          if (next instanceof Join) {
            final Join join = (Join) next;
            if (join.getMin() >= join.getMax()) {
              join.setMin(join.getMin()+1);
            }
            if (join.getMax() >= prev.getPredecessors().size()) {
              join.setMax(join.getMax() + 1);
            }
          }
          prev.getSuccessors().add(next.getIdentifier());
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

}
