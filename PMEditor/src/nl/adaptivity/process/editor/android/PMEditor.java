package nl.adaptivity.process.editor.android;

import java.io.IOException;
import java.io.InputStream;

import nl.adaptivity.diagram.android.DiagramView;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.processModel.ProcessModel;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


public class PMEditor extends Activity {

  private DiagramView diagramView1;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    diagramView1 = (DiagramView) findViewById(R.id.diagramView1);
    final InputStream file = getResources().openRawResource(R.raw.processmodel);
    try {
      ProcessModel pm = PMParser.parseProcessModel(file);
      diagramView1.setDiagram(new DrawableProcessModel(pm));
    } finally {
      try {
        file.close();
      } catch (IOException e) {
        Log.e(getClass().getName(), e.getMessage(), e);
        return;
      }
    }
  }
}
