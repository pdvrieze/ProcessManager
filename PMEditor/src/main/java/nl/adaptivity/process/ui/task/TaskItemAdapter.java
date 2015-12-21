package nl.adaptivity.process.ui.task;

import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.Observable.OnPropertyChangedCallback;
import android.databinding.ObservableList;
import android.databinding.ObservableList.OnListChangedCallback;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import nl.adaptivity.process.editor.android.BR;
import nl.adaptivity.process.tasks.TaskItem;
import nl.adaptivity.process.tasks.TaskItem.Type;
import nl.adaptivity.process.tasks.UserTask;


/**
 * A {@link android.support.v7.widget.RecyclerView.Adapter RecyclerView.Adapter} for {@link nl.adaptivity.process.tasks.TaskItem TaskItems}.
 * Created by pdvrieze on 21/12/15.
 */
public class TaskItemAdapter extends Adapter<TaskItemAdapter.TaskItemHolder> {
  public static class TaskItemHolder extends ViewHolder {

    private final ViewDataBinding binding;

    public TaskItemHolder(final ViewDataBinding itemViewBinding) {
      super(itemViewBinding.getRoot());
      binding = itemViewBinding;
    }

  }

  private final OnListChangedCallback<? extends ObservableList<TaskItem>> mListChangeCallback = new OnListChangedCallback<ObservableList<TaskItem>>() {
    @Override
    public void onChanged(final ObservableList<TaskItem> sender) {
      notifyDataSetChanged();
    }

    @Override
    public void onItemRangeChanged(final ObservableList<TaskItem> sender, final int positionStart, final int itemCount) {
      notifyItemRangeChanged(positionStart, itemCount);
    }

    @Override
    public void onItemRangeInserted(final ObservableList<TaskItem> sender, final int positionStart, final int itemCount) {
      notifyItemRangeInserted(positionStart, itemCount);
    }

    @Override
    public void onItemRangeMoved(final ObservableList<TaskItem> sender, final int fromPosition, final int toPosition, final int itemCount) {
      if (fromPosition<toPosition) {
        for (int i = itemCount-1; i >=0 ; i--) {
          notifyItemMoved(fromPosition+i, toPosition+i);
        }
      } else {
        for (int i = 0; i <itemCount ; i++) {
          notifyItemMoved(fromPosition+i, toPosition+i);
        }
      }

    }

    @Override
    public void onItemRangeRemoved(final ObservableList<TaskItem> sender, final int positionStart, final int itemCount) {
      notifyItemRangeRemoved(positionStart, itemCount);
    }
  };

  private UserTask mUserTask;
  private LayoutInflater mInflater;
  private OnPropertyChangedCallback mOnPropertyChangedCallback = new OnPropertyChangedCallback() {
    @Override
    public void onPropertyChanged(final Observable sender, final int propertyId) {
      if (propertyId == BR.editable) {
        notifyItemRangeChanged(0, getItemCount()); // All items changed
      }
    }
  };

  public TaskItemAdapter() {
    // do nothing
  }

  public TaskItemAdapter(UserTask userTask) {
    mUserTask = userTask;
    if (userTask!=null) {
      userTask.getItems().addOnListChangedCallback(mListChangeCallback);
    }
  }

  public void setUserTask(UserTask userTask) {
    if (mUserTask==userTask) { return; }
    if (mUserTask!=null) {
      mUserTask.getItems().removeOnListChangedCallback(mListChangeCallback);
      mUserTask.removeOnPropertyChangedCallback(mOnPropertyChangedCallback);
    }
    mUserTask = userTask;
    if (userTask!=null) {
      userTask.getItems().addOnListChangedCallback(mListChangeCallback);
      userTask.addOnPropertyChangedCallback(mOnPropertyChangedCallback);
    }
  }

  @Override
  public int getItemViewType(final int position) {
    TaskItem item = mUserTask.getItems().get(position);
    return getTypeOrd(item.getType());
  }

  private static int getTypeOrd(final Type item) {
    return item.ordinal();
  }

  private static Type getType(final int ord) {
    return Type.values()[ord];
  }

  @Override
  public TaskItemHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    if (mInflater==null) { mInflater = LayoutInflater.from(parent.getContext()); }
    Type type = getType(viewType);
    ViewDataBinding binding = DataBindingUtil.inflate(mInflater, type.layoutId, parent, false);

    return new TaskItemHolder(binding);
  }

  @Override
  public void onBindViewHolder(final TaskItemHolder holder, final int position) {
    TaskItem item = mUserTask.getItems().get(position);
    item.updateView(holder.binding);
    holder.binding.setVariable(BR.editable, mUserTask.isEditable());
    holder.binding.executePendingBindings();
  }

  @Override
  public int getItemCount() {
    return mUserTask==null ? 0 : mUserTask.getItems().size();
  }
}
