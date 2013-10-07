package nl.adaptivity.diagram.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import nl.adaptivity.diagram.Diagram;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.editor.android.BuildConfig;
import nl.adaptivity.process.editor.android.R;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;


public class DiagramView extends View implements OnZoomListener{


  public static abstract class DiagramDrawable extends Drawable {

    @Override
    public final void draw(Canvas pCanvas) {
      draw(pCanvas, 1d);
    }

    public abstract void draw(Canvas pCanvas, double pScale);

  }

  private static final double MAXSCALE = 3d;
  private static final double MINSCALE = 0.5d;
  private Diagram aDiagram;
  private Paint aRed;
  private Paint aTimePen;
  private Rect aBounds = new Rect();
  private double aOffsetX = 0;
  private double aOffsetY = 0;
  private Drawable aOverlay;
  private ZoomButtonsController aZoomController;
  private final boolean aMultitouch;
  private double aScale=1d;
  private GestureDetector aGestureDetector;
  private ScaleGestureDetector aScaleGestureDetector;

  private RectF aScrollerViewport = new RectF();

  private OnGestureListener aGestureListener = new SimpleOnGestureListener() {

  };

  private OnScaleGestureListener aScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

    @Override
    public boolean onScale(ScaleGestureDetector pDetector) {
      double scaleAdjust = ((double)pDetector.getCurrentSpan())/((double) pDetector.getPreviousSpan());
      double newScale = getScale()*scaleAdjust;
      if (newScale<=MAXSCALE && newScale>=MINSCALE) {
        setScale(newScale);
        return true;
      }
      return false;
    }

  };

  public DiagramView(Context pContext, AttributeSet pAttrs, int pDefStyle) {
    super(pContext, pAttrs, pDefStyle);
    aMultitouch = (! isEmulator()) && pContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    aGestureDetector = new GestureDetector(pContext, aGestureListener);
    aScaleGestureDetector = new ScaleGestureDetector(pContext, aScaleGestureListener);
  }

  public DiagramView(Context pContext, AttributeSet pAttrs) {
    super(pContext, pAttrs);
    aMultitouch = (! isEmulator()) && pContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    aGestureDetector = new GestureDetector(pContext, aGestureListener);
    aScaleGestureDetector = new ScaleGestureDetector(pContext, aScaleGestureListener);
  }

  public DiagramView(Context pContext) {
    super(pContext);
    aMultitouch = (! isEmulator()) && pContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    aGestureDetector = new GestureDetector(pContext, aGestureListener);
    aScaleGestureDetector = new ScaleGestureDetector(pContext, aScaleGestureListener);
  }

  private static boolean isEmulator() {
    return "google_sdk".equals( Build.PRODUCT )||"sdk_x86".equals(Build.PRODUCT);
  }

  public double getOffsetX() {
    return aOffsetX;
  }


  public void setOffsetX(double pOffsetX) {
    aOffsetX = pOffsetX;
  }


  public double getOffsetY() {
    return aOffsetY;
  }


  public void setOffsetY(double pOffsetY) {
    aOffsetY = pOffsetY;
  }


  public double getScale() {
    return aScale;
  }


  public void setScale(double pScale) {
    if (aScale!=pScale) {
      invalidate();
    }
    aScale = pScale;
  }

  public Diagram getDiagram() {
    return aDiagram;
  }

  public void setDiagram(Diagram pDiagram) {
    aDiagram = pDiagram;
    if (aZoomController!=null) {
      aZoomController.setZoomInEnabled(aDiagram!=null);
      aZoomController.setZoomOutEnabled(aDiagram!=null);
    }
  }

  @Override
  public void draw(Canvas pCanvas) {
    super.draw(pCanvas);
    int canvasSave = pCanvas.save();
    pCanvas.scale((float) aScale, (float) aScale);
//    pCanvas.drawLine(200, 0, 0, 200, aRed);
    if (aDiagram!=null) {
      final Rectangle clipBounds = new Rectangle(aOffsetX, aOffsetY, getHeight(), getWidth());
      final AndroidCanvas canvas = new AndroidCanvas(pCanvas);
      aDiagram.draw(canvas.childCanvas(clipBounds, 1d), clipBounds);
    } else {
      if (aRed==null) {
        aRed = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
        aRed.setARGB(255, 255, 0, 0);
        aRed.setTypeface(Typeface.DEFAULT);
        aRed.setTextSize(50f);
      }
      final String text = getContext().getResources().getString(R.string.missing_diagram);
      aRed.getTextBounds(text, 0, text.length(), aBounds);
      pCanvas.drawText(text, (getWidth()-aBounds.width())/2, (getHeight()-aBounds.height())/2, aRed);
      pCanvas.drawCircle(100, 100, 75, aRed);
    }
    if (aOverlay!=null) {
      if (aOverlay instanceof DiagramDrawable) {
        ((DiagramDrawable) aOverlay).draw(pCanvas, aScale);
      }
      aOverlay.draw(pCanvas);
    }

    pCanvas.restoreToCount(canvasSave);
    if (BuildConfig.DEBUG) {
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
          if (buildtime!=null) {
            if (aTimePen==null) {
              aTimePen = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
              aTimePen.setARGB(255, 0, 0, 31);
              aTimePen.setTypeface(Typeface.DEFAULT);
              aTimePen.setTextSize(25f);
            }

            aTimePen.getTextBounds(buildtime, 0, buildtime.length(), aBounds);
            pCanvas.drawText(buildtime, getWidth() - aBounds.width()-20, getHeight()-20, aTimePen);
          }
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

  public void setOverlay(Drawable pOverlay) {
    if (aOverlay!=null) {
      invalidate(aOverlay.getBounds());
    }
    aOverlay = pOverlay;
    if (pOverlay!=null) {
      invalidate(pOverlay.getBounds());
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent pEvent) {
    boolean retVal = aScaleGestureDetector.onTouchEvent(pEvent);
    retVal = aGestureDetector.onTouchEvent(pEvent) || retVal;
    return retVal || super.onTouchEvent(pEvent);
  }

  @Override
  public void onVisibilityChanged(boolean pVisible) {
    // Part of zoomcontroller
    // ignore it
  }



  @Override
  protected void onVisibilityChanged(View pChangedView, int pVisibility) {
    if (pChangedView==this && aZoomController!=null) {
      aZoomController.setVisible(pVisibility==View.VISIBLE);
    }
  }

  @Override
  public void onZoom(boolean pZoomIn) {
    if (pZoomIn) {
      setScale(getScale()*1.2);
    } else {
      setScale(getScale()/1.2);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (! aMultitouch) {
      aZoomController = new ZoomButtonsController(this);
      aZoomController.setOnZoomListener(this);
      aZoomController.setAutoDismissed(false);
      aZoomController.setZoomInEnabled(aDiagram!=null);
      aZoomController.setZoomOutEnabled(aDiagram!=null);
      aZoomController.setVisible(getVisibility()==VISIBLE);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    if (! aMultitouch) {
      aZoomController.setVisible(false);
    }
    super.onDetachedFromWindow();
  }

}
