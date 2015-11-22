package nl.adaptivity.process.editor.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.android.*;
import nl.adaptivity.diagram.android.DiagramView.DiagramDrawable;
import nl.adaptivity.diagram.android.DiagramView.OnNodeClickListener;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.diagram.*;
import nl.adaptivity.process.editor.android.NodeEditDialogFragment.NodeEditListener;
import nl.adaptivity.process.editor.android.PMProcessesFragment.PMProvider;
import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static nl.adaptivity.diagram.Drawable.*;

public class PMEditor extends AppCompatActivity implements OnNodeClickListener, NodeEditListener, PMProvider {


  private static final String KEY_PROCESSMODEL = "processmodel";
  private static final String KEY_PROCESSMODEL_URI = "processmodeluri";
  private static final int ITEM_MARGIN = 8/*dp*/;
  private static final int STATE_ACTIVE=STATE_CUSTOM1;
  private static final int STATE_GROUP=STATE_CUSTOM2;
  private static final int STATE_XMOST=STATE_CUSTOM3;
  private static final int STATE_MOVED=STATE_CUSTOM4;



  private final class LineDrawable extends DiagramDrawable {

    private double mX1;
    private double mY1;
    private double mX2;
    private double mY2;
    private final Paint mPaint;

    public LineDrawable(double x1, double y1, double x2, double y2, Paint paint) {
      mX1 = x1;
      mX2 = x2;
      mY1 = y1;
      mY2 = y2;
      mPaint = paint;
    }

    @Override
    public void draw(Canvas canvas, double scale) {
      int height = (int) Math.round((canvas.getHeight()/scale));
      int width = (int) Math.round(canvas.getWidth()/scale);
      if (!Double.isNaN(mX1)) {
        float scaledX = (float) (mX1-diagramView1.getOffsetX());
        canvas.drawLine(scaledX, 0, scaledX, height, mPaint);
      }
      if (!Double.isNaN(mY1)) {
        float scaledY = (float) (mY1-diagramView1.getOffsetY());
        canvas.drawLine(0, scaledY, width, scaledY, mPaint);
      }
      if (!Double.isNaN(mX2)) {
        float scaledX = (float) (mX2-diagramView1.getOffsetX());
        canvas.drawLine(scaledX, 0, scaledX, height, mPaint);
      }
      if (!Double.isNaN(mY2)) {
        float scaledY = (float) (mY2-diagramView1.getOffsetY());
        canvas.drawLine(0, scaledY, width, scaledY, mPaint);
      }
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSPARENT;
    }

    @Override
    public void setAlpha(int alpha) {
      //
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
      //
    }

  }

  final static LayoutAlgorithm<DrawableProcessNode> NULL_LAYOUT_ALGORITHM = new LayoutAlgorithm<DrawableProcessNode>(){

    @Override
    public boolean layout(List<? extends DiagramNode<DrawableProcessNode>> nodes) {
      return false; // don't do any layout
    }

  };

  private static final String TAG = PMEditor.class.getName();
  public static final String EXTRA_PROCESS_MODEL = "nl.adaptivity.process.model";

  private static class MoveDrawable extends DiagramDrawable{

    private int mAlpha = 255;
    private List<float[]> mArrows;
    private Paint mPaint;
    private Drawable mMinMaxOverlay;

    public MoveDrawable(@Nullable Drawable minMaxOverlay, @NotNull List<float[]> arrows) {
      mMinMaxOverlay = minMaxOverlay;
      mArrows = arrows;
    }

    @Override
    public void draw(Canvas canvas, double scale) {
      if (mMinMaxOverlay!=null) {
        if (mMinMaxOverlay instanceof DiagramDrawable) {
          ((DiagramDrawable)mMinMaxOverlay).draw(canvas, scale);
        } else {
          mMinMaxOverlay.draw(canvas);
        }
      }
      if (mPaint ==null) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setAlpha(mAlpha);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(3);
        mPaint.setARGB(255, 0, 255, 0);
      }
      for(float[] arrow:mArrows) {
        canvas.drawLine(arrow[0], arrow[1], arrow[2], arrow[3], mPaint);
      }
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSPARENT;
    }

