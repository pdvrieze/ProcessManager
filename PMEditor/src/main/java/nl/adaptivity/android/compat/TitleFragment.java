package nl.adaptivity.android.compat;

import android.content.Context;
import android.support.v4.app.Fragment;

import java.util.Collection;


public abstract class TitleFragment extends Fragment {

  public abstract CharSequence getTitle(Context context);
}
