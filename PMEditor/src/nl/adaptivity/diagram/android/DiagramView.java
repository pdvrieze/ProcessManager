package nl.adaptivity.diagram.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static nl.adaptivity.diagram.Drawable.*;
import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.diagram.Diagram;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.editor.android.BuildConfig;
import nl.adaptivity.process.editor.android.R;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;

public class DiagramView extends View implements OnZoomListener{


  private static final int CACHE_PADDING = 30;

  public interface OnNodeClickListener {
    public boolean onNodeClicked(DiagramView pView, nl.adaptivity.diagram.Drawable node, MotionEvent event);
  }

  private final class MyGestureListener extends SimpleOnGestureListener {

    private boolean aIgnoreMove = false;
    private Drawable aLastTouchedElement;

    @Override
    public boolean onScroll(MotionEvent pE1, MotionEvent pE2, float pDistanceX, float pDistanceY) {
      if (aIgnoreMove) { return false; }
      double scale = getScale();
      setOffsetX(getOffsetX()+(pDistanceX/scale));
      setOffsetY(getOffsetY()+(pDistanceY/scale));
      return true;
    }

    @Override
    public void onShowPress(MotionEvent pE) {
      nl.adaptivity.diagram.Drawable touchedElement = getTouchedElement(pE);
      if (touchedElement!=null) highlightTouch(touchedElement);
    }

    private nl.adaptivity.diagram.Drawable getTouchedElement(MotionEvent pE) {
      final int pIdx = pE.getActionIndex();
      final double diagX = pE.getX(pIdx)/aScale +aOffsetX;
      final double diagY = pE.getY(pIdx)/aScale +aOffsetY;
      if (aLastTouchedElement!=null) {
        aLastTouchedElement = aLastTouchedElement.getItemAt(diagX, diagY);
        if (aLastTouchedElement!=null) { return aLastTouchedElement; }
      }
      aLastTouchedElement = findTouchedElement(diagX, diagY);
      return aLastTouchedElement;
    }

    public void setIgnoreMove(boolean pValue) {
      aIgnoreMove = pValue;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent pE) {
      try {
        if (aOnNodeClickListener!=null) {
          nl.adaptivity.diagram.Drawable touchedElement = getTouchedElement(pE);
          if (touchedElement!=null) {
            return aOnNodeClickListener.onNodeClicked(DiagramView.this, touchedElement, pE);
          }
        }
        return false;
      } finally {
        aLastTouchedElement=null;
      }
    }

    @Override
    public boolean onSingleTapUp(MotionEvent pE) {
      try {
        return super.onSingleTapUp(pE);
      } finally {
        aLastTouchedElement = null;
      }
    }


  }

  public static abstract class DiagramDrawable extends android.graphics.drawable.Drawable {

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
  private final Rect aMissingDiagramTextBounds = new Rect();
  private String aMissingDiagramText;
  private double aOffsetX = 0;
  private double aOffsetY = 0;
  private android.graphics.drawable.Drawable aOverlay;
  private ZoomButtonsController aZoomController;
  private final boolean aMultitouch;
  private double aScale=1d;
  private GestureDetector aGestureDetector;
  private ScaleGestureDetector aScaleGestureDetector;

  private OnNodeClickListener aOnNodeClickListener = null;

  private MyGestureListener aGestureListener = new MyGestureListener();

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
  private Bitmap aCacheBitmap;
  private Canvas aCacheCanvas;
  private Rectangle aCacheRect = new Rectangle(0d,0d,0d,0d);

  private final Rect aBuildTimeBounds = new Rect();
  private String aBuildTimeText;
  private Rectangle aTmpRectangle = new Rectangle(0d, 0d, 0d, 0d);
  private final RectF aTmpRectF = new RectF();
  private final Rect aTmpRect = new Rect();

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
    return "google_sdk".equals( Build.PRODUCT )||"sdk_x86".equals(Build.PRODUCT)|| "sdk".equals(Build.PRODUCT);
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


  public OnNodeClickListener getOnNodeClickListener() {
    return aOnNodeClickListener;
  }


  public void setOnNodeClickListener(OnNodeClickListener pNodeClickListener) {
    aOnNodeClickListener = pNodeClickListener;
  }

  protected void highlightTouch(nl.adaptivity.diagram.Drawable pTouchedElement) {
    pTouchedElement.setState(pTouchedElement.getState()|STATE_TOUCHED);
    invalidate();
//    invalidate(toRect(pTouchedElement.getBounds()));
  }

