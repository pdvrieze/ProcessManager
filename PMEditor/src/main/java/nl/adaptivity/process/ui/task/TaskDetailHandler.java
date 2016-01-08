package nl.adaptivity.process.ui.task;

import android.view.View;


/**
 * Created by pdvrieze on 21/12/15.
 */
public interface TaskDetailHandler {
  void onAcceptClick(View v);
  void onCancelClick(View v);
  void onCompleteClick(View v);
}
