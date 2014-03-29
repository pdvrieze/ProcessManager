package nl.adaptivity.diagram.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.devrieze.util.Tupple;

import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.process.editor.android.BuildConfig;
import nl.adaptivity.process.editor.android.R;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
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


  private static final int INVALIDATE_MARGIN = 10;
  private static final int CACHE_PADDING = 30;

  public interface OnNodeClickListener {
    public boolean onNodeClicked(DiagramView pView, int pTouchedElement, MotionEvent event);
  }

  private final class MyGestureListener extends SimpleOnGestureListener {

    private boolean aMoveItem = false;
    private int aMoving = -1;
    private float aOrigX;
    private float aOrigY;
    @Override
    public boolean onScroll(MotionEvent pE1, MotionEvent pE2, float pDistanceX, float pDistanceY) {
      if (aMoving>=0 || aMoveItem) {
        int touchedElement = aMoving>=0 ? aMoving: getTouchedElement(pE1);
        if (touchedElement>=0) {
          if (aMoving <0) {
            if (Math.max(Math.abs(pDistanceY),Math.abs(pDistanceX))>MIN_DRAG_DIST) {
              aMoving = touchedElement;
              LightView lv = aAdapter.getView(touchedElement);
              lv.getBounds(aTmpRectF);
              aOrigX = aTmpRectF.left;
              aOrigY = aTmpRectF.top;
            }
          }
          if (aMoving>=0) {
            LightView lv = aAdapter.getView(touchedElement);
            if (mGridSize>0) {
              lv.getBounds(aTmpRectF);
              double dX = (pE2.getX()-pE1.getX())/aScale;
              float newX = Math.round((aOrigX + dX)/mGridSize)*mGridSize;
              double dY = (pE2.getY()-pE1.getY())/aScale;
              float newY = Math.round((aOrigY + dY)/mGridSize)*mGridSize;
              lv.move(newX-aTmpRectF.left, newY-aTmpRectF.top);
            } else {
              lv.move((float)(-pDistanceX/aScale), (float) (-pDistanceY/aScale));
            }
            invalidate();
          }
          return true;
        }
        return false;
      } else {
        double scale = getScale();
        setOffsetX(getOffsetX()+(pDistanceX/scale));
        setOffsetY(getOffsetY()+(pDistanceY/scale));
        return true;
      }
    }

    @Override
    public void onShowPress(MotionEvent pE) {
      int touchedElement = getTouchedElement(pE);
      if (touchedElement>=0) highlightTouch(touchedElement);
    }

    /**
     * Allow for specifying whether the move is for a single item, or for the
     * canvas.
     *
     * @param pValue <code>true</code> when the focus is an element,
     *          <code>false</code> when not.
     */
    public void setMoveItem(boolean pValue) {
      aMoveItem = pValue;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent pE) {
      try {
        if (aOnNodeClickListener!=null) {
          int touchedElement = getTouchedElement(pE);
          if (touchedElement>=0) {
            return aOnNodeClickListener.onNodeClicked(DiagramView.this, touchedElement, pE);
          }
        }
        return false;
      } finally {
        aLastTouchedElement=0;
      }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent pE) {
      try {
        return super.onSingleTapUp(pE);
      } finally {
        aLastTouchedElement = 0;
      }
    }

    public void actionFinished() {
      aMoveItem = false;
      aMoving = -1;
    }


  }

  public static abstract class DiagramDrawable extends android.graphics.drawable.Drawable {

    @Override
    public final void draw(Canvas pCanvas) {
      draw(pCanvas, 1d);
    }

    public abstract void draw(Canvas pCanvas, double pScale);

  }

  public static final double DENSITY = Resources.getSystem().getDisplayMetrics().density;
  private static final double MAXSCALE = 6d*DENSITY;
  private static final double MINSCALE = 0.5d*DENSITY;
  private static final float MIN_DRAG_DIST = (float) (8*DENSITY);
  private DiagramAdapter<?,?> aAdapter;
  private Paint aRed;
  private Paint aTimePen;
  private int aLastTouchedElement = -1;
  private final Rect aMissingDiagramTextBounds = new Rect();
  private String aMissingDiagramText;
  private double aOffsetX = 0;
  private double aOffsetY = 0;
  private android.graphics.drawable.Drawable aOverlay;
  private ZoomButtonsController aZoomController;
  private final boolean aMultitouch;
  private double aScale=DENSITY*160/96; // Use density of 96dpi for drawables
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
  private int mGridSize;
  private List<Tupple<Integer, RelativeLightView>> aDecorations = new ArrayList<>();
  private Tupple<Integer,RelativeLightView> aTouchedDecoration = null;

  private static final int DEFAULT_GRID_SIZE=8;

  public DiagramView(Context pContext, AttributeSet pAttrs, int pDefStyle) {
    super(pContext, pAttrs, pDefStyle);
    aMultitouch = (isNotEmulator()) && pContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    aGestureDetector = new GestureDetector(pContext, aGestureListener);
    aScaleGestureDetector = new ScaleGestureDetector(pContext, aScaleGestureListener);
    init(pAttrs);
  }

  public DiagramView(Context pContext, AttributeSet pAttrs) {
    super(pContext, pAttrs);
    aMultitouch = (isNotEmulator()) && pContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    aGestureDetector = new GestureDetector(pContext, aGestureListener);
    aScaleGestureDetector = new ScaleGestureDetector(pContext, aScaleGestureListener);
    init(pAttrs);
  }

  public DiagramView(Context pContext) {
    super(pContext);
    aMultitouch = (isNotEmulator()) && pContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    aGestureDetector = new GestureDetector(pContext, aGestureListener);
    aScaleGestureDetector = new ScaleGestureDetector(pContext, aScaleGestureListener);
    init(null);
  }

  public void init(AttributeSet pAttrs) {
    if (pAttrs==null) {
      mGridSize=DEFAULT_GRID_SIZE;
    } else {
      TypedArray a = getContext().getTheme().obtainStyledAttributes(pAttrs, R.styleable.DiagramView, 0, 0);
      try {
        mGridSize = a.getInteger(R.styleable.DiagramView_gridSize, DEFAULT_GRID_SIZE);
      } finally {
        a.recycle();
      }
    }
    setLayerType(LAYER_TYPE_SOFTWARE, null);
  }

  private static boolean isNotEmulator() {
    return !"google_sdk".equals(Build.PRODUCT) && !"sdk_x86".equals(Build.PRODUCT) && !"sdk".equals(Build.PRODUCT);
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


  public int getGridSize() {
    return mGridSize;
  }


  public void setGridSize(int pGridSize) {
    mGridSize = pGridSize;
  }

  public DiagramAdapter<?,?> getAdapter() {
    return aAdapter;
  }

  public void setAdapter(DiagramAdapter<?, ?> pDiagram) {
    aAdapter = pDiagram;
    if (aZoomController!=null) {
      aZoomController.setZoomInEnabled(aAdapter!=null);
      aZoomController.setZoomOutEnabled(aAdapter!=null);
    }
  }


  public OnNodeClickListener getOnNodeClickListener() {
    return aOnNodeClickListener;
  }


  public void setOnNodeClickListener(OnNodeClickListener pNodeClickListener) {
    aOnNodeClickListener = pNodeClickListener;
  }

  protected void highlightTouch(int pTouchedElement) {
    LightView lv = aAdapter.getView(pTouchedElement);
    if (! lv.isTouched()) {
      lv.setTouched(true);
      invalidate(pTouchedElement);
    }
  }

  /**
   * Invalidate the item at the given position.
   * @param pPosition
   */
  private void invalidate(int pPosition) {
    LightView lv = aAdapter.getView(pPosition);
    invalidate(lv);
  }

  public void invalidate(LightView view) {
    view.getBounds(aTmpRectF);
    outset(aTmpRectF, INVALIDATE_MARGIN);
    toCanvasRect(aTmpRectF, aTmpRect);
    invalidate(aTmpRect);
  }

  private static void outset(RectF pRect, float pOutset) {
    pRect.left-=pOutset;
    pRect.top-=pOutset;
    pRect.right+=pOutset;
    pRect.bottom+=pOutset;
  }

  private void toCanvasRect(RectF pSource, Rect pDest) {
    pDest.left=(int) Math.floor(toCanvasX(pSource.left));
    pDest.top=(int) Math.floor(toCanvasY(pSource.top));
    pDest.right = (int) Math.ceil(toCanvasX(pSource.right));
    pDest.bottom = (int) Math.ceil(toCanvasY(pSource.bottom));
  }

  /**
   * Get the element touched by the given event.
   *
   * @param pEvent The event to analyse.
   * @return The index of the touched element, or <code>-1</code> when none.
   */
  private int getTouchedElement(MotionEvent pEvent) {
    int pIdx = pEvent.getActionIndex();
    float x = pEvent.getX(pIdx);
    float diagX = toDiagramX(x);
    float y = pEvent.getY(pIdx);
    float diagY = toDiagramY(y);


//    final int pIdx = pE.getActionIndex();
//    float canvX = pE.getX(pIdx);
//    float canvY = pE.getY(pIdx);
    if (aLastTouchedElement>=0) {
      getItemBounds(aLastTouchedElement, aTmpRectF);
      if (aTmpRectF.contains(diagX, diagY)) {
        return aLastTouchedElement;
      }
    }
    aLastTouchedElement = findTouchedElement(diagX, diagY);
    return aLastTouchedElement;
  }

  protected int findTouchedElement(float diagX, float diagY) {
    final int len = aAdapter.getCount();
    for(int i=0;i < len ; ++i) {
      getItemBounds(i, aTmpRectF);
      if (aTmpRectF.contains(diagX, diagY)) {
        return i;
      }
    }
    return -1;
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

      drawDecorations(pCanvas);

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
    if (aAdapter!=null) {
      LightView bg = aAdapter.getBackground();
      Theme<AndroidStrategy, AndroidPen, AndroidPath> theme = aAdapter.getTheme();
      if (bg!=null) {
        int save = canvas.save();
        canvas.translate(toCanvasX(0), toCanvasY(0));
        bg.draw(canvas, theme, aScale);
        canvas.restoreToCount(save);
      }

      int len = aAdapter.getCount();
      for(int i=0; i<len; i++) {
        final LightView lv = aAdapter.getView(i);
        lv.getBounds(aTmpRectF);
        int save = canvas.save();
        canvas.translate(toCanvasX(aTmpRectF.left), toCanvasY(aTmpRectF.top));
        lv.draw(canvas, theme, aScale);
        canvas.restoreToCount(save);
      }

      LightView overlay = aAdapter.getOverlay();
      if (overlay!=null) {
        overlay.getBounds(aTmpRectF);
        int save = canvas.save();
        canvas.translate(toCanvasX(aTmpRectF.left), toCanvasY(aTmpRectF.top));
        overlay.draw(canvas, theme, aScale);
        canvas.restoreToCount(save);
      }
//      @SuppressLint("DrawAllocation")
//      final Rectangle clipBounds = new Rectangle(-(aOffsetX/aScale), -(aOffsetY/aScale), getHeight(), getWidth());
//      @SuppressLint("DrawAllocation")
//      final AndroidCanvas androidcanvas = new AndroidCanvas(canvas);
//      aAdapter.draw(androidcanvas.childCanvas(clipBounds, aScale), clipBounds);
    } else {
      ensureMissingDiagramTextBounds();
      canvas.drawText(aMissingDiagramText, (getWidth()-aMissingDiagramTextBounds.width())/2, (getHeight()-aMissingDiagramTextBounds.height())/2, getRedPen());
    }
  }

  private void drawDecorations(Canvas canvas) {
    // TODO handle decoration touches
    if (aAdapter!=null) {
      aDecorations.clear();
      Theme<AndroidStrategy, AndroidPen, AndroidPath> theme = aAdapter.getTheme();
      int len = aAdapter.getCount();
      for(int i=0; i<len; i++) {
        final LightView lv = aAdapter.getView(i);
        List<? extends RelativeLightView> decorations = aAdapter.getRelativeDecorations(i, aScale, lv.isSelected());
        for(RelativeLightView decoration: decorations) {
          aDecorations.add(Tupple.tupple(Integer.valueOf(i), decoration));
          int savePos = canvas.save();
          decoration.getBounds(aTmpRectF);
          canvas.translate(toCanvasX(aTmpRectF.left), toCanvasY(aTmpRectF.top));
          decoration.draw(canvas, theme, aScale);
          canvas.restoreToCount(savePos);
        }
      }
    }
  }

  public float toCanvasX(double pX) {
    return (float) ((pX-aOffsetX)*aScale);
  }

  public float toCanvasY(double pY) {
    return (float) ((pY-aOffsetY)*aScale);
  }

  public float toDiagramX(float x) {
    return (float) (x/aScale +aOffsetX);
  }

  public float toDiagramY(float y) {
    return (float) (y/aScale +aOffsetY);
  }

  private void getItemBounds(int pos, RectF rect) {
    aAdapter.getView(pos).getBounds(rect);
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
    RectF diagrambounds = aTmpRectF;
    aAdapter.getBounds(diagrambounds);
    double diagLeft = Math.max(diagrambounds.left-1, aOffsetX-CACHE_PADDING);
    double diagWidth = Math.min(diagrambounds.right-diagLeft+6, (getWidth()/aScale) + (CACHE_PADDING*2));
    double diagTop = Math.max(diagrambounds.top-1, aOffsetY-CACHE_PADDING);
    double diagHeight = Math.min(diagrambounds.bottom-diagTop+6, (getHeight()/aScale) + (CACHE_PADDING*2));
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
    int touchedElement = -1;
    int pIdx = pEvent.getActionIndex();
    float diagX = toDiagramX(pEvent.getX(pIdx));
    float diagY = toDiagramY(pEvent.getY(pIdx));
    if (action==MotionEvent.ACTION_DOWN) {

      touchedElement = findTouchedElement(diagX, diagY);
      if (touchedElement>=0) {
        highlightTouch(touchedElement);
        aGestureListener.setMoveItem(true);
        if (aTouchedDecoration!=null) {
          aTouchedDecoration.getElem2().setTouched(false);
          invalidate(aTouchedDecoration.getElem2());
          aTouchedDecoration =null;
        }
      } else {
        if (aTouchedDecoration!=null) {
          aTouchedDecoration.getElem2().getBounds(aTmpRectF);
          if (! aTmpRectF.contains(diagX, diagY)) {
            aTouchedDecoration.getElem2().setTouched(false);
            invalidate(aTouchedDecoration.getElem2());
            aTouchedDecoration = null;
          }
        }
        if (aTouchedDecoration==null) {
          for(Tupple<Integer,RelativeLightView> decoration: aDecorations) {
            decoration.getElem2().getBounds(aTmpRectF);
            if (aTmpRectF.contains(diagX, diagY)) {
              aTouchedDecoration = decoration;
              aTouchedDecoration.getElem2().setTouched(true);
              invalidate(aTouchedDecoration.getElem2());
              break;
            }
          }
        }
      }

//    if (BuildConfig.DEBUG) {
//    Debug.startMethodTracing();
//  }
//      aTouchActionOptimize  = true;
//      ensureValidCache();
    } else if (action==MotionEvent.ACTION_UP|| action==MotionEvent.ACTION_CANCEL) {
      final int len = aAdapter.getCount();
      for(int i=0; i<len ; ++i) {
        LightView lv = aAdapter.getView(i);
        lv.setTouched(false);
      }

      if (aTouchedDecoration!=null) {
        aTouchedDecoration.getElem2().setTouched(false);
        aTouchedDecoration.getElem2().getBounds(aTmpRectF);
        if (aTmpRectF.contains(diagX, diagY)) {
          aAdapter.onDecorationClick(this, aTouchedDecoration.getElem1(), aTouchedDecoration.getElem2());
        } else {
          aAdapter.onDecorationUp(this, aTouchedDecoration.getElem1(), aTouchedDecoration.getElem2(), diagX, diagY);
        }

        invalidate(aTouchedDecoration.getElem2());
        aTouchedDecoration = null;
      }

      aTouchActionOptimize  = false;
      aCacheBitmap = null; aCacheCanvas = null;
      Compat.postInvalidateOnAnimation(this);
//    if (BuildConfig.DEBUG) {
//      Debug.stopMethodTracing();
//    }
      aGestureListener.actionFinished();
//      aGestureListener.setMoveItem(false);
    } else if (action==MotionEvent.ACTION_MOVE && aTouchedDecoration!=null) {
      aAdapter.onDecorationMove(this, aTouchedDecoration.getElem1(), aTouchedDecoration.getElem2(), diagX, diagY);
      return true;
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
      boolean zoomIn = Compat.isZoomIn(pEvent);
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
    if (! (aMultitouch || isInEditMode())) {
      aZoomController = new ZoomButtonsController(this);
      aZoomController.setOnZoomListener(this);
      aZoomController.setAutoDismissed(false);
      aZoomController.setZoomInEnabled(aAdapter!=null);
      aZoomController.setZoomOutEnabled(aAdapter!=null);
      aZoomController.setVisible(getVisibility()==VISIBLE);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    if (! (aMultitouch|| isInEditMode())) {
      aZoomController.setVisible(false);
    }
    super.onDetachedFromWindow();
  }

  public void setSelection(int pPosition) {
    // TODO handle more complicated selection.
    int len = aAdapter.getCount();
    for(int i=0; i<len; ++i) {
      aAdapter.getView(i).setSelected(i==pPosition);
    }
  }

  public int getSelection() {
    int len = aAdapter.getCount();
    for(int i=0; i<len; ++i) {
      if (aAdapter.getView(i).isSelected()) {
        return i;
      }
    }
    return -1;
  }

}
