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

package nl.adaptivity.process.ui.activity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.text.*;
import android.text.Layout.Alignment;
import android.text.style.ReplacementSpan;
import android.util.Log;
import net.devrieze.util.StringUtil;


/**
 * Created by pdvrieze on 15/02/16.
 */
public class VariableSpan extends ReplacementSpan {

  private static final String TAG = "VariableSpan";

  @DrawableRes private final int mBorderId;
  private final Context mContext;
  private Drawable mBorder;
  private StaticLayout mLayout;
  private final Rect mPadding = new Rect(0,0,0,0);

  public VariableSpan(Context context, @DrawableRes final int borderId) {
    mBorderId = borderId;
    mContext = context;
  }

  @Override
  public int getSize(final Paint paint, final CharSequence text, final int start, final int end, final FontMetricsInt fm) {
    TextPaint textPaint = paint instanceof TextPaint ? (TextPaint) paint : new TextPaint(paint);
    if (mLayout == null) {
      // Create a copy without this spannable as that would create an infinite loop
      SpannableStringBuilder myText = new SpannableStringBuilder(text, start, end);
      for (VariableSpan span: myText.getSpans(0, end - start, VariableSpan.class)) {
        myText.removeSpan(span);
      }
      int desiredWidth = (int) Math.ceil(Layout.getDesiredWidth(myText, 0, myText.length(), textPaint));
      mLayout = new StaticLayout(myText, 0, myText.length(), textPaint, desiredWidth, Alignment.ALIGN_NORMAL, 1f, 0f, false);
    }
    if (mBorder==null && mBorderId!=0) {
      mBorder = mContext.getDrawable(mBorderId);
    }
    if (mBorder!=null) { mBorder.getPadding(mPadding); }
    int textWidth = mLayout.getWidth() + mPadding.left + mPadding.right;
    if (fm!=null) {
      fm.ascent = mLayout.getLineAscent(0) - mPadding.top;
      fm.top = mLayout.getLineAscent(0) - mPadding.top;
      fm.descent = mLayout.getLineDescent(0) + mPadding.bottom;
      fm.bottom = fm.descent;
    }
    int textHeight = mLayout.getHeight() + mPadding.top + mPadding.bottom;

    Log.d(TAG, "getSize: "+textWidth +" x "+textHeight);
    return textWidth;
  }

  @Override
  public void draw(final Canvas canvas, final CharSequence text, final int start, final int end, final float x, final int top, final int y, final int bottom, final Paint paint) {
    Log.d(TAG, "draw() called with: " + "canvas = [" + canvas + "], text = [" + text + "], start = [" + start + "], end = [" + end + "], x = [" + x + "], top = [" + top + "], y = [" + y + "], bottom = [" + bottom + "], paint = [" + paint + "]");
    int save = canvas.save();
    canvas.translate(x, top);
    if (mBorder!=null) {
      mBorder.setBounds(0, 0, mLayout.getWidth()+mPadding.left+mPadding.right, bottom);
      mBorder.draw(canvas);
    }
    canvas.translate(mPadding.left, -top +y+ mLayout.getLineAscent(0));
    mLayout.draw(canvas);
    canvas.restoreToCount(save);
  }

  public static Spanned newVarSpanned(final Context context, final String varName, final int borderDrawableId) {
    SpannableStringBuilder builder = new SpannableStringBuilder(varName);
    builder.setSpan(new VariableSpan(context, borderDrawableId), 0, varName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    return SpannableString.valueOf(builder);
  }
}