  private RectF toRectF(Rectangle pBounds) {
    aTmpRectF.set(pBounds.leftf(), pBounds.topf(), pBounds.rightf(), pBounds.bottomf());
    return aTmpRectF;
  }

  private Rect toRect(Rectangle pBounds) {
    aTmpRect.set((int)Math.round(pBounds.left*aScale-0.5), (int)Math.round(pBounds.top*aScale-0.5),
                 (int)Math.round(pBounds.right()*aScale+0.5), (int)Math.round(pBounds.bottom()*aScale+0.5));
    return aTmpRect;
  }

  protected nl.adaptivity.diagram.Drawable findTouchedElement(double pDiagX, double pDiagY) {
    return aDiagram.getItemAt(pDiagX, pDiagY);
  }

  @Override
  public void onDraw(Canvas pCanvas) {
    if (aTouchActionOptimize) {
      ensureValidCache();

      aTmpRectF.left = (float) ((aCacheRect.left-aOffsetX)*aScale);
      aTmpRectF.top = (float) ((aCacheRect.top-aOffsetY)*aScale);
      aTmpRectF.right = (float) (aTmpRectF.left+aCacheRect.width*aScale);
      aTmpRectF.bottom = (float) (aTmpRectF.top+aCacheRect.height*aScale);

      pCanvas.drawBitmap(aCacheBitmap, null, aTmpRectF, null);
    } else {

      drawDiagram(pCanvas);

      drawOverlay(pCanvas);

      if (BuildConfig.DEBUG) {
        drawDebugOverlay(pCanvas);
      }

    }
  }

  private void ensureValidCache() {
    if (aCacheBitmap!=null) {
      double oldScale = aCacheBitmap.getHeight()/aCacheRect.height;
      final double scaleChange = aScale/oldScale;
      if (scaleChange > 1.5 || scaleChange<0.34) {
        aCacheBitmap=null; // invalidate
        updateCacheRect(aCacheRect);
      } else {
        updateCacheRect(aTmpRectangle);
        if (aTmpRectangle.left+CACHE_PADDING < aCacheRect.left ||
            aTmpRectangle.top+CACHE_PADDING < aCacheRect.top ||
            aTmpRectangle.right()-CACHE_PADDING < aCacheRect.right() ||
            aTmpRectangle.bottom()-CACHE_PADDING < aCacheRect.bottom()) {
          aCacheBitmap = null;
          aCacheRect = aTmpRectangle;
        }
      }
    } else {
      updateCacheRect(aCacheRect); //cacherect is in screen scale
    }
    if (aCacheBitmap==null) {
      aCacheBitmap = Bitmap.createBitmap((int) Math.ceil(aCacheRect.width*aScale), (int) Math.ceil(aCacheRect.height*aScale), Bitmap.Config.ARGB_8888);
      aCacheCanvas = new Canvas(aCacheBitmap);
      aCacheBitmap.eraseColor(0x00000000);

      aCacheCanvas.translate((float)((aOffsetX - aCacheRect.left)*aScale), (float) ((aOffsetY - aCacheRect.top)*aScale));

      drawDiagram(aCacheCanvas);
      drawOverlay(aCacheCanvas);
    }
  }

  private void drawDiagram(Canvas canvas) {
    if (aDiagram!=null) {
      @SuppressLint("DrawAllocation")
      final Rectangle clipBounds = new Rectangle(-(aOffsetX/aScale), -(aOffsetY/aScale), getHeight(), getWidth());
      @SuppressLint("DrawAllocation")
      final AndroidCanvas androidcanvas = new AndroidCanvas(canvas);
      aDiagram.draw(androidcanvas.childCanvas(clipBounds, aScale), clipBounds);
    } else {
      ensureMissingDiagramTextBounds();
      canvas.drawText(aMissingDiagramText, (getWidth()-aMissingDiagramTextBounds.width())/2, (getHeight()-aMissingDiagramTextBounds.height())/2, getRedPen());
    }
  }

  private void drawOverlay(Canvas canvas) {
    int canvasSave = canvas.save();
    if (aOverlay!=null) {
      final float scalef = (float) aScale;
      canvas.scale(scalef, scalef);
      if (aOverlay instanceof DiagramDrawable) {
        ((DiagramDrawable) aOverlay).draw(canvas, aScale);
      } else {
        aOverlay.draw(canvas);
      }
    }
    canvas.restoreToCount(canvasSave);
  }

  private void drawDebugOverlay(Canvas canvas) {
    if (aBuildTimeText!=null) {
      ensureBuildTimeTextBounds();

      canvas.drawText(aBuildTimeText, getWidth() - aBuildTimeBounds.width()-20, getHeight()-20, aTimePen);
    }
  }

