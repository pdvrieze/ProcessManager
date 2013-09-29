package nl.adaptivity.process.editor.android;

import java.io.IOException;
import java.io.InputStream;
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


  private static class WaitTask extends FutureTask<Object> {

    private static final Callable<Object> NULLCALLABLE = new Callable<Object>() {

      @Override
      public Object call() throws Exception {
        throw new UnsupportedOperationException("Should not be set");
      }

    };

    public WaitTask() {
      super(NULLCALLABLE);
    }

    public void trigger() {
      set(Boolean.TRUE);
    }
  }

  private class MyStepper extends LayoutStepper<DrawableProcessNode> {

    LayoutTask aLayoutTask;
    private Pen aGreenPen;
    private boolean dontwait = false;

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
      if (! dontwait) {
        final WaitTask task = new WaitTask();
        aLayoutTask.postProgress(task);
        try {
          task.get();
        } catch (ExecutionException e) {

        } catch (InterruptedException e) {
        }
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
      aStepper = new MyStepper(this);
      aPm.getLayoutAlgorithm().setLayoutStepper(aStepper);
      aPm.layout();
      return null;
    }

    public void postProgress(WaitTask pWaitTask) {
      this.publishProgress(pWaitTask);
    }

    @Override
    protected void onProgressUpdate(WaitTask... pValues) {
//      findViewById(R.id.ac_next).setEnabled(true);
      diagramView1.invalidate();
      aTask = pValues[0];
    }

    @Override
    protected void onPostExecute(Object pResult) {
      aLayoutTask=null;
      diagramView1.invalidate();
    }

    public void playAll() {
      aStepper.dontwait= true;
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
    final InputStream file = getResources().openRawResource(R.raw.processmodel);
    try {
      aPm = PMParser.parseProcessModel(file);
      if (aPm!=null) {
        aPm.setScale(2.5d);
        aPm.invalidate();
        diagramView1.setDiagram(aPm);
      }
    } finally {
      try {
        file.close();
      } catch (IOException e) {
        Log.e(getClass().getName(), e.getMessage(), e);
        return;
      }
    }
  }

  public void updateDiagramBounds() {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    for(DrawableProcessNode node: aPm.getModelNodes()) {
      final Rectangle bounds = node.getBounds();
      if (bounds.left<minX) { minX = bounds.left; }
      if (bounds.top<minY) { minY = bounds.top; }
    }
    double offsetX= aPm.getLeftPadding()-minX;
    double offsetY= aPm.getTopPadding()-minY;
    for(DrawableProcessNode node: aPm.getModelNodes()) {
      if (!(Double.isNaN(node.getX()) || Double.isNaN(node.getY()))) {
        node.setX(node.getX()+offsetX);
        node.setY(node.getY()+offsetY);
      }
    }
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
        break;
      case R.id.ac_zoom_dec:
        aPm.setScale(aPm.getScale()/1.2);
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
