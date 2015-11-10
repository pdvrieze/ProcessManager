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

  public TaskItemAdapter(UserTask data) {
    mData = data;
  }

  @Override
  public int getCount() {
    return mData.getItems().size();
  }

  @Override
  public TaskItem getItem(int position) {
    return mData.getItems().get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).getType().ordinal();
  }

  @Override
  public int getViewTypeCount() {
    return TaskItem.Type.values().length;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (mInflater==null) { mInflater = LayoutInflater.from(parent.getContext()); }
    TaskItem item = getItem(position);
    View v = convertView;
    if (v==null) {
      return item.createView(mInflater, parent);
    } else {
      item.updateView(v);
      return v;
    }
  }

}
