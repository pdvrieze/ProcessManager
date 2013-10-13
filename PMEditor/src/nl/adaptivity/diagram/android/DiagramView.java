package nl.adaptivity.diagram.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.diagram.Diagram;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.editor.android.BuildConfig;
import nl.adaptivity.process.editor.android.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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

  private static final double MAXSCALE = 6d;
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

  private OnGestureListener aGestureListener = new SimpleOnGestureListener() {

    @Override
    public boolean onScroll(MotionEvent pE1, MotionEvent pE2, float pDistanceX, float pDistanceY) {
      double scale = getScale();
      setOffsetX(getOffsetX()+(pDistanceX/scale));
      setOffsetY(getOffsetY()+(pDistanceY/scale));
      return true;
    }

  };

  private OnScaleGestureListener aScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

    // These points are relative to the diagram. They should end up at the focal
    // point in canvas coordinates.
    double aPreviousDiagramX;
    double aPreviousDiagramY;


    @Override
    public boolean onScaleBegin(ScaleGestureDetector pDetector) {
      double newScale = getScale()*pDetector.getScaleFactor();
      aPreviousDiagramX = getOffsetX()+ pDetector.getFocusX()/newScale;
      aPreviousDiagramY = getOffsetY()+ pDetector.getFocusY()/newScale;

      return super.onScaleBegin(pDetector);
    }


    @Override
    public boolean onScale(ScaleGestureDetector pDetector) {
      double scaleAdjust = pDetector.getScaleFactor();
      double newScale = getScale()*scaleAdjust;
      if (newScale<=MAXSCALE && newScale>=MINSCALE) {
        setScale(newScale);

        double newDiagramX = pDetector.getFocusX()/newScale;
        double newDiagramY = pDetector.getFocusY()/newScale;

        setOffsetX(aPreviousDiagramX - newDiagramX);
        setOffsetY(aPreviousDiagramY - newDiagramY);

        return true;
      }
      return false;
    }

  };
  private boolean aTouchActionOptimize = false;
  private double aCachedScale;
  private Paint aCachePaint;
  private double aCachedOffsetY;
  private double aCachedOffsetX;
  private Bitmap aCacheBitmap;
  private Canvas aCacheCanvas;
  private Rectangle aCacheRect = new Rectangle(0d,0d,0d,0d);

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
    Compat.postInvalidateOnAnimation(this);
  }


  public double getOffsetY() {
    return aOffsetY;
  }


  public void setOffsetY(double pOffsetY) {
    aOffsetY = pOffsetY;
    Compat.postInvalidateOnAnimation(this);
  }


  public double getScale() {
    return aScale;
  }


  public void setScale(double pScale) {
    if (aScale!=pScale) {
      Compat.postInvalidateOnAnimation(this);
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
  public void onDraw(Canvas pCanvas) {
    Canvas canvas;
    if (aCacheCanvas ==null) {
      if (aTouchActionOptimize) {
        updateCacheRect(aCacheRect); //cacherect is in screen scale
        aCacheBitmap = Bitmap.createBitmap((int) Math.ceil(aCacheRect.width*aScale), (int) Math.ceil(aCacheRect.height*aScale), Bitmap.Config.ARGB_8888);
        aCacheCanvas = new Canvas(aCacheBitmap);
        aCacheBitmap.eraseColor(0x00000000);

        aCacheCanvas.translate((float)((aOffsetX - aCacheRect.left)*aScale), (float) ((aOffsetY - aCacheRect.top)*aScale));
        canvas = aCacheCanvas;
      } else {
        canvas = pCanvas;
      }
    } else {
      if (aTouchActionOptimize) {
        RectF target = new RectF();
        target.left = (float) ((aCacheRect.left-aOffsetX)*aScale);
        target.top = (float) ((aCacheRect.top-aOffsetY)*aScale);
        target.right = (float) (target.left+aCacheRect.width*aScale);
        target.bottom = (float) (target.top+aCacheRect.height*aScale);

//        pCanvas.drawBitmap(aCacheBitmap, target.left, target.top, null);
        pCanvas.drawBitmap(aCacheBitmap, null, target, null);
        // invalidate if needed, otherwise just blit bitmap.
  //      canvas = aCacheCanvas;
  //      aCacheBitmap.eraseColor(0x20ff0000);
        return;
      } else {
        canvas = pCanvas;
      }
    }

    int canvasSave = canvas.save();
//    pCanvas.drawLine(200, 0, 0, 200, aRed);
    if (aDiagram!=null) {

      @SuppressLint("DrawAllocation")
      final Rectangle clipBounds = new Rectangle(-(aOffsetX/aScale), -(aOffsetY/aScale), getHeight(), getWidth());
      @SuppressLint("DrawAllocation")
      final AndroidCanvas androidcanvas = new AndroidCanvas(canvas);
      aDiagram.draw(androidcanvas.childCanvas(clipBounds, aScale), clipBounds);
    } else {
      canvas.scale((float) aScale, (float) aScale);
      if (aRed==null) {
        aRed = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
        aRed.setARGB(255, 255, 0, 0);
        aRed.setTypeface(Typeface.DEFAULT);
        aRed.setTextSize(50f);
      }
      final String text = getContext().getResources().getString(R.string.missing_diagram);
      aRed.getTextBounds(text, 0, text.length(), aBounds);
      canvas.drawText(text, (getWidth()-aBounds.width())/2, (getHeight()-aBounds.height())/2, aRed);
      canvas.drawCircle(100, 100, 75, aRed);
    }
    canvas.restoreToCount(canvasSave);
    canvasSave = canvas.save();
    if (aOverlay!=null) {
      canvas.scale((float) aScale, (float) aScale);
      if (aOverlay instanceof DiagramDrawable) {
        ((DiagramDrawable) aOverlay).draw(canvas, aScale);
      }
      aOverlay.draw(canvas);
    }

    canvas.restoreToCount(canvasSave);
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
            canvas.drawText(buildtime, getWidth() - aBounds.width()-20, getHeight()-20, aTimePen);
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
    if (canvas == aCacheCanvas) {
      RectF target = new RectF();
      target.left = (float) ((aCacheRect.left-aOffsetX)*aScale);
      target.top = (float) ((aCacheRect.top-aOffsetY)*aScale);
      target.right = target.left+((float) (aCacheRect.width*aScale));
      target.bottom = target.top+((float) (aCacheRect.height*aScale));

//      pCanvas.drawBitmap(aCacheBitmap, target.left, target.top, null);
      pCanvas.drawBitmap(aCacheBitmap, null, target, null);
    }
  }

  private void updateCacheRect(Rectangle pCacheRect) {
    Rectangle diagrambounds = aDiagram.getBounds();
    double diagLeft = Math.max(diagrambounds.left-1, aOffsetX-30);
    double diagWidth = Math.min(diagrambounds.right()-diagLeft+6, (getWidth()/aScale) + 60);
    double diagTop = Math.max(diagrambounds.top-1, aOffsetY-30);
    double diagHeight = Math.min(diagrambounds.bottom()-diagTop+6, (getHeight()/aScale) + 60);
    pCacheRect.set(diagLeft, diagTop, diagWidth, diagHeight);
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
    int action = pEvent.getActionMasked();
    if (action==MotionEvent.ACTION_DOWN) {
      aTouchActionOptimize  = true;
//      buildDrawingCache(true);
//      setDrawingCacheEnabled(true);
    } else if (action==MotionEvent.ACTION_UP|| action==MotionEvent.ACTION_CANCEL) {
      aTouchActionOptimize  = false;
//      setDrawingCacheEnabled(false);
//      destroyDrawingCache();
//      Compat.postInvalidateOnAnimation(this);
      aCacheBitmap = null; aCacheCanvas = null;
      Compat.postInvalidateOnAnimation(this);
    }
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