    @Override
    public void setAlpha(int alpha) {
      mAlpha = alpha;
      if (mPaint!=null) {
        mPaint.setAlpha(alpha);
      }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
      // ignore
    }

  }

  private static class WaitTask extends FutureTask<Object> {

    private static final Callable<Object> NULLCALLABLE = new Callable<Object>() {

      @Override
      public Object call() throws Exception {
        throw new UnsupportedOperationException("Should not be set");
      }

    };

    private final boolean mImmediate;

    private Drawable mOverlay;

    public WaitTask(boolean immediate, Drawable overlay) {
      super(NULLCALLABLE);
      mImmediate = immediate;
      mOverlay = overlay;
    }

    public void trigger() {
      set(Boolean.TRUE);
    }

    public boolean isImmediate() {
      return mImmediate;
    }
  }

  private class MyStepper extends LayoutStepper<DrawableProcessNode> {

    private boolean mImmediate = false;
    private AndroidPen mXMostPen;
    private DiagramNode<DrawableProcessNode> mLayoutNode;
    private double mMinX = Double.NaN;
    private double mMinY = Double.NaN;
    private double mMaxX = Double.NaN;
    private double mMaxY = Double.NaN;
    private LineDrawable mMinMaxOverlay = null;

    @Override
    public void reportLayoutNode(DiagramNode<DrawableProcessNode> node) {
      mMinX = Double.NaN;
      mMinY = Double.NaN;
      mMaxX = Double.NaN;
      mMaxY = Double.NaN;
      mMinMaxOverlay = null;
      setLabel("node under consideration");
      if (! (Double.isNaN(node.getX()) || Double.isNaN(node.getY()))) {
        if (mLayoutNode!=null) {
          final DrawableProcessNode target = mLayoutNode.getTarget();
          target.setState(target.getState()& ~STATE_ACTIVE);
        }
        mLayoutNode = node;
        {
          final DrawableProcessNode target = mLayoutNode.getTarget();
          target.setState(target.getState()|STATE_ACTIVE);
        }
        waitForNextClicked(null);
      }
    }

    @Override
    public void reportLowest(List<? extends DiagramNode<DrawableProcessNode>> nodes, DiagramNode<DrawableProcessNode> node) {
      setLabel("lowest");
      reportXMost(nodes, node);
    }

    @Override
    public void reportHighest(List<? extends DiagramNode<DrawableProcessNode>> nodes, DiagramNode<DrawableProcessNode> node) {
      setLabel("highest");
      reportXMost(nodes, node);
    }

    @Override
    public void reportRightmost(List<? extends DiagramNode<DrawableProcessNode>> nodes, DiagramNode<DrawableProcessNode> node) {
      setLabel("rightmost");
      reportXMost(nodes, node);
    }

    @Override
    public void reportLeftmost(List<? extends DiagramNode<DrawableProcessNode>> nodes, DiagramNode<DrawableProcessNode> node) {
      setLabel("leftmost");
      reportXMost(nodes, node);
    }

    @Override
    public void reportMinX(List<? extends DiagramNode<DrawableProcessNode>> nodes, double x) {
      setLabel("minX");
      mMinX = x;
      reportMinMax(nodes);
    }

    @Override
    public void reportMaxX(List<? extends DiagramNode<DrawableProcessNode>> nodes, double x) {
      setLabel("maxX");
      mMaxX=x;
      reportMinMax(nodes);
    }

    @Override
    public void reportMinY(List<? extends DiagramNode<DrawableProcessNode>> nodes, double y) {
      setLabel("minY");
      mMinY = y;
      reportMinMax(nodes);
    }

    @Override
    public void reportMaxY(List<? extends DiagramNode<DrawableProcessNode>> nodes, double y) {
      setLabel("maxY");
      mMaxY = y;
      reportMinMax(nodes);
    }

    private void reportMinMax(List<? extends DiagramNode<DrawableProcessNode>> nodes) {
      for(DiagramNode<DrawableProcessNode> node: nodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()|STATE_GROUP);
      }
      if (mMinMaxOverlay==null) {
        mMinMaxOverlay = new LineDrawable(mMinX, mMinY, mMaxX, mMaxY, getLinePen().getPaint());
      } else {
        mMinMaxOverlay.mX1 = mMinX;
        mMinMaxOverlay.mY1 = mMinY;
        mMinMaxOverlay.mX2 = mMaxX;
        mMinMaxOverlay.mY2 = mMaxY;
      }
      waitForNextClicked(mMinMaxOverlay);
      for(DiagramNode<DrawableProcessNode> node: nodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~STATE_GROUP);
      }
    }

    private void reportXMost(List<? extends DiagramNode<DrawableProcessNode>> nodes, DiagramNode<DrawableProcessNode> activeNode) {
      for(DiagramNode<DrawableProcessNode> node: nodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setX(node.getX());
        target.setY(node.getY());

        if (node!=activeNode) {
          target.setState(target.getState()|STATE_GROUP);
        }
      }
      if (activeNode!=null) {
        final DrawableProcessNode target = activeNode.getTarget();
        target.setState(target.getState()|STATE_XMOST);
      }
      updateDiagramBounds();
      waitForNextClicked(mMinMaxOverlay);
      for(DiagramNode<DrawableProcessNode> node: nodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~(STATE_XMOST|STATE_GROUP));
      }
    }

    @Override
    public void reportMove(DiagramNode<DrawableProcessNode> node, double newX, double newY) {
      setLabel("move");
      final DrawableProcessNode target = node.getTarget();
      target.setState(target.getState()|STATE_MOVED);
      target.setX(newX);
      target.setY(newY);
      updateDiagramBounds();

      waitForNextClicked(moveDrawable(mMinMaxOverlay, Arrays.asList(node)));

      target.setState(target.getState()& ~STATE_MOVED);
      mMinX = Double.NaN;
      mMinY = Double.NaN;
      mMaxX = Double.NaN;
      mMaxY = Double.NaN;
      mMinMaxOverlay = null;
    }

    @NotNull
    private MoveDrawable moveDrawable(@Nullable Drawable minMaxOverlay, @NotNull List<? extends DiagramNode<?>> nodes) {
      List<float[]> arrows = new ArrayList<>(nodes.size());
      for(DiagramNode<?> node: nodes) {
        if (! (Double.isNaN(node.getX())|| Double.isNaN(node.getY()))) {
          final double scale = diagramView1.getScale();
          arrows.add(new float[]{(float) ((node.getX()+(diagramView1.getOffsetX()*scale))*scale),
                                 (float) ((node.getY()+(diagramView1.getOffsetY()*scale))*scale),
                                 (float) ((node.getTarget().getX()+(diagramView1.getOffsetX()*scale))*scale),
                                 (float) ((node.getTarget().getY()+(diagramView1.getOffsetY()*scale))*scale)});
        }
      }
      return new MoveDrawable(minMaxOverlay, arrows);
    }



    @Override
    public void reportMoveX(List<? extends DiagramNode<DrawableProcessNode>> nodes, double offset) {
      setLabel("moveX");
      for(DiagramNode<DrawableProcessNode> node: nodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setX(node.getX()+offset);
        target.setY(node.getY());
        target.setState(target.getState()|STATE_MOVED);
      }
      updateDiagramBounds();
      waitForNextClicked(moveDrawable(mMinMaxOverlay, nodes));
      for(DiagramNode<DrawableProcessNode> node: nodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~STATE_MOVED);
      }
    }

    @Override
    public void reportMoveY(List<? extends DiagramNode<DrawableProcessNode>> nodes, double offset) {
      setLabel("moveY");
      for(DiagramNode<DrawableProcessNode> node: nodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setX(node.getX());
        target.setY(node.getY()+offset);
        target.setState(target.getState()|STATE_MOVED);
      }
      updateDiagramBounds();
      waitForNextClicked(moveDrawable(mMinMaxOverlay, nodes));
      for(DiagramNode<DrawableProcessNode> node: nodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~STATE_MOVED);
      }
    }



    @Override
    public void reportSiblings(DiagramNode<DrawableProcessNode> currentNode, List<? extends DiagramNode<DrawableProcessNode>> nodes,
                               boolean above) {
      setLabel(above ? "Siblings above" : "Siblings below");
      for(DiagramNode<DrawableProcessNode> node: nodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setX(node.getX());
        target.setY(node.getY());

        if (node!=currentNode) {
          target.setState(target.getState()|STATE_GROUP);
        }
      }
      if (currentNode!=null) {
        final DrawableProcessNode target = currentNode.getTarget();
        target.setState(target.getState()|STATE_ACTIVE);
      }
      updateDiagramBounds();
      waitForNextClicked(mMinMaxOverlay);
      for(DiagramNode<DrawableProcessNode> node: nodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~(STATE_ACTIVE|STATE_GROUP));
      }
    }

    private void waitForNextClicked(Drawable overlay) {
      if (mLayoutTask!=null) {
        if (Thread.currentThread()==Looper.getMainLooper().getThread()) {
          if (BuildConfig.DEBUG) {
            throw new IllegalStateException("Performing layout on UI thread");
          }
        } else {
          final WaitTask task = new WaitTask(mImmediate, overlay);
          mLayoutTask.postProgress(task);
          try {
            task.get();
          } catch (ExecutionException e) { // ignore
          } catch (InterruptedException e) { // ignore
          }
        }
      }
    }

    private AndroidPen getLinePen() {
      if (mXMostPen ==null) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setARGB(255, 255, 0, 0);
        mXMostPen = new AndroidPen(paint);
      }
      return mXMostPen;
    }

  }

  private class LayoutTask extends AsyncTask<DrawableProcessModel, WaitTask, DrawableProcessModel> {

    private final MyStepper mStepper = new MyStepper();
    private WaitTask mTask;
    private DrawableProcessModel mPm;

    LayoutTask(DrawableProcessModel pm) {
      if (pm!=null) {
        mPm = pm;
      } else if (mPm == null || mPm.getModelNodes().isEmpty()) {
        mPm = loadInitialProcessModel(NULL_LAYOUT_ALGORITHM, new LayoutAlgorithm<DrawableProcessNode>());
      }
    }

    @Override
    protected void onPreExecute() {
      if (mPm !=null) {
        LayoutAlgorithm<DrawableProcessNode> alg = new LayoutAlgorithm<>();
        alg.setGridSize(diagramView1.getGridSize());
        alg.setTighten(true);
        alg.setLayoutStepper(mStepper);
        mPm.setLayoutAlgorithm(alg);
        mAdapter = new MyDiagramAdapter(PMEditor.this, mPm);
        diagramView1.setAdapter(mAdapter);
//        mPm.layout();
      }
    }

    @Override
    protected DrawableProcessModel doInBackground(DrawableProcessModel... params) {
      // Start with null layout algorithm, to prevent dual layout.
      if (mPm !=null) {
        mPm.layout();
      }
      return mPm;
    }

    public void postProgress(WaitTask waitTask) {
      this.publishProgress(waitTask);
    }

    @Override
    protected void onProgressUpdate(WaitTask... values) {
      final WaitTask task = values[0];
      if (! isCancelled()) {
  //      findViewById(R.id.ac_next).setEnabled(true);
        diagramView1.setOverlay(task.mOverlay);
        diagramView1.invalidate();
      }
      if (task.isImmediate()) {
        task.trigger();
      }
      mTask = task;
    }

    @Override
    protected void onPostExecute(DrawableProcessModel result) {
      mLayoutTask=null;
      updateDiagramBounds();
      if (mStepper.mLayoutNode!=null) {
        final DrawableProcessNode target = mStepper.mLayoutNode.getTarget();
        target.setState(target.getState()& (~(STATE_CUSTOM1|STATE_CUSTOM2|STATE_CUSTOM3|STATE_CUSTOM4)));
        mStepper.mLayoutNode = null;
      }
      diagramView1.setOverlay(null);
      diagramView1.invalidate();
    }

    public void playAll() {
      mStepper.mImmediate= true;
      if (mTask !=null) {
        mTask.trigger();
        mTask = null;
//        findViewById(R.id.ac_next).setEnabled(false);
      }
    }

    public void next() {
      if (mTask !=null) {
        mTask.trigger();
        mTask = null;
//        findViewById(R.id.ac_next).setEnabled(false);
      }
    }

  }

  private class ItemDragListener implements OnDragListener, OnTouchListener {

    @Override
    public boolean onDrag(View v, DragEvent event) {
      int action = event.getAction();
      switch (action) {
        case DragEvent.ACTION_DRAG_STARTED:
          return true;
        case DragEvent.ACTION_DRAG_ENDED:
          return true;
        case DragEvent.ACTION_DRAG_LOCATION:
          return true;
        case DragEvent.ACTION_DRAG_ENTERED:
          return (v==diagramView1);
        case DragEvent.ACTION_DROP: {
          if (v==diagramView1) {
            Class<?> nodeType = (Class<?>) event.getLocalState();
            Constructor<?> constructor;
            try {
              constructor = nodeType.getConstructor();
              DrawableProcessNode node = (DrawableProcessNode) constructor.newInstance();
              float diagramX = diagramView1.toDiagramX(event.getX());
              float diagramY = diagramView1.toDiagramY(event.getY());
              final int gridSize = diagramView1.getGridSize();
              if (gridSize>0) {
                diagramX = Math.round(diagramX/gridSize)*gridSize;
                diagramY = Math.round(diagramY/gridSize)*gridSize;
              }
              node.setX(diagramX);
              node.setY(diagramY);
              mPm.addNode(node);
              diagramView1.invalidate();
              return true;
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
              Log.e(PMEditor.class.getSimpleName(), "Failure to instantiate new node", e);
            }
          }
          break;
        }
      }
      return false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
      // Don't handle multitouch
      if (event.getPointerCount()>1) {
        return false;
      }

      ImageView v = (ImageView) view;
      DrawableDrawable d = (DrawableDrawable) v.getDrawable();
      int action = event.getAction();
      switch (action) {
        case MotionEvent.ACTION_DOWN: {
          // Get the bounds of the drawable
          RectF boundsF = new RectF(d.getBounds());
          // Convert the bounds into imageview relative coordinates
          v.getImageMatrix().mapRect(boundsF);
          // Only work when the image is touched
          if (boundsF.contains(event.getX(), event.getY())) {
            ClipData data = ClipData.newPlainText("label", "text");
            v.startDrag(data , new ItemShadowBuilder(d), v.getTag(), 0);
          }
          break;
        }

      }

      return false;
    }

  }

  private class ItemShadowBuilder extends DragShadowBuilder {

    private DrawableDrawable mDrawable;
    private double mScale;

    public ItemShadowBuilder(DrawableDrawable d) {
      super(elementsView);
      mDrawable = d.clone();
      mDrawable.setState(new int[]{android.R.attr.state_pressed});
    }

    @Override
    public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
      mScale = diagramView1.getScale();
      mDrawable.setScale(mScale);
      shadowSize.x=(int) Math.ceil(mDrawable.getIntrinsicWidth()+2*mScale*AndroidTheme.SHADER_RADIUS);
      shadowSize.y=(int) Math.ceil(mDrawable.getIntrinsicHeight()+2*mScale*AndroidTheme.SHADER_RADIUS);
      shadowTouchPoint.x=shadowSize.x/2;
      shadowTouchPoint.y=shadowSize.y/2;
      final int padding = (int) Math.round(mScale*AndroidTheme.SHADER_RADIUS);
      mDrawable.setBounds(padding, padding, shadowSize.x-padding, shadowSize.y-padding);
    }

    @Override
    public void onDrawShadow(Canvas canvas) {
      final float padding = (float) (mScale*AndroidTheme.SHADER_RADIUS);
      int restore = canvas.save();
      canvas.translate(padding, padding);
      mDrawable.draw(canvas);
      canvas.restoreToCount(restore);
    }

  }

  boolean mStep = true;

  private ItemDragListener mItemDragListener = new ItemDragListener();

  private DiagramView diagramView1;


  private DrawableProcessModel mPm;

  private MyDiagramAdapter mAdapter;

  private LayoutTask mLayoutTask;
  private LinearLayout elementsView;
  private Uri mPmUri;
  private boolean mCancelled = false;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_processmodel_editor);

    diagramView1 = (DiagramView) findViewById(R.id.diagramView1);
    diagramView1.setOffsetX(0d);
    diagramView1.setOffsetY(0d);
    diagramView1.setOnNodeClickListener(this);
    diagramView1.setOnDragListener(mItemDragListener);

    elementsView = (LinearLayout) findViewById(R.id.diagramElementsGroup);
    if (elementsView!=null) {
      elementsView.removeAllViews();

      final AndroidTheme theme = new AndroidTheme(AndroidStrategy.INSTANCE);

      addNodeView(theme, new DrawableStartNode(false));
      addNodeView(theme, new DrawableActivity(false));
      addNodeView(theme, new DrawableSplit());
      addNodeView(theme, new DrawableJoin(false));
      addNodeView(theme, new DrawableEndNode());

      elementsView.requestLayout();
    }

    if(savedInstanceState!=null) {
      final PMParcelable pmparcelable = savedInstanceState.getParcelable(KEY_PROCESSMODEL);
      if (pmparcelable!=null) {
        mPm = DrawableProcessModel.get(pmparcelable.getProcessModel());
      }
      mPmUri = savedInstanceState.getParcelable(KEY_PROCESSMODEL_URI);
    } else {
      mPmUri = getIntent().getData();
      if (mPmUri!=null) {
        final LayoutAlgorithm<DrawableProcessNode> layoutAlgorithm = new LayoutAlgorithm<DrawableProcessNode>();
        mPm = loadProcessModel(mPmUri, layoutAlgorithm, layoutAlgorithm);
      }
    }
    if (mPm == null) {
      mPm = loadInitialProcessModel();
    }
    getSupportFragmentManager().beginTransaction().add(new PMProcessesFragment(), "processModelHelper").commit();
  }

  /**
   * Add a view to the elementsView that shows a single process node.
   * @param theme
   * @param node
   */
  private void addNodeView(AndroidTheme theme, DrawableProcessNode node) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    lp.gravity=Gravity.CENTER;
    lp.weight=1f;
    if (elementsView.getOrientation()==LinearLayout.HORIZONTAL) {
      lp.leftMargin=(int) Math.round(DiagramView.DENSITY*ITEM_MARGIN);
      lp.rightMargin=lp.leftMargin;
    } else {
      lp.topMargin=(int) Math.round(DiagramView.DENSITY*ITEM_MARGIN/*dp*/);
      lp.bottomMargin=lp.topMargin;
    }

    ImageView v = new ImageView(this);
    v.setLayoutParams(lp);
    v.setImageDrawable(new DrawableDrawable(positionNode(node), theme));
    v.setOnTouchListener(mItemDragListener);
    v.setOnDragListener(mItemDragListener);
    v.setTag(node.getClass());
    elementsView.addView(v);
  }

  private static DrawableProcessNode positionNode(final DrawableProcessNode node) {
    node.setX(0); node.setY(0);
    Rectangle bounds = node.getBounds();
    node.setX(-bounds.left); node.setY(-bounds.top);
    return node;
  }

  public void setLabel(final String string) {
    runOnUiThread(new Runnable(){

      @Override
      public void run() {
        getSupportActionBar().setTitle("PMEditor - " +string);
      }

    });

  }

  public void updateDiagramBounds() {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for(DrawableProcessNode node: mPm.getModelNodes()) {
      if (!(Double.isNaN(node.getX()) || Double.isNaN(node.getY()))) {
        final Rectangle bounds = node.getBounds();
        if (bounds.left<minX) { minX = bounds.left; }
        if (bounds.top<minY) { minY = bounds.top; }
        if (bounds.bottom()>maxY) { maxY = bounds.bottom(); }
      }
    }
    double offsetX= Double.isInfinite(minX)? 0 : minX - mPm.getLeftPadding();
    double offsetY= Double.isInfinite(minY)? 0 : minY - mPm.getTopPadding();
    diagramView1.setOffsetX(offsetX/*/diagramView1.getScale()*/);
    diagramView1.setOffsetY(offsetY-(((diagramView1.getHeight()/diagramView1.getScale())-(maxY-minY))/2));
  }

  private DrawableProcessModel loadInitialProcessModel() {
    final LayoutAlgorithm<DrawableProcessNode> layout = new LayoutAlgorithm<>();
    return loadInitialProcessModel(layout, layout);
  }

  private DrawableProcessModel loadInitialProcessModel(LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) {
    return loadProcessModel(getResources().openRawResource(R.raw.processmodel), simpleLayoutAlgorithm, advancedAlgorithm);
  }

  private DrawableProcessModel loadProcessModel(Uri uri, LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) {
    try {
      return loadProcessModel(getContentResolver().openInputStream(uri), simpleLayoutAlgorithm, advancedAlgorithm);
    } catch (FileNotFoundException e1) {
      throw new RuntimeException(e1);
    }
  }

  public DrawableProcessModel loadProcessModel(InputStream in, LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) {
    try {
      return PMParser.parseProcessModel(in, simpleLayoutAlgorithm, advancedAlgorithm);
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        Log.e(PMEditor.class.getName(), e.getMessage(), e);
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    mAdapter = new MyDiagramAdapter(this, mPm);
    diagramView1.setAdapter(mAdapter);
  }



  @Override
  protected void onDestroy() {
    if (mLayoutTask!=null) {
      mLayoutTask.playAll();
      mLayoutTask.cancel(false);
    }
    super.onDestroy();
  }



  @Override
  public void finish() {
    updateActivityResult();
    super.finish();
  }

  private void updateActivityResult() {
    if (mCancelled ) {
      setResult(RESULT_CANCELED);
    } else {
      Intent data = new Intent();
      if (mPmUri!=null) {
        data.setData(mPmUri);
        if ("content".equals(mPmUri.getScheme())||"file".equals(mPmUri.getScheme())) {
          OutputStream out;
          try {
            out = getContentResolver().openOutputStream(mPmUri);
            try {
              PMParser.serializeProcessModel(out, mPm);
            } finally {
              out.close();
            }
          } catch (XmlPullParserException|IOException e) {
            Log.e(TAG, "Failure to save process model on closure", e);
          }
        }
      }
      if (mPm!=null) {
        PMParcelable parcelable = new PMParcelable(mPm);
        data.putExtra(EXTRA_PROCESS_MODEL, parcelable);
      }
      setResult(RESULT_OK, data);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.ac_cancel:
        mCancelled = true;
        finish();
        break;
      case R.id.ac_next:
        if (mLayoutTask!=null) {
          mLayoutTask.next();
        } else {
          mLayoutTask = new LayoutTask(mPm);
          mLayoutTask.execute();
        }
        break;
      case R.id.ac_reset:
        if (mLayoutTask!=null) {
          mLayoutTask.cancel(false);
          mLayoutTask.playAll();
        }
        mPm = null; // unset the process model
        mLayoutTask = new LayoutTask(mPm);
        mLayoutTask.execute();
        break;
      case R.id.ac_play:
        if (mLayoutTask!=null) {
          mLayoutTask.playAll();
        }
        break;
      case android.R.id.home:
        updateActivityResult();
      //$FALL-THROUGH$ we just add behaviour but then go to the parent.
    default:
        return super.onOptionsItemSelected(item);
    }
    return true;
  }



  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  @Override
  public Intent getParentActivityIntent() {
    Intent intent = super.getParentActivityIntent();
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    return intent;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode==Activity.RESULT_OK) {
      // no results yet
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.diagram_menu, menu);
    return true;
  }

  @Override
  public boolean onNodeClicked(DiagramView view, int position, MotionEvent event) {
    int oldSelection = view.getSelection();
    if (oldSelection==position) {
      view.setSelection(-1);
    } else {
      view.setSelection(position);
    }
    return true;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(KEY_PROCESSMODEL, new PMParcelable(mPm));
    if (mPmUri!=null) {
      outState.putParcelable(KEY_PROCESSMODEL_URI, mPmUri);
    }
  }

  @Override
  public DrawableProcessNode getNode(int pos) {
    return mAdapter.getItem(pos);
  }

  @Override
  public void onNodeEdit(int pos) {
    diagramView1.invalidate();
  }

  @Override
  public ClientProcessModel<?> getProcessModel() {
    return mPm;
  }



}
