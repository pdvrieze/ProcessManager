package nl.adaptivity.diagram.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import nl.adaptivity.diagram.Diagram;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.editor.android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;


public class DiagramView extends View {

  private Diagram aDiagram;
  private Paint aRed;
  private Rect aBounds = new Rect();

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
    if (aRed==null) {
      aRed = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
      aRed.setARGB(255, 255, 0, 0);
      aRed.setTypeface(Typeface.DEFAULT);
      aRed.setTextSize(50f);
    }
    if (aDiagram!=null) {
      final Rectangle clipBounds = new Rectangle(0d, 0d, getHeight(), getWidth());
      final AndroidCanvas canvas = new AndroidCanvas(pCanvas);
      aDiagram.draw(canvas, clipBounds);
    } else {
      final String text = getContext().getResources().getString(R.string.missing_diagram);
      aRed.getTextBounds(text, 0, text.length(), aBounds);
      pCanvas.drawText(text, (getWidth()-aBounds.width())/2, (getHeight()-aBounds.height())/2, aRed);
      pCanvas.drawCircle(100, 100, 75, aRed);
    }
    InputStream stream = getClass().getClassLoader().getResourceAsStream("nl/adaptivity/process/diagram/version.properties");
    if (stream!=null) {
      try {
        Properties props = new Properties();
        try {
          props.load(stream);
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }
        String buildtime = props.getProperty("buildtime");
        aRed.getTextBounds(buildtime, 0, buildtime.length(), aBounds);
        pCanvas.drawText(buildtime, getWidth() - aBounds.width()-20, getHeight()-aBounds.height()-20, aRed);
      } finally {
        try {
          stream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

}
