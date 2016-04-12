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

package nl.adaptivity.process.ui;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BindingAdapter;
import android.graphics.drawable.Drawable;
import android.support.annotation.BoolRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import nl.adaptivity.process.editor.android.R;


/**
 * Created by pdvrieze on 17/12/15.
 */
public class BindingAdapters {

  private static final String TAG = "BindingAdapters";

  private BindingAdapters() {}

  @BindingAdapter("textRes")
  public static void setTextRes(TextView view, @StringRes int textRes) {
    if (textRes==0) {
      view.setText(null);
    } else {
      view.setText(textRes);
    }
  }

  @BindingAdapter("drawableRes")
  public static void setDrawableRes(ImageView view, @DrawableRes int drawableRes) {
    if (drawableRes==0) {
      view.setImageDrawable(null);
    } else {
      view.setImageResource(drawableRes);
    }
  }

}
