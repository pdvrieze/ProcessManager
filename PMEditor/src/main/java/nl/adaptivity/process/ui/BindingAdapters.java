/*
 * Copyright (c) 2017.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.ui;

import android.databinding.BindingAdapter;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * Created by pdvrieze on 17/12/15.
 */
public final class BindingAdapters {

  private static final String TAG = "BindingAdapters";

  private BindingAdapters() {}

  @BindingAdapter("textRes")
  public static void setTextRes(final TextView view, @StringRes final int textRes) {
    if (textRes==0) {
      view.setText(null);
    } else {
      view.setText(textRes);
    }
  }

  @BindingAdapter("drawableRes")
  public static void setDrawableRes(final ImageView view, @DrawableRes final int drawableRes) {
    if (drawableRes==0) {
      view.setImageDrawable(null);
    } else {
      view.setImageResource(drawableRes);
    }
  }

}
