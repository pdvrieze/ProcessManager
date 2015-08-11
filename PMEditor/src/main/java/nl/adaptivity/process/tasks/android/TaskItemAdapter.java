package nl.adaptivity.process.tasks.android;

import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.UserTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


public class TaskItemAdapter extends BaseAdapter {

  private UserTask mData;
  private LayoutInflater mInflater;

  public TaskItemAdapter(UserTask pData) {
    mData = pData;
  }

  @Override
  public int getCount() {
    return mData.getItems().size();
  }

  @Override
  public TaskItem getItem(int pPosition) {
    return mData.getItems().get(pPosition);
  }

  @Override
  public long getItemId(int pPosition) {
    return pPosition;
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  @Override
  public int getItemViewType(int pPosition) {
    return getItem(pPosition).getType().ordinal();
  }

  @Override
  public int getViewTypeCount() {
    return TaskItem.Type.values().length;
  }

  @Override
  public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
    if (mInflater==null) { mInflater = LayoutInflater.from(pParent.getContext()); }
    TaskItem item = getItem(pPosition);
    View v = pConvertView;
    if (v==null) {
      return item.createView(mInflater, pParent);
    } else {
      item.updateView(v);
      return v;
    }
  }

}
