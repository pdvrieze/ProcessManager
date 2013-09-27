package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Diagram;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.editor.android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;


public class DiagramView extends View {

  private Diagram aDiagram;
  private Paint aRed;

  public DiagramView(Context pContext, AttributeSet pAttrs, int pDefStyle) {
    super(pContext, pAttrs, pDefStyle);
  }

  public DiagramView(Context pContext, AttributeSet pAttrs) {
    super(pContext, pAttrs);
  }

  public DiagramView(Context pContext) {
    super(pContext);
  }

  public Diagram getDiagram() {
    return aDiagram;
  }

  public void setDiagram(Diagram pDiagram) {
    aDiagram = pDiagram;
  }

  @Override
  public void draw(Canvas pCanvas) {
    super.draw(pCanvas);
//    pCanvas.drawLine(200, 0, 0, 200, aRed);

    if (aDiagram!=null) {
      final Rectangle clipBounds = new Rectangle(0d, 0d, getHeight(), getWidth());
      final AndroidCanvas canvas = new AndroidCanvas(pCanvas);
      aDiagram.draw(canvas, clipBounds);
    } else {
      if (aRed==null) {
        aRed = new Paint();
        aRed.setARGB(255, 255, 0, 0);
        aRed.setTypeface(Typeface.DEFAULT);
        aRed.setTextSize(50f);
      }
      pCanvas.drawText(getContext().getResources().getString(R.string.missing_diagram), getWidth()/2, getHeight()/2, aRed);
      pCanvas.drawCircle(100, 100, 75, aRed);
    }
  }

}
