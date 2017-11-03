/*
 * Copyright (c) 2017.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.diagram.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.*;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;
import net.devrieze.util.Tupple;
import nl.adaptivity.android.compat.Compat;
import nl.adaptivity.diagram.Point;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import nl.adaptivity.process.editor.android.BuildConfig;
import nl.adaptivity.process.editor.android.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DiagramView extends View implements OnZoomListener {



  private static class DiagramViewStateCreator implements Creator<DiagramViewState> {

    @NonNull
    @Override
    public DiagramViewState createFromParcel(@NonNull final Parcel source) {
      return new DiagramViewState(source);
    }

    @NonNull
    @Override
    public DiagramViewState[] newArray(final int size) {
      return new DiagramViewState[size];
    }

  }

  private static class DiagramViewState extends View.BaseSavedState {

    @SuppressWarnings({ "unused", "hiding" })
    public static final Parcelable.Creator<DiagramViewState> CREATOR = new DiagramViewStateCreator();

    private final double mOffsetX;
    private final double mOffsetY;
    private final double mScale;
    private final int    mGridSize;

    public DiagramViewState(final DiagramView diagramView, final Parcelable viewState) {
      super(viewState);
      mOffsetX = diagramView.mOffsetX;
      mOffsetY = diagramView.mOffsetY;
      mScale = diagramView.mScale;
      mGridSize = diagramView.mGridSize;
    }

    public DiagramViewState(@NonNull final Parcel source) {
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
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
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
    boolean onNodeClicked(DiagramView view, int touchedElement, MotionEvent event);
  }

  private final class MyGestureListener extends SimpleOnGestureListener {

    private boolean mMoveItem = false;
    private int mMoving = -1;
    private double mOrigX;
    private double mOrigY;
    @Override
    public boolean onScroll(@NonNull final MotionEvent e1, @NonNull final MotionEvent e2, final float distanceX, final float distanceY) {
      if (isEditable()&& (mMoving>=0 || mMoveItem)) {
        final int touchedElement = mMoving >= 0 ? mMoving: getTouchedElement(e1);
        if (touchedElement>=0) {
          final double touchedElemX = mAdapter.getGravityX(touchedElement);
          final double touchedElemY = mAdapter.getGravityY(touchedElement);
          if (mMoving <0) {
            if (Math.max(Math.abs(distanceY),Math.abs(distanceX))>MIN_DRAG_DIST) {
              mMoving = touchedElement;
              mOrigX = touchedElemX;
              mOrigY = touchedElemY;
              // Cancel the selection on drag.
              setSelection(-1);
            }
          }
          if (mMoving>=0) {
            final LightView lv   = mAdapter.getView(touchedElement);
            Point attractor = mAdapter.closestAttractor(touchedElement, toDiagramX(e2.getX()), toDiagramY(e2.getY()));

            if ((attractor != null) &&
                (attractor.distanceTo(Point.of(toDiagramX(e2.getX()), toDiagramY(e2.getY()))) < mGridSize)) {
              mAdapter.setPos(touchedElement, attractor.x, attractor.y);
            } else if (mGridSize>0) {
              final double dX   = (e2.getX() - e1.getX()) / mScale;
              final float  newX = Math.round((mOrigX + dX) / mGridSize) * mGridSize;
              final double dY   = (e2.getY() - e1.getY()) / mScale;
              final float  newY = Math.round((mOrigY + dY) / mGridSize) * mGridSize;
              mAdapter.setPos(touchedElement, newX, newY);
            } else {
              mAdapter.setPos(touchedElement, touchedElemX - (distanceX/ mScale), touchedElemY - (distanceY/mScale));
            }
            invalidate();
          }
          return true;
        }
        return false;
      } else {
        final double scale = getScale();
        setOffsetX(getOffsetX() + (distanceX / scale));
        setOffsetY(getOffsetY() + (distanceY / scale));
        return true;
      }
    }

    @Override
    public void onShowPress(@NonNull final MotionEvent e) {
      final int touchedElement = getTouchedElement(e);
      if (touchedElement>=0) highlightTouch(touchedElement);
    }

    /**
     * Allow for specifying whether the move is for a single item, or for the
     * canvas.
     *
     * @param value <code>true</code> when the focus is an element,
     *          <code>false</code> when not.
     */
    public void setMoveItem(final boolean value) {
      mMoveItem = value;
    }

    @Override
    public boolean onSingleTapUp(@NonNull final MotionEvent e) {
      try {
        final int touchedElement = getTouchedElement(e);
        if (touchedElement>=0 && isEditable()) {
          if (mAdapter.onNodeClickOverride(DiagramView.this, touchedElement, e)) {
            return true;
          }
          if (mOnNodeClickListener!=null) {
            if(mOnNodeClickListener.onNodeClicked(DiagramView.this, touchedElement, e)) {
              return true;
            }
          }
        }
        return false;
      } finally {
        mLastTouchedElement=0;
      }
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
      try {
        return super.onSingleTapUp(e);
      } finally {
        mLastTouchedElement = 0;
      }
    }

    public void actionFinished() {
      mMoveItem = false;
      mMoving = -1;
    }


  }

  public static abstract class DiagramDrawable extends android.graphics.drawable.Drawable {

    @Override
    public final void draw(final Canvas canvas) {
      draw(canvas, 1d);
    }

    public abstract void draw(Canvas canvas, double scale);

  }

  public static final double DENSITY = Resources.getSystem().getDisplayMetrics().density;
  private static final double MAXSCALE = 6d*DENSITY;
  private static final double MINSCALE = 0.5d*DENSITY;
  private static final float MIN_DRAG_DIST = (float) (8*DENSITY);
  private DiagramAdapter<?,?> mAdapter;
  private Paint mRed;
  private Paint mTimePen;
  private int mLastTouchedElement = -1;
  private final Rect mMissingDiagramTextBounds = new Rect();
  private String mMissingDiagramText;
  private double mOffsetX = 0;
  private double mOffsetY = 0;
  @Nullable private android.graphics.drawable.Drawable mOverlay;
  @Nullable private ZoomButtonsController              mZoomController;
  private final     boolean                            mMultitouch;
  private double mScale =DENSITY*160/96; // Use density of 96dpi for drawables
  @NonNull private final GestureDetector      mGestureDetector;
  @NonNull private final ScaleGestureDetector mScaleGestureDetector;

  @Nullable private OnNodeClickListener mOnNodeClickListener = null;

  private final MyGestureListener mGestureListener = new MyGestureListener();

  private final OnScaleGestureListener mScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

    // These points are relative to the diagram. They should end up at the focal
    // point in canvas coordinates.
    double mPreviousDiagramX;
    double mPreviousDiagramY;


    @Override
    public boolean onScaleBegin(final ScaleGestureDetector detector) {
      final double newScale = getScale() * detector.getScaleFactor();
      mPreviousDiagramX = getOffsetX() + detector.getFocusX() / newScale;
      mPreviousDiagramY = getOffsetY() + detector.getFocusY() / newScale;

      return super.onScaleBegin(detector);
    }


    @Override
    public boolean onScale(final ScaleGestureDetector detector) {
      final double scaleAdjust = detector.getScaleFactor();
      final double newScale    = getScale() * scaleAdjust;
      if (newScale<=MAXSCALE && newScale>=MINSCALE) {
        setScale(newScale);

        final double newDiagramX = detector.getFocusX() / newScale;
        final double newDiagramY = detector.getFocusY() / newScale;

        setOffsetX(mPreviousDiagramX - newDiagramX);
        setOffsetY(mPreviousDiagramY - newDiagramY);

        return true;
      }
      return false;
    }

  };

  private boolean mTouchActionOptimize = false;
  @Nullable private Bitmap mCacheBitmap;
  @Nullable private Canvas mCacheCanvas;
  @NonNull private Rectangle mCacheRect = new Rectangle(0d, 0d, 0d, 0d);

  private final Rect mBuildTimeBounds = new Rect();
  private String mBuildTimeText;
  private final Rectangle mTmpRectangle = new Rectangle(0d, 0d, 0d, 0d);
  private final RectF     mTmpRectF     = new RectF();
  private final Rect      mTmpRect      = new Rect();
  private int mGridSize;
  private final     List<Tupple<Integer, RelativeLightView>> mDecorations       = new ArrayList<>();
  @Nullable private Tupple<Integer,RelativeLightView>        mTouchedDecoration = null;

  private boolean mEditable = true;

  private double mMaxScale;

  private static final int DEFAULT_GRID_SIZE=8;
  private static final float DEFAULT_MAX_SCALE= (float) (2 * DENSITY);
  private Paint mBoundsPaint;

  public DiagramView(@NonNull final Context context, final AttributeSet attrs, final int defStyle) {
    super(context, attrs, defStyle);
    mMultitouch = (isNotEmulator()) && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    mGestureDetector = new GestureDetector(context, mGestureListener);
    mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
    init(attrs);
  }

  public DiagramView(@NonNull final Context context, final AttributeSet attrs) {
    super(context, attrs);
    mMultitouch = (!isInEditMode()) &&(isNotEmulator()) && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    mGestureDetector = new GestureDetector(context, mGestureListener);
    mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
    init(attrs);
  }

  public DiagramView(@NonNull final Context context) {
    super(context);
    mMultitouch = (isNotEmulator()) && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    mGestureDetector = new GestureDetector(context, mGestureListener);
    mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
    init(null);
  }

  public void init(@Nullable final AttributeSet attrs) {
    if (attrs==null) {
      mGridSize=DEFAULT_GRID_SIZE;
      mMaxScale=DEFAULT_MAX_SCALE;
    } else {
      final TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.DiagramView, 0, 0);
      try {
        mGridSize = a.getInteger(R.styleable.DiagramView_gridSize, DEFAULT_GRID_SIZE);
        mEditable = a.getBoolean(R.styleable.DiagramView_editable, true);
        mMaxScale = a.getFloat(R.styleable.DiagramView_maxScale, Float.NaN)*DENSITY;
        if (Double.isNaN(mMaxScale)) { mMaxScale = DEFAULT_MAX_SCALE; }
      } finally {
        a.recycle();
      }
    }
    setLayerType(LAYER_TYPE_SOFTWARE, null);

    mBoundsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mBoundsPaint.setStyle(Style.FILL_AND_STROKE);
    mBoundsPaint.setStrokeWidth(4);
    mBoundsPaint.setARGB(64,128,128,255);
  }

  private static boolean isNotEmulator() {
    return !"google_sdk".equals(Build.PRODUCT) && !"sdk_x86".equals(Build.PRODUCT) && !"sdk".equals(Build.PRODUCT);
  }

  public double getOffsetX() {
    return mOffsetX;
  }


  public void setOffsetX(final double offsetX) {
    mOffsetX = offsetX;
    Compat.postInvalidateOnAnimation(this);
  }


  public double getOffsetY() {
    return mOffsetY;
  }


  public void setOffsetY(final double offsetY) {
    mOffsetY = offsetY;
    Compat.postInvalidateOnAnimation(this);
  }


  public double getScale() {
    return mScale;
  }


  public void setScale(double scale) {
    scale = Math.min(mMaxScale, scale);
    if (mScale !=scale) {
      Compat.postInvalidateOnAnimation(this);
    }
    mScale = scale;
  }


  public int getGridSize() {
    return mGridSize;
  }


  public void setGridSize(final int gridSize) {
    mGridSize = gridSize;
  }

  public DiagramAdapter<?,?> getAdapter() {
    return mAdapter;
  }

  public void setAdapter(final DiagramAdapter<?, ?> diagram) {
    mAdapter = diagram;
    updateZoomControlButtons();
    postInvalidate();
  }


  @Nullable
  public OnNodeClickListener getOnNodeClickListener() {
    return mOnNodeClickListener;
  }


  public void setOnNodeClickListener(final OnNodeClickListener nodeClickListener) {
    mOnNodeClickListener = nodeClickListener;
  }

  protected void highlightTouch(final int touchedElement) {
    final LightView lv = mAdapter.getView(touchedElement);
    if (! lv.isTouched()) {
      lv.setTouched(true);
      invalidate(touchedElement);
    }
  }

  /**
   * Invalidate the item at the given position.
   * @param position
   */
  private void invalidate(final int position) {
    final LightView lv = mAdapter.getView(position);
    invalidate(lv);
  }

  public void invalidate(@NonNull final LightView view) {
    view.getBounds(mTmpRectF);
    outset(mTmpRectF, INVALIDATE_MARGIN);
    toCanvasRect(mTmpRectF, mTmpRect);
    invalidate(mTmpRect);
  }

  private static void outset(final RectF rect, final float outset) {
    rect.left-=outset;
    rect.top-=outset;
    rect.right+=outset;
    rect.bottom+=outset;
  }

  private void toCanvasRect(final RectF source, final Rect dest) {
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
  private int getTouchedElement(final MotionEvent event) {
    final int   idx   = event.getActionIndex();
    final float x     = event.getX(idx);
    final float diagX = toDiagramX(x);
    final float y     = event.getY(idx);
    final float diagY = toDiagramY(y);


//    final int pIdx = pE.getActionIndex();
//    float canvX = pE.getX(pIdx);
//    float canvY = pE.getY(pIdx);
    if (mLastTouchedElement>=0) {
      getItemBounds(mLastTouchedElement, mTmpRectF);
      if (mTmpRectF.contains(diagX, diagY)) {
        return mLastTouchedElement;
      }
    }
    mLastTouchedElement = findTouchedElement(diagX, diagY);
    return mLastTouchedElement;
  }

  protected int findTouchedElement(final float diagX, final float diagY) {
    final int len = mAdapter.getCount();
    for(int i=0;i < len ; ++i) {
      getItemBounds(i, mTmpRectF);
      if (mTmpRectF.contains(diagX, diagY)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public void onDraw(@NonNull final Canvas canvas) {
    if (mTouchActionOptimize) {
      ensureValidCache();

      mTmpRectF.left = (float) ((mCacheRect.left - mOffsetX) * mScale) + getPaddingLeft();
      mTmpRectF.top = (float) ((mCacheRect.top - mOffsetY) * mScale) + getPaddingTop();
      mTmpRectF.right = (float) (mTmpRectF.left+mCacheRect.width* mScale);
      mTmpRectF.bottom = (float) (mTmpRectF.top+mCacheRect.height* mScale);

      canvas.drawBitmap(mCacheBitmap, null, mTmpRectF, null);
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
    if (mCacheBitmap!=null) {
      final double oldScale    = mCacheBitmap.getHeight() / mCacheRect.height;
      final double scaleChange = mScale /oldScale;
      if (scaleChange > 1.5 || scaleChange<0.34) {
        mCacheBitmap=null; // invalidate
        updateCacheRect(mCacheRect);
      } else {
        updateCacheRect(mTmpRectangle);
        if (mTmpRectangle.left+CACHE_PADDING < mCacheRect.left ||
            mTmpRectangle.top+CACHE_PADDING < mCacheRect.top ||
            mTmpRectangle.right()-CACHE_PADDING < mCacheRect.right() ||
            mTmpRectangle.bottom()-CACHE_PADDING < mCacheRect.bottom()) {
          mCacheBitmap = null;
          mCacheRect = mTmpRectangle;
        }
      }
    } else {
      updateCacheRect(mCacheRect); //cacherect is in screen scale
    }
    if (mCacheBitmap==null) {
      mCacheBitmap = Bitmap.createBitmap((int) Math.ceil(mCacheRect.width* mScale), (int) Math.ceil(mCacheRect.height* mScale), Bitmap.Config.ARGB_8888);
      mCacheCanvas = new Canvas(mCacheBitmap);
      mCacheBitmap.eraseColor(0x00000000);

      mCacheCanvas.translate((float)((mOffsetX - mCacheRect.left) * mScale) - getPaddingLeft(), (float) ((mOffsetY - mCacheRect.top) * mScale) - getPaddingTop());

      drawDiagram(mCacheCanvas);
      drawOverlay(mCacheCanvas);
    }
  }

  private void drawDiagram(@NonNull final Canvas canvas) {
    if (mAdapter !=null) {
      final LightView                                       bg    = mAdapter.getBackground();
      final Theme<AndroidStrategy, AndroidPen, AndroidPath> theme = mAdapter.getTheme();
      if (bg!=null) {
        drawPositioned(canvas, theme, bg);
      }

      final int len = mAdapter.getCount();
      for(int i=0; i<len; i++) {
        drawPositioned(canvas, theme, mAdapter.getView(i));
      }

      final LightView overlay = mAdapter.getOverlay();
      if (overlay!=null) {
        drawPositioned(canvas, theme, overlay);
      }
    } else {
      ensureMissingDiagramTextBounds();
      canvas.drawText(mMissingDiagramText, (getWidth()-mMissingDiagramTextBounds.width())/2, (getHeight()-mMissingDiagramTextBounds.height())/2, getRedPen());
    }
  }

  private void drawPositioned(final Canvas canvas, final Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, final LightView view) {
    view.getBounds(mTmpRectF);
    final int save = canvas.save();
    canvas.translate(toCanvasX(mTmpRectF.left), toCanvasY(mTmpRectF.top));
    view.draw(canvas, theme, mScale);
    canvas.restoreToCount(save);
  }

  private void drawDecorations(@NonNull final Canvas canvas) {
    // TODO handle decoration touches
    if (mAdapter !=null) {
      mDecorations.clear();
      final Theme<AndroidStrategy, AndroidPen, AndroidPath> theme = mAdapter.getTheme();
      final int                                             len   = mAdapter.getCount();
      for(int i=0; i<len; i++) {
        final LightView                         lv          = mAdapter.getView(i);
        final List<? extends RelativeLightView> decorations = mAdapter.getRelativeDecorations(i, mScale, lv.isSelected());
        for(final RelativeLightView decoration: decorations) {
          mDecorations.add(Tupple.tupple(Integer.valueOf(i), decoration));
          final int savePos = canvas.save();
          decoration.getBounds(mTmpRectF);
          canvas.translate(toCanvasX(mTmpRectF.left), toCanvasY(mTmpRectF.top));
          decoration.draw(canvas, theme, mScale);
          canvas.restoreToCount(savePos);
        }
      }
    }
  }

  public float toCanvasX(final double x) {
    return (float) ((x - mOffsetX) * mScale) + getPaddingLeft();
  }

  public float toCanvasY(final double y) {
    return (float) ((y - mOffsetY) * mScale) + getPaddingTop();
  }

  public float toDiagramX(final float x) {
    return (float) ((x-getPaddingLeft())/ mScale + mOffsetX);
  }

  public float toDiagramY(final float y) {
    return (float) ((y-getPaddingTop())/ mScale + mOffsetY);
  }

  private void getItemBounds(final int pos, final RectF rect) {
    mAdapter.getView(pos).getBounds(rect);
  }

  private void drawOverlay(final Canvas canvas) {
    final int canvasSave = canvas.save();
    if (mOverlay!=null) {
      final float scalef = (float) mScale;
      canvas.scale(scalef, scalef);
      if (mOverlay instanceof DiagramDrawable) {
        ((DiagramDrawable) mOverlay).draw(canvas, mScale);
      } else {
        mOverlay.draw(canvas);
      }
    }
    canvas.restoreToCount(canvasSave);
  }

  private void drawDebugOverlay(@NonNull final Canvas canvas) {
    if (mBuildTimeText!=null) {
      ensureBuildTimeTextBounds();

      canvas.drawText(mBuildTimeText, getWidth() - mBuildTimeBounds.width()-20, getHeight()-20, mTimePen);
    }
  }

  private void ensureBuildTimeTextBounds() {
    if (mBuildTimeText==null) {
      final InputStream stream = getClass().getClassLoader().getResourceAsStream("nl/adaptivity/process/diagram/version.properties");
      if (stream!=null) {
        try {
          final Properties props = new Properties();
          try {
            props.load(stream);
          } catch (IOException e) {
            e.printStackTrace();
            mBuildTimeText="";
            mBuildTimeBounds.set(0, 0, 0, 0);
            return;
          }
          mBuildTimeText = props.getProperty("buildtime");
          if (mBuildTimeText!=null) {
            mTimePen.getTextBounds(mBuildTimeText, 0, mBuildTimeText.length(), mBuildTimeBounds);
          } else {
            mBuildTimeText = "";
            mBuildTimeBounds.set(0, 0, 0, 0);
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
    if (mTimePen==null) {
      mTimePen = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
      mTimePen.setARGB(255, 0, 0, 31);
      mTimePen.setTypeface(Typeface.DEFAULT);
      mTimePen.setTextSize(25f);
    }
  }

  private void ensureMissingDiagramTextBounds() {
    if (mMissingDiagramText==null) {
      mMissingDiagramText = getContext().getResources().getString(R.string.missing_diagram);
      getRedPen().getTextBounds(mMissingDiagramText, 0, mMissingDiagramText.length(), mMissingDiagramTextBounds);
    }
  }

  private Paint getRedPen() {
    if (mRed==null) {
      mRed = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
      mRed.setARGB(255, 255, 0, 0);
      mRed.setTypeface(Typeface.DEFAULT);
      mRed.setTextSize(50f);
    }
    return mRed;
  }

  private void updateCacheRect(final Rectangle cacheRect) {
    final RectF diagrambounds = mTmpRectF;
    mAdapter.getBounds(diagrambounds);
    final double diagLeft   = Math.max(diagrambounds.left - 1, mOffsetX - CACHE_PADDING - getPaddingLeft());
    final double diagWidth  = Math.min(diagrambounds.right - diagLeft + 6, (getWidth() / mScale) + (CACHE_PADDING * 2)+getPaddingLeft()+getPaddingRight());
    final double diagTop    = Math.max(diagrambounds.top - 1, mOffsetY - CACHE_PADDING - getPaddingTop());
    final double diagHeight = Math.min(diagrambounds.bottom - diagTop + 6, (getHeight() / mScale) + (CACHE_PADDING * 2)+getPaddingTop()+getPaddingBottom());
    cacheRect.set(diagLeft, diagTop, diagWidth, diagHeight);
  }

  public void setOverlay(@Nullable final android.graphics.drawable.Drawable overlay) {
    if (mOverlay!=null) {
      invalidate(mOverlay.getBounds());
    }
    mOverlay = overlay;
    if (overlay!=null) {
      invalidate(overlay.getBounds());
    }
  }

  @Override
  public boolean onTouchEvent(@NonNull final MotionEvent event) {
    final int   action         = event.getActionMasked();
    int         touchedElement = -1;
    final int   idx            = event.getActionIndex();
    final float diagX          = toDiagramX(event.getX(idx));
    final float diagY          = toDiagramY(event.getY(idx));
    if (action==MotionEvent.ACTION_DOWN) {

      touchedElement = findTouchedElement(diagX, diagY);
      if (touchedElement>=0) {
        highlightTouch(touchedElement);
        mGestureListener.setMoveItem(true);
        if (mTouchedDecoration!=null) {
          mTouchedDecoration.getElem2().setTouched(false);
          invalidate(mTouchedDecoration.getElem2());
          mTouchedDecoration =null;
        }
      } else {
        if (mTouchedDecoration!=null) {
          mTouchedDecoration.getElem2().getBounds(mTmpRectF);
          if (! mTmpRectF.contains(diagX, diagY)) {
            mTouchedDecoration.getElem2().setTouched(false);
            invalidate(mTouchedDecoration.getElem2());
            mTouchedDecoration = null;
          }
        }
        if (mTouchedDecoration==null) {
          for(final Tupple<Integer,RelativeLightView> decoration: mDecorations) {
            decoration.getElem2().getBounds(mTmpRectF);
            if (mTmpRectF.contains(diagX, diagY)) {
              mTouchedDecoration = decoration;
              mTouchedDecoration.getElem2().setTouched(true);
              invalidate(mTouchedDecoration.getElem2());
              break;
            }
          }
        }
      }

//    if (BuildConfig.DEBUG) {
//    Debug.startMethodTracing();
//  }
//      mTouchActionOptimize  = true;
//      ensureValidCache();
    } else if (action==MotionEvent.ACTION_UP|| action==MotionEvent.ACTION_CANCEL) {
      final int len = mAdapter.getCount();
      for(int i=0; i<len ; ++i) {
        final LightView lv = mAdapter.getView(i);
        lv.setTouched(false);
      }

      if (mTouchedDecoration!=null) {
        mTouchedDecoration.getElem2().setTouched(false);
        if (isEditable()) {
          mTouchedDecoration.getElem2().getBounds(mTmpRectF);
          if (mTmpRectF.contains(diagX, diagY)) {
            mAdapter.onDecorationClick(this, mTouchedDecoration.getElem1().intValue(), mTouchedDecoration.getElem2());
          } else {
            mAdapter.onDecorationUp(this, mTouchedDecoration.getElem1().intValue(), mTouchedDecoration.getElem2(), diagX, diagY);
          }
        }

        invalidate(mTouchedDecoration.getElem2());
        mTouchedDecoration = null;
      }

      mTouchActionOptimize  = false;
      mCacheBitmap = null; mCacheCanvas = null;
      Compat.postInvalidateOnAnimation(this);
//    if (BuildConfig.DEBUG) {
//      Debug.stopMethodTracing();
//    }
      mGestureListener.actionFinished();
//      mGestureListener.setMoveItem(false);
    } else if (action==MotionEvent.ACTION_MOVE && mTouchedDecoration!=null && isEditable()) {
      mAdapter.onDecorationMove(this, mTouchedDecoration.getElem1().intValue(), mTouchedDecoration.getElem2(), diagX, diagY);
      return true;
    }
    boolean retVal = mScaleGestureDetector.onTouchEvent(event);
    retVal = mGestureDetector.onTouchEvent(event) || retVal;
    return retVal || super.onTouchEvent(event);
  }



  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  @Override
  public boolean onGenericMotionEvent(@NonNull final MotionEvent event) {
    final int action = event.getActionMasked();
    if (action==MotionEvent.ACTION_SCROLL) {
      final boolean zoomIn = Compat.isZoomIn(event);
      onZoom(zoomIn);
    }
    return super.onGenericMotionEvent(event);
  }

  @Override
  public void onVisibilityChanged(final boolean visible) {
    // Part of zoomcontroller
    // ignore it
  }



  @Override
  protected void onWindowVisibilityChanged(final int visibility) {
    if (visibility==View.VISIBLE) {
      showZoomController();
    } else {
      dismissZoomController();
    }

    super.onWindowVisibilityChanged(visibility);
  }

  @Override
  protected void onVisibilityChanged(final View changedView, final int visibility) {
    if (mZoomController!=null) {
      final boolean show = visibility == View.VISIBLE;
      if (show!=mZoomController.isVisible()) {
        mZoomController.setVisible(show);
        updateZoomControlButtons();
      }
    }
  }

  @Override
  public void onZoom(final boolean zoomIn) {
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
  protected void onDetachedFromWindow() {
    dismissZoomController();
    super.onDetachedFromWindow();
  }

  private void showZoomController() {
    if (mEditable && (! (mMultitouch || isInEditMode()))) {
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

  @NonNull
  @Override
  protected Parcelable onSaveInstanceState() {
    return new DiagramViewState(this, super.onSaveInstanceState());
  }

  @Override
  protected void onRestoreInstanceState(final Parcelable state) {
    if (!(state instanceof DiagramViewState)) {
      super.onRestoreInstanceState(state);
      return;
    }
    final DiagramViewState diagramViewState = (DiagramViewState) state;
    super.onRestoreInstanceState(diagramViewState.getSuperState());
    mOffsetX = diagramViewState.mOffsetX;
    mOffsetY = diagramViewState.mOffsetY;
    mScale = diagramViewState.mScale;
    mGridSize = diagramViewState.mGridSize;
  }

  public void setSelection(final int position) {
    // TODO handle more complicated selection.
    final int len = mAdapter.getCount();
    for(int i=0; i<len; ++i) {
      mAdapter.getView(i).setSelected(i==position);
    }
  }

  public int getSelection() {
    final int len = mAdapter.getCount();
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


  public void setEditable(final boolean editable) {
    mEditable = editable;
    dismissZoomController();
  }

}
