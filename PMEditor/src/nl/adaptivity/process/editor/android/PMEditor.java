package nl.adaptivity.process.editor.android;

import static nl.adaptivity.diagram.Drawable.STATE_CUSTOM1;
import static nl.adaptivity.diagram.Drawable.STATE_CUSTOM2;
import static nl.adaptivity.diagram.Drawable.STATE_CUSTOM3;
import static nl.adaptivity.diagram.Drawable.STATE_CUSTOM4;

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

import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.android.AndroidPen;
import nl.adaptivity.diagram.android.AndroidStrategy;
import nl.adaptivity.diagram.android.AndroidTheme;
import nl.adaptivity.diagram.android.DiagramView;
import nl.adaptivity.diagram.android.DiagramView.DiagramDrawable;
import nl.adaptivity.diagram.android.DiagramView.OnNodeClickListener;
import nl.adaptivity.diagram.android.DrawableDrawable;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.diagram.*;
import nl.adaptivity.process.editor.android.NodeEditDialogFragment.NodeEditListener;

import org.xmlpull.v1.XmlPullParserException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.*;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class PMEditor extends Activity implements OnNodeClickListener, NodeEditListener {


  private static final String KEY_PROCESSMODEL = "processmodel";
  private static final int ITEM_MARGIN = 8/*dp*/;
  private static final int STATE_ACTIVE=STATE_CUSTOM1;
  private static final int STATE_GROUP=STATE_CUSTOM2;
  private static final int STATE_XMOST=STATE_CUSTOM3;
  private static final int STATE_MOVED=STATE_CUSTOM4;



  private final class LineDrawable extends DiagramDrawable {

    private double aX1;
    private double aY1;
    private double aX2;
    private double aY2;
    private final Paint aPaint;

    public LineDrawable(double pX1, double pY1, double pX2, double pY2, Paint pPaint) {
      aX1 = pX1;
      aX2 = pX2;
      aY1 = pY1;
      aY2 = pY2;
      aPaint = pPaint;
    }

    @Override
    public void draw(Canvas pCanvas, double pScale) {
      int height = (int) Math.round((pCanvas.getHeight()/pScale));
      int width = (int) Math.round(pCanvas.getWidth()/pScale);
      if (!Double.isNaN(aX1)) {
        float scaledX = (float) (aX1-diagramView1.getOffsetX());
        pCanvas.drawLine(scaledX, 0, scaledX, height, aPaint);
      }
      if (!Double.isNaN(aY1)) {
        float scaledY = (float) (aY1-diagramView1.getOffsetY());
        pCanvas.drawLine(0, scaledY, width, scaledY, aPaint);
      }
      if (!Double.isNaN(aX2)) {
        float scaledX = (float) (aX2-diagramView1.getOffsetX());
        pCanvas.drawLine(scaledX, 0, scaledX, height, aPaint);
      }
      if (!Double.isNaN(aY2)) {
        float scaledY = (float) (aY2-diagramView1.getOffsetY());
        pCanvas.drawLine(0, scaledY, width, scaledY, aPaint);
      }
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSPARENT;
    }

    @Override
    public void setAlpha(int pAlpha) {
      //
    }

    @Override
    public void setColorFilter(ColorFilter pCf) {
      //
    }

  }

  final static LayoutAlgorithm<DrawableProcessNode> NULL_LAYOUT_ALGORITHM = new LayoutAlgorithm<DrawableProcessNode>(){

    @Override
    public boolean layout(List<? extends DiagramNode<DrawableProcessNode>> pNodes) {
      return false; // don't do any layout
    }

  };
  private static final int REQUEST_SAVE_FILE = 42;
  private static final String TAG = PMEditor.class.getName();

  private static class MoveDrawable extends DiagramDrawable{

    private int aAlpha = 255;
    private List<float[]> arrows;
    private Paint aPaint;
    private Drawable aMinMaxOverlay;

    public MoveDrawable(Drawable pMinMaxOverlay, List<float[]> pArrows) {
      aMinMaxOverlay = pMinMaxOverlay;
      arrows = pArrows;
    }

    @Override
    public void draw(Canvas pCanvas, double pScale) {
      if (aMinMaxOverlay!=null) {
        if (aMinMaxOverlay instanceof DiagramDrawable) {
          ((DiagramDrawable)aMinMaxOverlay).draw(pCanvas, pScale);
        } else {
          aMinMaxOverlay.draw(pCanvas);
        }
      }
      if (aPaint ==null) {
        aPaint = new Paint();
        aPaint.setAntiAlias(true);
        aPaint.setAlpha(aAlpha);
        aPaint.setStyle(Style.STROKE);
        aPaint.setStrokeWidth(3);
        aPaint.setARGB(255, 0, 255, 0);
      }
      for(float[] arrow:arrows) {
        pCanvas.drawLine(arrow[0], arrow[1], arrow[2], arrow[3], aPaint);
      }
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSPARENT;
    }

    @Override
    public void setAlpha(int pAlpha) {
      aAlpha = pAlpha;
      if (aPaint!=null) {
        aPaint.setAlpha(pAlpha);
      }
    }

    @Override
    public void setColorFilter(ColorFilter pCf) {
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

    private final boolean aImmediate;

    private Drawable aOverlay;

    public WaitTask(boolean pImmediate, Drawable pOverlay) {
      super(NULLCALLABLE);
      aImmediate = pImmediate;
      aOverlay = pOverlay;
    }

    public void trigger() {
      set(Boolean.TRUE);
    }

    public boolean isImmediate() {
      return aImmediate;
    }
  }

  private class MyStepper extends LayoutStepper<DrawableProcessNode> {

    private boolean aImmediate = false;
    private AndroidPen aXMostPen;
    private DiagramNode<DrawableProcessNode> aLayoutNode;
    private double aMinX = Double.NaN;
    private double aMinY = Double.NaN;
    private double aMaxX = Double.NaN;
    private double aMaxY = Double.NaN;
    private LineDrawable aMinMaxOverlay = null;

    @Override
    public void reportLayoutNode(DiagramNode<DrawableProcessNode> pNode) {
      aMinX = Double.NaN;
      aMinY = Double.NaN;
      aMaxX = Double.NaN;
      aMaxY = Double.NaN;
      aMinMaxOverlay = null;
      setLabel("node under consideration");
      if (! (Double.isNaN(pNode.getX()) || Double.isNaN(pNode.getY()))) {
        if (aLayoutNode!=null) {
          final DrawableProcessNode target = aLayoutNode.getTarget();
          target.setState(target.getState()& ~STATE_ACTIVE);
        }
        aLayoutNode = pNode;
        {
          final DrawableProcessNode target = aLayoutNode.getTarget();
          target.setState(target.getState()|STATE_ACTIVE);
        }
        waitForNextClicked(null);
      }
    }

    @Override
    public void reportLowest(List<? extends DiagramNode<DrawableProcessNode>> pNodes, DiagramNode<DrawableProcessNode> pNode) {
      setLabel("lowest");
      reportXMost(pNodes, pNode);
    }

    @Override
    public void reportHighest(List<? extends DiagramNode<DrawableProcessNode>> pNodes, DiagramNode<DrawableProcessNode> pNode) {
      setLabel("highest");
      reportXMost(pNodes, pNode);
    }

    @Override
    public void reportRightmost(List<? extends DiagramNode<DrawableProcessNode>> pNodes, DiagramNode<DrawableProcessNode> pNode) {
      setLabel("rightmost");
      reportXMost(pNodes, pNode);
    }

    @Override
    public void reportLeftmost(List<? extends DiagramNode<DrawableProcessNode>> pNodes, DiagramNode<DrawableProcessNode> pNode) {
      setLabel("leftmost");
      reportXMost(pNodes, pNode);
    }

    @Override
    public void reportMinX(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pX) {
      setLabel("minX");
      aMinX = pX;
      reportMinMax(pNodes);
    }

    @Override
    public void reportMaxX(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pX) {
      setLabel("maxX");
      aMaxX=pX;
      reportMinMax(pNodes);
    }

    @Override
    public void reportMinY(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pY) {
      setLabel("minY");
      aMinY = pY;
      reportMinMax(pNodes);
    }

    @Override
    public void reportMaxY(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pY) {
      setLabel("maxY");
      aMaxY = pY;
      reportMinMax(pNodes);
    }

    private void reportMinMax(List<? extends DiagramNode<DrawableProcessNode>> pNodes) {
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()|STATE_GROUP);
      }
      if (aMinMaxOverlay==null) {
        aMinMaxOverlay = new LineDrawable(aMinX, aMinY, aMaxX, aMaxY, getLinePen().getPaint());
      } else {
        aMinMaxOverlay.aX1 = aMinX;
        aMinMaxOverlay.aY1 = aMinY;
        aMinMaxOverlay.aX2 = aMaxX;
        aMinMaxOverlay.aY2 = aMaxY;
      }
      waitForNextClicked(aMinMaxOverlay);
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~STATE_GROUP);
      }
    }

    private void reportXMost(List<? extends DiagramNode<DrawableProcessNode>> pNodes, DiagramNode<DrawableProcessNode> pNode) {
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setX(node.getX());
        target.setY(node.getY());

        if (node!=pNode) {
          target.setState(target.getState()|STATE_GROUP);
        }
      }
      if (pNode!=null) {
        final DrawableProcessNode target = pNode.getTarget();
        target.setState(target.getState()|STATE_XMOST);
      }
      updateDiagramBounds();
      waitForNextClicked(aMinMaxOverlay);
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~(STATE_XMOST|STATE_GROUP));
      }
    }

    @Override
    public void reportMove(DiagramNode<DrawableProcessNode> pNode, double pNewX, double pNewY) {
      setLabel("move");
      final DrawableProcessNode target = pNode.getTarget();
      target.setState(target.getState()|STATE_MOVED);
      target.setX(pNewX);
      target.setY(pNewY);
      updateDiagramBounds();

      waitForNextClicked(moveDrawable(aMinMaxOverlay, Arrays.asList(pNode)));

      target.setState(target.getState()& ~STATE_MOVED);
      aMinX = Double.NaN;
      aMinY = Double.NaN;
      aMaxX = Double.NaN;
      aMaxY = Double.NaN;
      aMinMaxOverlay = null;
    }

    private MoveDrawable moveDrawable(Drawable pMinMaxOverlay, List<? extends DiagramNode<?>> pNodes) {
      List<float[]> arrows = new ArrayList<>(pNodes.size());
      for(DiagramNode<?> pNode: pNodes) {
        if (! (Double.isNaN(pNode.getX())|| Double.isNaN(pNode.getY()))) {
          final double scale = diagramView1.getScale();
          arrows.add(new float[]{(float) ((pNode.getX()+(diagramView1.getOffsetX()*scale))*scale),
                                 (float) ((pNode.getY()+(diagramView1.getOffsetY()*scale))*scale),
                                 (float) ((pNode.getTarget().getX()+(diagramView1.getOffsetX()*scale))*scale),
                                 (float) ((pNode.getTarget().getY()+(diagramView1.getOffsetY()*scale))*scale)});
        }
      }
      return new MoveDrawable(pMinMaxOverlay, arrows);
    }



    @Override
    public void reportMoveX(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pOffset) {
      setLabel("moveX");
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setX(node.getX()+pOffset);
        target.setY(node.getY());
        target.setState(target.getState()|STATE_MOVED);
      }
      updateDiagramBounds();
      waitForNextClicked(moveDrawable(aMinMaxOverlay, pNodes));
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~STATE_MOVED);
      }
    }

    @Override
    public void reportMoveY(List<? extends DiagramNode<DrawableProcessNode>> pNodes, double pOffset) {
      setLabel("moveY");
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setX(node.getX());
        target.setY(node.getY()+pOffset);
        target.setState(target.getState()|STATE_MOVED);
      }
      updateDiagramBounds();
      waitForNextClicked(moveDrawable(aMinMaxOverlay, pNodes));
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~STATE_MOVED);
      }
    }



    @Override
    public void reportSiblings(DiagramNode<DrawableProcessNode> pNode, List<? extends DiagramNode<DrawableProcessNode>> pNodes,
                               boolean pAbove) {
      setLabel(pAbove ? "Siblings above" : "Siblings below");
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setX(node.getX());
        target.setY(node.getY());

        if (node!=pNode) {
          target.setState(target.getState()|STATE_GROUP);
        }
      }
      if (pNode!=null) {
        final DrawableProcessNode target = pNode.getTarget();
        target.setState(target.getState()|STATE_ACTIVE);
      }
      updateDiagramBounds();
      waitForNextClicked(aMinMaxOverlay);
      for(DiagramNode<DrawableProcessNode> node: pNodes) {
        final DrawableProcessNode target = node.getTarget();
        target.setState(target.getState()& ~(STATE_ACTIVE|STATE_GROUP));
      }
    }

    private void waitForNextClicked(Drawable pOverlay) {
      if (aLayoutTask!=null) {
        if (Thread.currentThread()==Looper.getMainLooper().getThread()) {
          if (BuildConfig.DEBUG) {
            throw new IllegalStateException("Performing layout on UI thread");
          }
        } else {
          final WaitTask task = new WaitTask(aImmediate, pOverlay);
          aLayoutTask.postProgress(task);
          try {
            task.get();
          } catch (ExecutionException e) { // ignore
          } catch (InterruptedException e) { // ignore
          }
        }
      }
    }

    private AndroidPen getLinePen() {
      if (aXMostPen ==null) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setARGB(255, 255, 0, 0);
        aXMostPen = new AndroidPen(paint);
      }
      return aXMostPen;
    }

  }

  private class LayoutTask extends AsyncTask<DrawableProcessModel, WaitTask, Object> {

    private final MyStepper aStepper = new MyStepper();
    private WaitTask aTask;

    @Override
    protected Object doInBackground(DrawableProcessModel... pParams) {
      // Start with null layout algorithm, to prevent dual layout.
      if (pParams.length>0) {
        aPm = pParams[0];
      } else if (aPm == null || aPm.getModelNodes().isEmpty()) {
        aPm = getProcessModel(NULL_LAYOUT_ALGORITHM);
      }
      if (aPm!=null) {
        LayoutAlgorithm<DrawableProcessNode> alg = new LayoutAlgorithm<>();
        alg.setGridSize(diagramView1.getGridSize());
        alg.setTighten(true);
        alg.setLayoutStepper(aStepper);
        aPm.setLayoutAlgorithm(alg);
        aAdapter = new MyDiagramAdapter(PMEditor.this, aPm);
        diagramView1.setAdapter(aAdapter);
        aPm.layout();
      }
      return null;
    }

    public void postProgress(WaitTask pWaitTask) {
      this.publishProgress(pWaitTask);
    }

    @Override
    protected void onProgressUpdate(WaitTask... pValues) {
      final WaitTask task = pValues[0];
      if (! isCancelled()) {
  //      findViewById(R.id.ac_next).setEnabled(true);
        diagramView1.setOverlay(task.aOverlay);
        diagramView1.invalidate();
      }
      if (task.isImmediate()) {
        task.trigger();
      }
      aTask = task;
    }

    @Override
    protected void onPostExecute(Object pResult) {
      aLayoutTask=null;
      updateDiagramBounds();
      if (aStepper.aLayoutNode!=null) {
        final DrawableProcessNode target = aStepper.aLayoutNode.getTarget();
        target.setState(target.getState()& (~(STATE_CUSTOM1|STATE_CUSTOM2|STATE_CUSTOM3|STATE_CUSTOM4)));
        aStepper.aLayoutNode = null;
      }
      diagramView1.setOverlay(null);
      diagramView1.invalidate();
    }

    public void playAll() {
      aStepper.aImmediate= true;
      if (aTask!=null) {
        aTask.trigger();
        aTask = null;
//        findViewById(R.id.ac_next).setEnabled(false);
      }
    }

    public void next() {
      if (aTask!=null) {
        aTask.trigger();
        aTask = null;
//        findViewById(R.id.ac_next).setEnabled(false);
      }
    }

  }

  private class ItemDragListener implements OnDragListener, OnTouchListener {

    @Override
    public boolean onDrag(View pV, DragEvent pEvent) {
      int action = pEvent.getAction();
      switch (action) {
        case DragEvent.ACTION_DRAG_STARTED:
          return true;
        case DragEvent.ACTION_DRAG_ENDED:
          return true;
        case DragEvent.ACTION_DRAG_LOCATION:
          return true;
        case DragEvent.ACTION_DRAG_ENTERED:
          return (pV==diagramView1);
        case DragEvent.ACTION_DROP: {
          if (pV==diagramView1) {
            Class<?> nodeType = (Class<?>) pEvent.getLocalState();
            Constructor<?> constructor;
            try {
              constructor = nodeType.getConstructor(ClientProcessModel.class);
              DrawableProcessNode node = (DrawableProcessNode) constructor.newInstance(aPm);
              float diagramX = diagramView1.toDiagramX(pEvent.getX());
              float diagramY = diagramView1.toDiagramY(pEvent.getY());
              final int gridSize = diagramView1.getGridSize();
              if (gridSize>0) {
                diagramX = Math.round(diagramX/gridSize)*gridSize;
                diagramY = Math.round(diagramY/gridSize)*gridSize;
              }
              node.setX(diagramX);
              node.setY(diagramY);
              aPm.addNode(node);
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
    public boolean onTouch(View pV, MotionEvent pEvent) {
      // Don't handle multitouch
      if (pEvent.getPointerCount()>1) {
        return false;
      }

      ImageView v = (ImageView) pV;
      DrawableDrawable d = (DrawableDrawable) v.getDrawable();
      int action = pEvent.getAction();
      switch (action) {
        case MotionEvent.ACTION_DOWN: {
          // Get the bounds of the drawable
          RectF boundsF = new RectF(d.getBounds());
          // Convert the bounds into imageview relative coordinates
          v.getImageMatrix().mapRect(boundsF);
          // Only work when the image is touched
          if (boundsF.contains(pEvent.getX(), pEvent.getY())) {
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

    private DrawableDrawable aDrawable;
    private double aScale;

    public ItemShadowBuilder(DrawableDrawable pD) {
      super(elementsView);
      aDrawable = pD.clone();
      aDrawable.setState(new int[]{android.R.attr.state_pressed});
    }

    @Override
    public void onProvideShadowMetrics(Point pShadowSize, Point pShadowTouchPoint) {
      aScale = diagramView1.getScale();
      aDrawable.setScale(aScale);
      pShadowSize.x=(int) Math.ceil(aDrawable.getIntrinsicWidth()+2*aScale*AndroidTheme.SHADER_RADIUS);
      pShadowSize.y=(int) Math.ceil(aDrawable.getIntrinsicHeight()+2*aScale*AndroidTheme.SHADER_RADIUS);
      pShadowTouchPoint.x=pShadowSize.x/2;
      pShadowTouchPoint.y=pShadowSize.y/2;
      final int padding = (int) Math.round(aScale*AndroidTheme.SHADER_RADIUS);
      aDrawable.setBounds(padding, padding, pShadowSize.x-padding, pShadowSize.y-padding);
    }

    @Override
    public void onDrawShadow(Canvas pCanvas) {
      final float padding = (float) (aScale*AndroidTheme.SHADER_RADIUS);
      int restore = pCanvas.save();
      pCanvas.translate(padding, padding);
      aDrawable.draw(pCanvas);
      pCanvas.restoreToCount(restore);
    }

  }

  boolean aStep = true;

  private ItemDragListener mItemDragListener = new ItemDragListener();

  private DiagramView diagramView1;


  private DrawableProcessModel aPm;

  private MyDiagramAdapter aAdapter;

  private LayoutTask aLayoutTask;
  private LinearLayout elementsView;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    diagramView1 = (DiagramView) findViewById(R.id.diagramView1);
    diagramView1.setOffsetX(0d);
    diagramView1.setOffsetY(0d);
    diagramView1.setOnNodeClickListener(this);
    diagramView1.setOnDragListener(mItemDragListener);

    elementsView = (LinearLayout) findViewById(R.id.diagramElementsGroup);
    if (elementsView!=null) {
      elementsView.removeAllViews();

      final AndroidTheme theme = new AndroidTheme(AndroidStrategy.INSTANCE);

      addNodeView(theme, new DrawableStartNode((ClientProcessModel<DrawableProcessNode>) null));
      addNodeView(theme, new DrawableActivity((ClientProcessModel<DrawableProcessNode>) null));
      addNodeView(theme, new DrawableSplit((ClientProcessModel<DrawableProcessNode>) null));
      addNodeView(theme, new DrawableJoin((ClientProcessModel<DrawableProcessNode>) null));
      addNodeView(theme, new DrawableEndNode((ClientProcessModel<DrawableProcessNode>) null));

      elementsView.requestLayout();
    }

    if(savedInstanceState!=null) {
      final PMParcelable pmparcelable = savedInstanceState.getParcelable(KEY_PROCESSMODEL);
      if (pmparcelable!=null) {
        aPm = pmparcelable.getProcessModel();
      }
    }
    if (aPm == null) {
      aPm = getProcessModel();
    }

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

  public void setLabel(final String pString) {
    runOnUiThread(new Runnable(){

      @Override
      public void run() {
        getActionBar().setTitle("PMEditor - " +pString);
      }

    });

  }

  public void updateDiagramBounds() {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for(DrawableProcessNode node: aPm.getModelNodes()) {
      if (!(Double.isNaN(node.getX()) || Double.isNaN(node.getY()))) {
        final Rectangle bounds = node.getBounds();
        if (bounds.left<minX) { minX = bounds.left; }
        if (bounds.top<minY) { minY = bounds.top; }
        if (bounds.bottom()>maxY) { maxY = bounds.bottom(); }
      }
    }
    double offsetX= Double.isInfinite(minX)? 0 : minX - aPm.getLeftPadding();
    double offsetY= Double.isInfinite(minY)? 0 : minY - aPm.getTopPadding();
    diagramView1.setOffsetX(offsetX/*/diagramView1.getScale()*/);
    diagramView1.setOffsetY(offsetY-(((diagramView1.getHeight()/diagramView1.getScale())-(maxY-minY))/2));
  }

  private DrawableProcessModel getProcessModel() {
    return getProcessModel(new LayoutAlgorithm<DrawableProcessNode>());
  }

  private DrawableProcessModel getProcessModel(LayoutAlgorithm<DrawableProcessNode> layoutAlgorithm) {
    final InputStream file = getResources().openRawResource(R.raw.processmodel);
    try {
      return PMParser.parseProcessModel(file, layoutAlgorithm);
    } finally {
      try {
        file.close();
      } catch (IOException e) {
        Log.e(PMEditor.class.getName(), e.getMessage(), e);
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    aAdapter = new MyDiagramAdapter(this, aPm);
    diagramView1.setAdapter(aAdapter);
  }



  @Override
  protected void onDestroy() {
    if (aLayoutTask!=null) {
      aLayoutTask.playAll();
      aLayoutTask.cancel(false);
    }
    super.onDestroy();
  }

  @Override
  public boolean onMenuItemSelected(int pFeatureId, MenuItem pItem) {
    switch (pItem.getItemId()) {
      case R.id.ac_next:
        if (aLayoutTask!=null) {
          aLayoutTask.next();
        } else {
          aLayoutTask = new LayoutTask();
          aLayoutTask.execute();
        }
        break;
      case R.id.ac_reset:
        if (aLayoutTask!=null) {
          aLayoutTask.cancel(false);
          aLayoutTask.playAll();
        }
        aLayoutTask = new LayoutTask();
        aPm = null; // unset the process model
        aLayoutTask.execute();
        break;
      case R.id.ac_play:
        if (aLayoutTask!=null) {
          aLayoutTask.playAll();
        }
        break;
      case R.id.ac_save:
        doSave();
        break;
      default:
        return false;
    }
    return true;
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  private void doSave() {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT) {
      Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
      i.setType("*/*");
      i.addCategory(Intent.CATEGORY_OPENABLE);
      startActivityForResult(i, REQUEST_SAVE_FILE);
    } else {
      Toast.makeText(this, "Saving not yet supported below kitkat", Toast.LENGTH_LONG).show();
      // TODO alternative way
    }
    // TODO Auto-generated method stub

  }



  @Override
  protected void onActivityResult(int pRequestCode, int pResultCode, Intent pData) {
    if (pRequestCode==REQUEST_SAVE_FILE && pResultCode==Activity.RESULT_OK) {
      Uri uri = pData.getData();
      OutputStream out;
      try {
        out = getContentResolver().openOutputStream(uri);
        try {
          PMParser.serializeProcessModel(out , aPm);
        } finally {
          out.close();
        }
      } catch (XmlPullParserException |RuntimeException| IOException e) {
        Log.e(TAG, "Failure to save file", e);
      }
    }
    // TODO Auto-generated method stub
    super.onActivityResult(pRequestCode, pResultCode, pData);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu pMenu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.diagram_menu, pMenu);
    return true;
  }

  @Override
  public boolean onNodeClicked(DiagramView pView, int position, MotionEvent pEvent) {
    int oldSelection = pView.getSelection();
    if (oldSelection==position) {
      pView.setSelection(-1);
    } else {
      pView.setSelection(position);
    }
    return true;
  }

  @Override
  protected void onSaveInstanceState(Bundle pOutState) {
    super.onSaveInstanceState(pOutState);
    pOutState.putParcelable(KEY_PROCESSMODEL, new PMParcelable(aPm));
  }

  @Override
  public DrawableProcessNode getNode(int pPos) {
    return aAdapter.getItem(pPos);
  }

  @Override
  public void onNodeEdit(int pPos) {
    diagramView1.invalidate();
  }



}
