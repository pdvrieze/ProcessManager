package nl.adaptivity.process.editor.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.android.AndroidPen;
import nl.adaptivity.diagram.android.DiagramView;
import nl.adaptivity.process.diagram.DiagramNode;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.diagram.LayoutStepper;
import android.app.Activity;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


public class PMEditor extends Activity {

  private final static LayoutAlgorithm<DrawableProcessNode> NULL_LAYOUT_ALGORITHM = new LayoutAlgorithm<DrawableProcessNode>(){

    @Override
    public boolean layout(List<? extends DiagramNode<DrawableProcessNode>> pNodes) {
      return false; // don't do any layout
    }

  };

  private static class WaitTask extends FutureTask<Object> {

    private static final Callable<Object> NULLCALLABLE = new Callable<Object>() {

      @Override
      public Object call() throws Exception {
        throw new UnsupportedOperationException("Should not be set");
      }

    };

    private final boolean aImmediate;

    public WaitTask(boolean pImmediate) {
      super(NULLCALLABLE);
      aImmediate = pImmediate;
    }

    public void trigger() {
      set(Boolean.TRUE);
    }

    public boolean isImmediate() {
      return aImmediate;
    }
  }

  private class MyStepper extends LayoutStepper<DrawableProcessNode> {

    LayoutTask aLayoutTask;
    private Pen aGreenPen;
    private boolean aImmediate = false;

    public MyStepper(LayoutTask pLayoutTask) {
      aLayoutTask = pLayoutTask;
    }

    @Override
    public void reportMove(DiagramNode<DrawableProcessNode> pNode, double pNewX, double pNewY) {
      pNode.getTarget().setFGPen(getGreenPen());
      pNode.getTarget().setX(pNewX);
      pNode.getTarget().setY(pNewY);
      updateDiagramBounds();

      waitForNextClicked();

      pNode.getTarget().setFGPen(null); // reset the pen
    }

    private void waitForNextClicked() {
      final WaitTask task = new WaitTask(aImmediate);
      aLayoutTask.postProgress(task);
      try {
        task.get();
      } catch (ExecutionException e) { // ignore
      } catch (InterruptedException e) { // ignore
      }
    }

    private Pen getGreenPen() {
      if (aGreenPen ==null) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setARGB(255, 0, 255, 0);
        aGreenPen = new AndroidPen(paint);
      }
      return aGreenPen;
    }

  }

  private class LayoutTask extends AsyncTask<Object, WaitTask, Object> {

    private MyStepper aStepper;
    private WaitTask aTask;

    @Override
    protected Object doInBackground(Object... pParams) {
      if (aStepper==null) {
        aStepper = new MyStepper(this);
      }
      final InputStream file = getResources().openRawResource(R.raw.processmodel);
      try {
        // Start with null layout algorithm, to prevent dual layout.
        aPm = PMParser.parseProcessModel(file, NULL_LAYOUT_ALGORITHM);
        if (aPm!=null) {
          aPm.setScale(2.5d);
          LayoutAlgorithm<DrawableProcessNode> alg = new LayoutAlgorithm<DrawableProcessNode>();
          alg.setLayoutStepper(aStepper);
          aPm.setLayoutAlgorithm(alg);
          diagramView1.setDiagram(aPm);
          aPm.layout();
        }
      } finally {
        try {
          file.close();
        } catch (IOException e) {
          Log.e(getClass().getName(), e.getMessage(), e);
          return null;
        }
      }
      return null;
    }

    public void postProgress(WaitTask pWaitTask) {
      this.publishProgress(pWaitTask);
    }

    @Override
    protected void onProgressUpdate(WaitTask... pValues) {
//      findViewById(R.id.ac_next).setEnabled(true);
      diagramView1.invalidate();
      final WaitTask task = pValues[0];
      if (task.isImmediate()) {
        task.trigger();
      }
      aTask = pValues[0];
    }

    @Override
    protected void onPostExecute(Object pResult) {
      aLayoutTask=null;
      updateDiagramBounds();
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

  boolean aStep = true;


  private DiagramView diagramView1;


  private DrawableProcessModel aPm;


  private LayoutTask aLayoutTask;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    diagramView1 = (DiagramView) findViewById(R.id.diagramView1);
  }

  public void updateDiagramBounds() {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    for(DrawableProcessNode node: aPm.getModelNodes()) {
      if (!(Double.isNaN(node.getX()) || Double.isNaN(node.getY()))) {
        final Rectangle bounds = node.getBounds();
        if (bounds.left<minX) { minX = bounds.left; }
        if (bounds.top<minY) { minY = bounds.top; }
      }
    }
    double offsetX= Double.isInfinite(minX)? 0 : aPm.getLeftPadding()-minX;
    double offsetY= Double.isInfinite(minY)? 0 : aPm.getTopPadding()-minY;
    diagramView1.setOffsetX(offsetX);
    diagramView1.setOffsetY(offsetY);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (aLayoutTask==null) {
      aLayoutTask = new LayoutTask();
      aLayoutTask.execute();
    }
  }



  @Override
  protected void onDestroy() {
    if (aLayoutTask!=null) {
      aLayoutTask.playAll();
      aLayoutTask = null;
    }
    super.onDestroy();
  }

  @Override
  public boolean onMenuItemSelected(int pFeatureId, MenuItem pItem) {
    switch (pItem.getItemId()) {
      case R.id.ac_next:
        if (aLayoutTask!=null) {
          aLayoutTask.next();
        }
        break;
      case R.id.ac_play:
        if (aLayoutTask!=null) {
          aLayoutTask.playAll();
        }
        break;
      case R.id.ac_zoom_inc:
        aPm.setScale(aPm.getScale()*1.2);
        diagramView1.invalidate();
        break;
      case R.id.ac_zoom_dec:
        aPm.setScale(aPm.getScale()/1.2);
        diagramView1.invalidate();
        break;
      default:
        return false;
    }
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu pMenu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.diagram_menu, pMenu);
    return true;
  }
}
