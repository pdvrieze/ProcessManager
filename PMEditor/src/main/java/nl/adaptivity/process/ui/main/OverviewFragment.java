package nl.adaptivity.process.ui.main;

import android.content.Context;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import nl.adaptivity.android.compat.TitleFragment;
import nl.adaptivity.android.recyclerview.ClickableAdapter;
import nl.adaptivity.android.recyclerview.ClickableAdapter.OnItemClickListener;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.FragmentOverviewBinding;
import nl.adaptivity.process.models.ProcessModelProvider;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.ui.model.OverviewPMCursorAdapter;
import nl.adaptivity.process.ui.model.OverviewPMCursorAdapter.OverviewPMViewHolder;
import nl.adaptivity.process.ui.model.ProcessModelListOuterFragment.ProcessModelListCallbacks;
import nl.adaptivity.process.ui.model.ProcessModelLoaderCallbacks;
import nl.adaptivity.process.ui.task.OverviewTaskCursorAdapter;
import nl.adaptivity.process.ui.task.TaskListOuterFragment.TaskListCallbacks;
import nl.adaptivity.process.ui.task.TaskLoaderCallbacks;
import nl.adaptivity.sync.RemoteXmlSyncAdapter;
import nl.adaptivity.sync.RemoteXmlSyncAdapter.XmlBaseColumns;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OverviewFragment.OverviewCallbacks} interface
 * to handle interaction events.
 * Use the {@link OverviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OverviewFragment extends TitleFragment implements OnItemClickListener {

  private static final int LOADER_TASKS = 1;
  private static final int LOADER_MODELS = 2;

  public static final int LIST_STATE_LOADING = 0;
  public static final int LIST_STATE_LOADED = 1;
  public static final int LIST_STATE_EMPTY = 2;
  public static final int LIST_STATE_ERROR = 3;

  @IntDef({LIST_STATE_LOADED, LIST_STATE_LOADED, LIST_STATE_EMPTY, LIST_STATE_ERROR})
  @Retention(RetentionPolicy.SOURCE)
  @interface ListState {}

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p/>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OverviewCallbacks extends ProcessModelListCallbacks, TaskListCallbacks{

    void showTasksFragment();

    void showModelsFragment();
  }

  private OverviewCallbacks mCallbacks;
  private FragmentOverviewBinding mBinding;
  private TaskLoaderCallbacks mTaskLoaderCallbacks;
  private ProcessModelLoaderCallbacks mPMLoaderCallbacks;

  // Object Initialization
  public OverviewFragment() {
    // Required empty public constructor
  }
// Object Initialization end

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_overview, container, false);
    mBinding.setFragment(this);
    OverviewTaskCursorAdapter taskAdapter = new OverviewTaskCursorAdapter(getActivity(), null);
    taskAdapter.setSelectionEnabled(false);
    taskAdapter.setOnItemClickListener(this);
    mBinding.overviewTaskList.setAdapter(taskAdapter);
    mTaskLoaderCallbacks = new TaskLoaderCallbacks(getActivity(), taskAdapter) {
      @Override
      public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        super.onLoadFinished(loader, data);
        if (data==null) {
          mBinding.setTasklistState(LIST_STATE_ERROR);
          mBinding.overviewTaskAlttext.setText(R.string.lbl_overview_tasklist_error);
        } else if (! data.moveToFirst()) {
          mBinding.setTasklistState(LIST_STATE_EMPTY);
        } else {
          mBinding.setTasklistState(LIST_STATE_LOADED);
        }
      }
    };

    OverviewPMCursorAdapter pmAdapter = new OverviewPMCursorAdapter(getActivity(), null);
    pmAdapter.setSelectionEnabled(false);
    pmAdapter.setOnItemClickListener(this);
    mBinding.overviewModelList.setAdapter(pmAdapter);
    mPMLoaderCallbacks = new ProcessModelLoaderCallbacks(getActivity(), pmAdapter) {

      @Override
      public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return new CursorLoader(mContext, ProcessModelProvider.ProcessModels.CONTENT_ID_URI_BASE, new String[] {BaseColumns._ID, ProcessModels.COLUMN_NAME, ProcessModels.COLUMN_INSTANCECOUNT, ProcessModels.COLUMN_MODEL}, ProcessModels.COLUMN_FAVOURITE + " != 0 AND (" + XmlBaseColumns.COLUMN_SYNCSTATE + " IS NULL OR ( " + XmlBaseColumns.COLUMN_SYNCSTATE + " != " + RemoteXmlSyncAdapter.SYNC_DELETE_ON_SERVER + " AND " + XmlBaseColumns.COLUMN_SYNCSTATE + " != " + RemoteXmlSyncAdapter.SYNC_NEWDETAILSPENDING + " ))", null, null);
      }

      @Override
      public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        super.onLoadFinished(loader, data);
        if (data==null) {
          mBinding.setModellistState(LIST_STATE_ERROR);
          mBinding.overviewModelAlttext.setText(R.string.lbl_overview_modellist_error);
        } else if (! data.moveToFirst()) {
          mBinding.setModellistState(LIST_STATE_EMPTY);
        } else {
          mBinding.setModellistState(LIST_STATE_LOADED);
        }
      }
    };

    final LoaderManager loaderManager = getLoaderManager();
    loaderManager.initLoader(LOADER_TASKS, null, mTaskLoaderCallbacks);
    loaderManager.initLoader(LOADER_MODELS, null, mPMLoaderCallbacks);


    return mBinding.getRoot();
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OverviewCallbacks) {
      mCallbacks = (OverviewCallbacks) context;
    } else {
      throw new RuntimeException(context.toString() + " must implement OverviewCallbacks");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mCallbacks = null;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
  }

  @Override
  public void onResume() {
    super.onResume();
    updateLayoutManagerColumnCount();
  }

  private void updateLayoutManagerColumnCount() {
    int minColWidth = getResources().getDimensionPixelSize(R.dimen.fragment_overview_min_col_width);
    {
      GridLayoutManager taskGlm = (GridLayoutManager) mBinding.overviewTaskList.getLayoutManager();
      int taskWidth = ((ViewGroup)mBinding.overviewTaskList.getParent()).getWidth();
      taskGlm.setSpanCount(Math.max(1,taskWidth / minColWidth));
    }
    {
      GridLayoutManager modelGlm = (GridLayoutManager) mBinding.overviewModelList.getLayoutManager();
      int modelWidth = ((ViewGroup)mBinding.overviewModelList.getParent()).getWidth();
      modelGlm.setSpanCount(Math.max(1,modelWidth / minColWidth));
    }
  }

  public void onPendingTasksClicked(View view) {
    if (mCallbacks!=null) {
      mCallbacks.showTasksFragment();
    }
  }

  public void onMoreModelsClicked(View view) {
    if (mCallbacks!=null) {
      mCallbacks.showModelsFragment();
    }
  }

  @Override
  public CharSequence getTitle(final Context context) {
    return context.getResources().getString(R.string.title_overview_fragment);
  }

  @Override
  public boolean onClickItem(final ClickableAdapter adapter, final ViewHolder viewHolder) {
    if (viewHolder instanceof OverviewPMCursorAdapter.OverviewPMViewHolder) {
      mCallbacks.onInstantiateModel(viewHolder.getItemId(), ((OverviewPMViewHolder)viewHolder).getBinding().getName().toString()+" instance");
    } else if (viewHolder instanceof OverviewTaskCursorAdapter.OverviewTaskCursorViewHolder) {
      mCallbacks.onShowTask(viewHolder.getItemId());
    }
    return false;
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @return A new instance of fragment OverviewFragment.
   */
  // TODO: Rename and change types and number of parameters
  public static OverviewFragment newInstance() {
    OverviewFragment fragment = new OverviewFragment();
//    Bundle args = new Bundle();
//    args.putString(ARG_PARAM1, param1);
//    args.putString(ARG_PARAM2, param2);
//    fragment.setArguments(args);
    return fragment;
  }


}