  private void ensureBuildTimeTextBounds() {
    if (aBuildTimeText==null) {
      InputStream stream = getClass().getClassLoader().getResourceAsStream("nl/adaptivity/process/diagram/version.properties");
      if (stream!=null) {
        try {
          Properties props = new Properties();
          try {
            props.load(stream);
          } catch (IOException e) {
            e.printStackTrace();
            aBuildTimeText="";
            aBuildTimeBounds.set(0, 0, 0, 0);
            return;
          }
          aBuildTimeText = props.getProperty("buildtime");
          if (aBuildTimeText!=null) {
            aTimePen.getTextBounds(aBuildTimeText, 0, aBuildTimeText.length(), aBuildTimeBounds);
          } else {
            aBuildTimeText = "";
            aBuildTimeBounds.set(0, 0, 0, 0);
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
    if (aTimePen==null) {
      aTimePen = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
      aTimePen.setARGB(255, 0, 0, 31);
      aTimePen.setTypeface(Typeface.DEFAULT);
      aTimePen.setTextSize(25f);
    }
  }

  private void ensureMissingDiagramTextBounds() {
    if (aMissingDiagramText==null) {
      aMissingDiagramText = getContext().getResources().getString(R.string.missing_diagram);
      getRedPen().getTextBounds(aMissingDiagramText, 0, aMissingDiagramText.length(), aMissingDiagramTextBounds);
    }
  }

  private Paint getRedPen() {
    if (aRed==null) {
      aRed = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
      aRed.setARGB(255, 255, 0, 0);
      aRed.setTypeface(Typeface.DEFAULT);
      aRed.setTextSize(50f);
    }
    return aRed;
  }

  private void updateCacheRect(Rectangle pCacheRect) {
    Rectangle diagrambounds = aDiagram.getBounds();
    double diagLeft = Math.max(diagrambounds.left-1, aOffsetX-CACHE_PADDING);
    double diagWidth = Math.min(diagrambounds.right()-diagLeft+6, (getWidth()/aScale) + (CACHE_PADDING*2));
    double diagTop = Math.max(diagrambounds.top-1, aOffsetY-CACHE_PADDING);
    double diagHeight = Math.min(diagrambounds.bottom()-diagTop+6, (getHeight()/aScale) + (CACHE_PADDING*2));
    pCacheRect.set(diagLeft, diagTop, diagWidth, diagHeight);
  }

  public void setOverlay(android.graphics.drawable.Drawable pOverlay) {
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
    nl.adaptivity.diagram.Drawable touchedElement = null;
    if (action==MotionEvent.ACTION_DOWN) {
      int pIdx = pEvent.getActionIndex();
      double diagX = pEvent.getX(pIdx)/aScale +aOffsetX;
      double diagY = pEvent.getY(pIdx)/aScale +aOffsetY;
      touchedElement = findTouchedElement(diagX, diagY);
      if (touchedElement!=null) {
        highlightTouch(touchedElement);
        aGestureListener.setIgnoreMove(true);
      }

//    if (BuildConfig.DEBUG) {
//    Debug.startMethodTracing();
//  }
//      aTouchActionOptimize  = true;
//      ensureValidCache();
    } else if (action==MotionEvent.ACTION_UP|| action==MotionEvent.ACTION_CANCEL) {
      for(nl.adaptivity.diagram.Drawable drawable: aDiagram.getChildElements()) {
        int state = drawable.getState();
        if ((state&STATE_TOUCHED)!=0) {
          drawable.setState(state & ~STATE_TOUCHED);
        }
      }

      aTouchActionOptimize  = false;
      aCacheBitmap = null; aCacheCanvas = null;
      Compat.postInvalidateOnAnimation(this);
//    if (BuildConfig.DEBUG) {
//      Debug.stopMethodTracing();
//    }
      aGestureListener.setIgnoreMove(false);
    }
    boolean retVal = aScaleGestureDetector.onTouchEvent(pEvent);
    retVal = aGestureDetector.onTouchEvent(pEvent) || retVal;
    return retVal || super.onTouchEvent(pEvent);
  }



  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  @Override
  public boolean onGenericMotionEvent(MotionEvent pEvent) {
    int action = pEvent.getActionMasked();
    if (action==MotionEvent.ACTION_SCROLL) {
      boolean zoomIn = Compat.isZoomIn(pEvent);;
      onZoom(zoomIn);
    }
    return super.onGenericMotionEvent(pEvent);
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
