package nl.adaptivity.process.ui;

import android.databinding.BindingAdapter;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * Created by pdvrieze on 17/12/15.
 */
public class BindingAdapters {

  private BindingAdapters() {}

  @BindingAdapter("bind:textRes")
  public static void setTextRes(TextView view, @StringRes int textRes) {
    if (textRes==0) {
      view.setText(null);
    } else {
      view.setText(textRes);
    }
  }

  @BindingAdapter("bind:drawableRes")
  public static void setDrawableRes(ImageView view, @DrawableRes int drawableRes) {
    if (drawableRes==0) {
      view.setImageDrawable(null);
    } else {
      view.setImageResource(drawableRes);
    }
  }
}
