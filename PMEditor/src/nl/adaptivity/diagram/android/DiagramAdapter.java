package nl.adaptivity.diagram.android;

import android.graphics.RectF;


public interface DiagramAdapter<T extends LightView, V> {
  
  int getCount();
  
  V getItem(int pPosition);
  
  T getView(int pPosition);

  LightView getBackground();
  
  LightView getOverlay();

  void getBounds(RectF pDiagramBounds);

}
