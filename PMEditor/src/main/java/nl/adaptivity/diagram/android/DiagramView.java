package nl.adaptivity.diagram.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.view.*;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;
import net.devrieze.util.Tupple;
import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.process.editor.android.BuildConfig;
import nl.adaptivity.process.editor.android.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DiagramView extends View implements OnZoomListener{



  private static class DiagramViewStateCreator implements Creator<DiagramViewState> {

    @Override
    public DiagramViewState createFromParcel(Parcel source) {
      return new DiagramViewState(source);
    }

    @Override
    public DiagramViewState[] newArray(int size) {
      return new DiagramViewState[size];
    }

  }

  private static class DiagramViewState extends View.BaseSavedState {

    @SuppressWarnings({ "unused", "hiding" })
    public static final Parcelable.Creator<DiagramViewState> CREATOR = new DiagramViewStateCreator();

    private double mOffsetX;
    private double mOffsetY;
    private double mScale;
    private int mGridSize;

    public DiagramViewState(DiagramView diagramView, Parcelable viewState) {
      super(viewState);
      mOffsetX = diagramView.mOffsetX;
      mOffsetY = diagramView.mOffsetY;
      mScale = diagramView.mScale;
      mGridSize = diagramView.mGridSize;
    }

    public DiagramViewState(Parcel source) {
      super(source);
      mOffsetX = source.readDouble();
      mOffsetY = source.readDouble();
      mScale = source.readDouble();
      mGridSize = source.readInt();
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      dest.writeDouble(mOffsetX);
      dest.writeDouble(mOffsetY);
      dest.writeDouble(mScale);
      dest.writeInt(mGridSize);
    }

  }

  private static final int INVALIDATE_MARGIN = 10;
  private static final int CACHE_PADDING = 30;

  public interface OnNodeClickListener {
    public boolean onNodeClicked(DiagramView view, int touchedElement, MotionEvent event);
  }

  private final class MyGestureListener extends SimpleOnGestureListener {

    private boolean aMoveItem = false;
    private int aMoving = -1;
    private double aOrigX;
    private double aOrigY;
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      if (isEditable()&& (aMoving>=0 || aMoveItem)) {
        int touchedElement = aMoving>=0 ? aMoving: getTouchedElement(e1);
        if (touchedElement>=0) {
          if (aMoving <0) {
            if (Math.max(Math.abs(distanceY),Math.abs(distanceX))>MIN_DRAG_DIST) {
              aMoving = touchedElement;
              aOrigX = mAdapter.getGravityX(touchedElement);
              aOrigY = mAdapter.getGravityY(touchedElement);
              // Cancel the selection on drag.
              setSelection(-1);
            }
          }
          if (aMoving>=0) {
            LightView lv = mAdapter.getView(touchedElement);
            if (mGridSize>0) {
              double dX = (e2.getX()-e1.getX())/ mScale;
              float newX = Math.round((aOrigX + dX)/mGridSize)*mGridSize;
              double dY = (e2.getY()-e1.getY())/ mScale;
              float newY = Math.round((aOrigY + dY)/mGridSize)*mGridSize;
              lv.move((float)(newX- mAdapter.getGravityX(touchedElement)), (float) (newY- mAdapter.getGravityY(touchedElement)));
            } else {
              lv.move((float)(-distanceX/ mScale), (float) (-distanceY/ mScale));
            }
            invalidate();
          }
          return true;
        }
        return false;
      } else {
        double scale = getScale();
        setOffsetX(getOffsetX()+(distanceX/scale));
        setOffsetY(getOffsetY()+(distanceY/scale));
        return true;
      }
    }

    @Override
    public void onShowPress(MotionEvent e) {
      int touchedElement = getTouchedElement(e);
      if (touchedElement>=0) highlightTouch(touchedElement);
    }

    /**
     * Allow for specifying whether the move is for a single item, or for the
     * canvas.
     *
     * @param value <code>true</code> when the focus is an element,
     *          <code>false</code> when not.
     */
    public void setMoveItem(boolean value) {
      aMoveItem = value;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
      try {
        int touchedElement = getTouchedElement(e);
        if (touchedElement>=0 && isEditable()) {
          if (mAdapter.onNodeClickOverride(DiagramView.this, touchedElement, e)) {
            return true;
          }
          if (aOnNodeClickListener!=null) {
            if(aOnNodeClickListener.onNodeClicked(DiagramView.this, touchedElement, e)) {
              return true;
            }
          }
        }
        return false;
      } finally {
        aLastTouchedElement=0;
      }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
      try {
        return super.onSingleTapUp(e);
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
    public final void draw(Canvas canvas) {
      draw(canvas, 1d);
    }

    public abstract void draw(Canvas canvas, double scale);

  }

  public static final double DENSITY = Resources.getSystem().getDisplayMetrics().density;
  private static final double MAXSCALE = 6d*DENSITY;
  private static final double MINSCALE = 0.5d*DENSITY;
  private static final float MIN_DRAG_DIST = (float) (8*DENSITY);
  private DiagramAdapter<?,?> mAdapter;
  private Paint aRed;
  private Paint aTimePen;
  private int aLastTouchedElement = -1;
  private final Rect aMissingDiagramTextBounds = new Rect();
  private String aMissingDiagramText;
  private double mOffsetX = 0;
  private double mOffsetY = 0;
  private android.graphics.drawable.Drawable aOverlay;
  private ZoomButtonsController mZoomController;
  private final boolean aMultitouch;
  private double mScale =DENSITY*160/96; // Use density of 96dpi for drawables
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
    public boolean onScaleBegin(ScaleGestureDetector detector) {
      double newScale = getScale()*detector.getScaleFactor();
      aPreviousDiagramX = getOffsetX()+ detector.getFocusX()/newScale;
      aPreviousDiagramY = getOffsetY()+ detector.getFocusY()/newScale;

      return super.onScaleBegin(detector);
    }


    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      double scaleAdjust = detector.getScaleFactor();
      double newScale = getScale()*scaleAdjust;
      if (newScale<=MAXSCALE && newScale>=MINSCALE) {
        setScale(newScale);

        double newDiagramX = detector.getFocusX()/newScale;
        double newDiagramY = detector.getFocusY()/newScale;

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

  private boolean mEditable = true;

  private static final int DEFAULT_GRID_SIZE=8;

  public DiagramView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    aMultitouch = (isNotEmulator()) && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    aGestureDetector = new GestureDetector(context, aGestureListener);
    aScaleGestureDetector = new ScaleGestureDetector(context, aScaleGestureListener);
    init(attrs);
  }

  public DiagramView(Context context, AttributeSet attrs) {
    super(context, attrs);
    aMultitouch = (isNotEmulator()) && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    aGestureDetector = new GestureDetector(context, aGestureListener);
    aScaleGestureDetector = new ScaleGestureDetector(context, aScaleGestureListener);
    init(attrs);
  }

  public DiagramView(Context context) {
    super(context);
    aMultitouch = (isNotEmulator()) && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    aGestureDetector = new GestureDetector(context, aGestureListener);
    aScaleGestureDetector = new ScaleGestureDetector(context, aScaleGestureListener);
    init(null);
  }

  public void init(AttributeSet attrs) {
    if (attrs==null) {
      mGridSize=DEFAULT_GRID_SIZE;
    } else {
      TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.DiagramView, 0, 0);
      try {
        mGridSize = a.getInteger(R.styleable.DiagramView_gridSize, DEFAULT_GRID_SIZE);
        mEditable = a.getBoolean(R.styleable.DiagramView_editable, true);
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
    return mOffsetX;
  }


  public void setOffsetX(double offsetX) {
    mOffsetX = offsetX;
    Compat.postInvalidateOnAnimation(this);
  }


  public double getOffsetY() {
    return mOffsetY;
  }


  public void setOffsetY(double offsetY) {
    mOffsetY = offsetY;
    Compat.postInvalidateOnAnimation(this);
  }


  public double getScale() {
    return mScale;
  }


  public void setScale(double scale) {
    if (mScale !=scale) {
      Compat.postInvalidateOnAnimation(this);
    }
    mScale = scale;
  }


  public int getGridSize() {
    return mGridSize;
  }


  public void setGridSize(int gridSize) {
    mGridSize = gridSize;
  }

  public DiagramAdapter<?,?> getAdapter() {
    return mAdapter;
  }

  public void setAdapter(DiagramAdapter<?, ?> diagram) {
    mAdapter = diagram;
    updateZoomControlButtons();
    postInvalidate();
  }


  public OnNodeClickListener getOnNodeClickListener() {
    return aOnNodeClickListener;
  }


  public void setOnNodeClickListener(OnNodeClickListener nodeClickListener) {
    aOnNodeClickListener = nodeClickListener;
  }

  protected void highlightTouch(int touchedElement) {
    LightView lv = mAdapter.getView(touchedElement);
    if (! lv.isTouched()) {
      lv.setTouched(true);
      invalidate(touchedElement);
    }
  }

  /**
   * Invalidate the item at the given position.
   * @param position
   */
  private void invalidate(int position) {
    LightView lv = mAdapter.getView(position);
    invalidate(lv);
  }

  public void invalidate(LightView view) {
    view.getBounds(aTmpRectF);
    outset(aTmpRectF, INVALIDATE_MARGIN);
    toCanvasRect(aTmpRectF, aTmpRect);
    invalidate(aTmpRect);
  }

  private static void outset(RectF rect, float outset) {
    rect.left-=outset;
    rect.top-=outset;
    rect.right+=outset;
    rect.bottom+=outset;
  }

  private void toCanvasRect(RectF source, Rect dest) {
    dest.left=(int) Math.floor(toCanvasX(source.left));
    dest.top=(int) Math.floor(toCanvasY(source.top));
    dest.right = (int) Math.ceil(toCanvasX(source.right));
    dest.bottom = (int) Math.ceil(toCanvasY(source.bottom));
  }

  /**
   * Get the element touched by the given event.
   *
   * @param event The event to analyse.
   * @return The index of the touched element, or <code>-1</code> when none.
   */
  private int getTouchedElement(MotionEvent event) {
    int idx = event.getActionIndex();
    float x = event.getX(idx);
    float diagX = toDiagramX(x);
    float y = event.getY(idx);
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
    final int len = mAdapter.getCount();
    for(int i=0;i < len ; ++i) {
      getItemBounds(i, aTmpRectF);
      if (aTmpRectF.contains(diagX, diagY)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public void onDraw(Canvas canvas) {
    if (aTouchActionOptimize) {
      ensureValidCache();

      aTmpRectF.left = (float) ((aCacheRect.left- mOffsetX)* mScale);
      aTmpRectF.top = (float) ((aCacheRect.top- mOffsetY)* mScale);
      aTmpRectF.right = (float) (aTmpRectF.left+aCacheRect.width* mScale);
      aTmpRectF.bottom = (float) (aTmpRectF.top+aCacheRect.height* mScale);

      canvas.drawBitmap(aCacheBitmap, null, aTmpRectF, null);
    } else {

      drawDiagram(canvas);

      drawDecorations(canvas);

      drawOverlay(canvas);

      if (BuildConfig.DEBUG) {
        drawDebugOverlay(canvas);
      }

    }
  }

  private void ensureValidCache() {
    if (aCacheBitmap!=null) {
      double oldScale = aCacheBitmap.getHeight()/aCacheRect.height;
      final double scaleChange = mScale /oldScale;
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
      aCacheBitmap = Bitmap.createBitmap((int) Math.ceil(aCacheRect.width* mScale), (int) Math.ceil(aCacheRect.height* mScale), Bitmap.Config.ARGB_8888);
      aCacheCanvas = new Canvas(aCacheBitmap);
      aCacheBitmap.eraseColor(0x00000000);

      aCacheCanvas.translate((float)((mOffsetX - aCacheRect.left)* mScale), (float) ((mOffsetY - aCacheRect.top)* mScale));

      drawDiagram(aCacheCanvas);
      drawOverlay(aCacheCanvas);
    }
  }

  private void drawDiagram(Canvas canvas) {
    if (mAdapter !=null) {
      LightView bg = mAdapter.getBackground();
      Theme<AndroidStrategy, AndroidPen, AndroidPath> theme = mAdapter.getTheme();
      if (bg!=null) {
        drawPositioned(canvas, theme, bg);
      }

      int len = mAdapter.getCount();
      for(int i=0; i<len; i++) {
        drawPositioned(canvas, theme, mAdapter.getView(i));
      }

      LightView overlay = mAdapter.getOverlay();
      if (overlay!=null) {
        drawPositioned(canvas, theme, overlay);
      }
    } else {
      ensureMissingDiagramTextBounds();
      canvas.drawText(aMissingDiagramText, (getWidth()-aMissingDiagramTextBounds.width())/2, (getHeight()-aMissingDiagramTextBounds.height())/2, getRedPen());
    }
  }

  private void drawPositioned(Canvas canvas, Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, LightView view) {
    view.getBounds(aTmpRectF);
    int save = canvas.save();
    canvas.translate(toCanvasX(aTmpRectF.left), toCanvasY(aTmpRectF.top));
    view.draw(canvas, theme, mScale);
    canvas.restoreToCount(save);
  }

  private void drawDecorations(Canvas canvas) {
    // TODO handle decoration touches
    if (mAdapter !=null) {
      aDecorations.clear();
      Theme<AndroidStrategy, AndroidPen, AndroidPath> theme = mAdapter.getTheme();
      int len = mAdapter.getCount();
      for(int i=0; i<len; i++) {
        final LightView lv = mAdapter.getView(i);
        List<? extends RelativeLightView> decorations = mAdapter.getRelativeDecorations(i, mScale, lv.isSelected());
        for(RelativeLightView decoration: decorations) {
          aDecorations.add(Tupple.tupple(Integer.valueOf(i), decoration));
          int savePos = canvas.save();
          decoration.getBounds(aTmpRectF);
          canvas.translate(toCanvasX(aTmpRectF.left), toCanvasY(aTmpRectF.top));
          decoration.draw(canvas, theme, mScale);
          canvas.restoreToCount(savePos);
        }
      }
    }
  }

  public float toCanvasX(double x) {
    return (float) ((x- mOffsetX)* mScale);
  }

  public float toCanvasY(double y) {
    return (float) ((y- mOffsetY)* mScale);
  }

  public float toDiagramX(float x) {
    return (float) (x/ mScale + mOffsetX);
  }

  public float toDiagramY(float y) {
    return (float) (y/ mScale + mOffsetY);
  }

  private void getItemBounds(int pos, RectF rect) {
    mAdapter.getView(pos).getBounds(rect);
  }

  private void drawOverlay(Canvas canvas) {
    int canvasSave = canvas.save();
    if (aOverlay!=null) {
      final float scalef = (float) mScale;
      canvas.scale(scalef, scalef);
      if (aOverlay instanceof DiagramDrawable) {
        ((DiagramDrawable) aOverlay).draw(canvas, mScale);
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

  private void updateCacheRect(Rectangle cacheRect) {
    RectF diagrambounds = aTmpRectF;
    mAdapter.getBounds(diagrambounds);
    double diagLeft = Math.max(diagrambounds.left-1, mOffsetX -CACHE_PADDING);
    double diagWidth = Math.min(diagrambounds.right-diagLeft+6, (getWidth()/ mScale) + (CACHE_PADDING*2));
    double diagTop = Math.max(diagrambounds.top-1, mOffsetY -CACHE_PADDING);
    double diagHeight = Math.min(diagrambounds.bottom-diagTop+6, (getHeight()/ mScale) + (CACHE_PADDING*2));
    cacheRect.set(diagLeft, diagTop, diagWidth, diagHeight);
  }

  public void setOverlay(android.graphics.drawable.Drawable overlay) {
    if (aOverlay!=null) {
      invalidate(aOverlay.getBounds());
    }
    aOverlay = overlay;
    if (overlay!=null) {
      invalidate(overlay.getBounds());
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getActionMasked();
    int touchedElement = -1;
    int idx = event.getActionIndex();
    float diagX = toDiagramX(event.getX(idx));
    float diagY = toDiagramY(event.getY(idx));
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
      final int len = mAdapter.getCount();
      for(int i=0; i<len ; ++i) {
        LightView lv = mAdapter.getView(i);
        lv.setTouched(false);
      }

      if (aTouchedDecoration!=null) {
        aTouchedDecoration.getElem2().setTouched(false);
        if (isEditable()) {
          aTouchedDecoration.getElem2().getBounds(aTmpRectF);
          if (aTmpRectF.contains(diagX, diagY)) {
            mAdapter.onDecorationClick(this, aTouchedDecoration.getElem1().intValue(), aTouchedDecoration.getElem2());
          } else {
            mAdapter.onDecorationUp(this, aTouchedDecoration.getElem1().intValue(), aTouchedDecoration.getElem2(), diagX, diagY);
          }
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
    } else if (action==MotionEvent.ACTION_MOVE && aTouchedDecoration!=null && isEditable()) {
      mAdapter.onDecorationMove(this, aTouchedDecoration.getElem1().intValue(), aTouchedDecoration.getElem2(), diagX, diagY);
      return true;
    }
    boolean retVal = aScaleGestureDetector.onTouchEvent(event);
    retVal = aGestureDetector.onTouchEvent(event) || retVal;
    return retVal || super.onTouchEvent(event);
  }



  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  @Override
  public boolean onGenericMotionEvent(MotionEvent event) {
    int action = event.getActionMasked();
    if (action==MotionEvent.ACTION_SCROLL) {
      boolean zoomIn = Compat.isZoomIn(event);
      onZoom(zoomIn);
    }
    return super.onGenericMotionEvent(event);
  }

  @Override
  public void onVisibilityChanged(boolean visible) {
    // Part of zoomcontroller
    // ignore it
  }



  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    if (visibility==View.VISIBLE) {
      showZoomController();
    } else {
      dismissZoomController();
    }

    super.onWindowVisibilityChanged(visibility);
  }

  @Override
  protected void onVisibilityChanged(View changedView, int visibility) {
    if (mZoomController!=null) {
      boolean show = visibility==View.VISIBLE;
      if (show!=mZoomController.isVisible()) {
        mZoomController.setVisible(show);
        updateZoomControlButtons();
      }
    }
  }

  @Override
  public void onZoom(boolean zoomIn) {
    final double newScale;
    if (zoomIn) {
      newScale = getScale()*1.2;
    } else {
      newScale = getScale()/1.2;
    }
    setScale(newScale);
    updateZoomControlButtons();
  }

  public void updateZoomControlButtons() {
    if (mZoomController!=null) {
      mZoomController.setZoomInEnabled(mAdapter !=null && (getScale()*1.2)<MAXSCALE);
      mZoomController.setZoomOutEnabled(mAdapter !=null && (getScale()/1.2)>MINSCALE);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
//    showZoomController();
  }

  @Override
  protected void onDetachedFromWindow() {
    dismissZoomController();
    super.onDetachedFromWindow();
  }

  private void showZoomController() {
    if (mEditable && (! (aMultitouch || isInEditMode()))) {
      if (mZoomController==null) { mZoomController = new ZoomButtonsController(this); }
      mZoomController.setOnZoomListener(this);
      mZoomController.setAutoDismissed(false);
      if (!mZoomController.isVisible()) {
        mZoomController.setVisible(getVisibility()==VISIBLE);
      }
      updateZoomControlButtons();
    }
  }

  private void dismissZoomController() {
    if (mZoomController!=null) {
      mZoomController.setAutoDismissed(true);
      mZoomController.setVisible(false);
      ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).removeViewImmediate(mZoomController.getContainer());
      mZoomController = null;
    }
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    return new DiagramViewState(this, super.onSaveInstanceState());
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    if (!(state instanceof DiagramViewState)) {
      super.onRestoreInstanceState(state);
      return;
    }
    DiagramViewState diagramViewState = (DiagramViewState) state;
    super.onRestoreInstanceState(diagramViewState.getSuperState());
    mOffsetX = diagramViewState.mOffsetX;
    mOffsetY = diagramViewState.mOffsetY;
    mScale = diagramViewState.mScale;
    mGridSize = diagramViewState.mGridSize;
  }

  public void setSelection(int position) {
    // TODO handle more complicated selection.
    int len = mAdapter.getCount();
    for(int i=0; i<len; ++i) {
      mAdapter.getView(i).setSelected(i==position);
    }
  }

  public int getSelection() {
    int len = mAdapter.getCount();
    for(int i=0; i<len; ++i) {
      if (mAdapter.getView(i).isSelected()) {
        return i;
      }
    }
    return -1;
  }


  public boolean isEditable() {
    return mEditable;
  }


  public void setEditable(boolean editable) {
    mEditable = editable;
    dismissZoomController();
  }

}
