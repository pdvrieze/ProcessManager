package nl.adaptivity.process.editor.android;

import java.util.ArrayList;
import java.util.List;

import android.graphics.RectF;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.android.DiagramAdapter;
import nl.adaptivity.diagram.android.LightView;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;


public class MyDiagramAdapter implements DiagramAdapter<DrawableView, DrawableProcessNode> {

  private DrawableProcessModel aDiagram;
  private List<DrawableView> aViewCache;
  private LightView aBackground;
  private LightView aOverlay;
  private RectF aBounds = new RectF();
  private boolean aInvalid = true;

  public MyDiagramAdapter(DrawableProcessModel pDiagram) {
    aDiagram = pDiagram;
    aViewCache = new ArrayList<DrawableView>(pDiagram.getModelNodes().size());
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
  public DrawableView getView(int pPosition) {
    for(int i=pPosition-aViewCache.size();i>=0;--i) { aViewCache.add(null); }
    DrawableView result = aViewCache.get(pPosition);
    if (result!=null) {
      return result;
    }
    result = new DrawableView(getItem(pPosition));
    aViewCache.set(pPosition, result);
    return result;
  }

  @Override
  public LightView getBackground() {
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

}
