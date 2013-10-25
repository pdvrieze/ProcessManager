package nl.adaptivity.diagram.android;

import android.graphics.Rect;
import android.widget.Adapter;


public interface DiagramAdapter extends Adapter {

  Rect getBounds(int position);

}
