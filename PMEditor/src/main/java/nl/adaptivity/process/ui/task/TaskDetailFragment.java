/*
 * Copyright (c) 2016.
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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.ui.task;

import android.content.ContentUris;
import android.content.Context;
import android.content.OperationApplicationException;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import nl.adaptivity.android.util.MasterDetailOuterFragment;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.FragmentTaskDetailBinding;
import nl.adaptivity.process.tasks.ExecutableUserTask;
import nl.adaptivity.process.tasks.ExecutableUserTask.TaskState;
import nl.adaptivity.process.tasks.data.TaskLoader;
import nl.adaptivity.process.tasks.data.TaskProvider;

import java.util.NoSuchElementException;

/**
 * A fragment representing a single ProcessModel detail screen. This fragment is
 * either contained in a {@link TaskListOuterFragment} in two-pane mode (on
 * tablets) or a {@link TaskDetailActivity} on handsets.
 */
public class TaskDetailFragment extends Fragment implements LoaderCallbacks<ExecutableUserTask>, TaskDetailHandler {

  public interface TaskDetailCallbacks {
    void dismissTaskDetails();
  }

  private static final int LOADER_TASKITEM = 0;


  private long mTaskId;

  private TaskDetailCallbacks mCallbacks;
  private FragmentTaskDetailBinding mBinding;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public TaskDetailFragment() {}

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
//    setHasOptionsMenu(true);
  }

  @Override
  public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (getArguments().containsKey(MasterDetailOuterFragment.ARG_ITEM_ID)) {
      getLoaderManager().initLoader(LOADER_TASKITEM, getArguments(), this);
    }
  }

  @Override
  public void onAttach(final Context context) {
    super.onAttach(context);
    if (context instanceof TaskDetailCallbacks) {
      mCallbacks = (TaskDetailCallbacks) context;
    } else {
      mCallbacks = null;
    }
  }

  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                           final Bundle savedInstanceState) {
    mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_task_detail, container, false);
    mBinding.setLoading(true);
    mBinding.setHandler(this);
    mBinding.taskDetail.setLayoutManager(new LinearLayoutManager(getContext()));
    return mBinding.getRoot();
  }

  @BindingAdapter("usertask")
  public static void bindTaskItemAdapter(final RecyclerView view, final ExecutableUserTask task) {
    if (view.getAdapter() instanceof TaskItemAdapter) {
      final TaskItemAdapter adapter = (TaskItemAdapter) view.getAdapter();
      adapter.setUserTask(task);
    } else {
      view.setAdapter(new TaskItemAdapter(task));
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    final ExecutableUserTask task = mBinding.getTask();
    if (task != null && task.isDirty()) {
      try {
        TaskProvider.updateValuesAndState(getActivity(), mTaskId, task);
      } catch (NoSuchElementException e) {
        Log.w(TaskDetailFragment.class.getSimpleName(), "The task no longer exists", e);
      } catch (RemoteException | OperationApplicationException e) {
        Log.w(TaskDetailFragment.class.getSimpleName(), "Failure to update the task state", e);
        Toast.makeText(getActivity(), "The task could not be stored", Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public Loader<ExecutableUserTask> onCreateLoader(final int id, final Bundle args) {
    mTaskId = args.getLong(MasterDetailOuterFragment.ARG_ITEM_ID);
    final Uri uri = ContentUris.withAppendedId(TaskProvider.Tasks.CONTENT_ID_URI_BASE, mTaskId);
    return new TaskLoader(getActivity(), uri);
  }

  @Override
  public void onLoadFinished(final Loader<ExecutableUserTask> loader, final ExecutableUserTask data) {
    mBinding.setLoading(false);
    if (data==null) { onLoaderReset(loader); return;}
    mBinding.setTask(data);
  }

  @Override
  public void onLoaderReset(final Loader<ExecutableUserTask> loader) {
    mBinding.setTask(null);
  }

  @Override
  public void onAcceptClick(final View v) {
    final ExecutableUserTask task         = mBinding.getTask();
    final boolean            initialDirty = task.isDirty();
    task.setState(TaskState.Taken);
    if (initialDirty) {
      try {
        TaskProvider.updateValuesAndState(getActivity(), mTaskId, task);
      } catch (RemoteException | OperationApplicationException e) {
        throw new RuntimeException(e);
      }
    } else if (task.isDirty()) {
      TaskProvider.updateTaskState(getActivity(), mTaskId, task.getState());
    }
  }

  @Override
  public void onCancelClick(final View v) {
    mBinding.getTask().setState(TaskState.Complete);
    if (mCallbacks!=null) {
      mCallbacks.dismissTaskDetails(); // should trigger save
    }
  }

  @Override
  public void onCompleteClick(final View v) {
    mBinding.getTask().setState(TaskState.Complete);
    if (mCallbacks!=null) {
      mCallbacks.dismissTaskDetails(); // should trigger save
    }
  }

}
