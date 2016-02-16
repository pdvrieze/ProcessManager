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
import net.devrieze.util.CollectionUtil;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.util.VariableReference;


/**
 * Created by pdvrieze on 15/02/16.
 */
public class VariableSpan extends ReplacementSpan {

  private static final String TAG = "VariableSpan";

  private final VariableReference mReference;
  @DrawableRes private final int mBorderId;
  private final Context mContext;
  private Drawable mBorder;
  private StaticLayout mLayout;
  private final Rect mPadding = new Rect(0,0,0,0);

  public VariableSpan(final Context context, final VariableReference reference, @DrawableRes final int borderId) {
    mReference = reference;
    mBorderId = borderId;
    mContext = context;
  }

  @Override
  public int getSize(final Paint paint, final CharSequence text, final int start, final int end, final FontMetricsInt fm) {
    final TextPaint textPaint = paint instanceof TextPaint ? (TextPaint) paint : new TextPaint(paint);
    if (mLayout == null) {
      // Create a copy without this spannable as that would create an infinite loop
      final SpannableStringBuilder myText = new SpannableStringBuilder(text, start, end);
      for (final VariableSpan span: myText.getSpans(0, end - start, VariableSpan.class)) {
        myText.removeSpan(span);
      }
      final int desiredWidth = (int) Math.ceil(Layout.getDesiredWidth(myText, 0, myText.length(), textPaint)+100);
      mLayout = new StaticLayout(myText, 0, myText.length(), textPaint, desiredWidth, Alignment.ALIGN_NORMAL, 1f, 0f, false);
    }
    if (mBorder==null && mBorderId!=0) {
      mBorder = mContext.getDrawable(mBorderId);
    }
    if (mBorder!=null) { mBorder.getPadding(mPadding); }
    final int textWidth = (int) Math.ceil(mLayout.getLineMax(0) + mPadding.left + mPadding.right);
    if (fm!=null) {
      fm.ascent = mLayout.getLineAscent(0) - mPadding.top;
      fm.top = fm.ascent;
      fm.descent = mLayout.getLineDescent(0) + mPadding.bottom;
      fm.bottom = fm.descent;
    }

    return textWidth;
  }

  @Override
  public void draw(final Canvas canvas, final CharSequence text, final int start, final int end, final float x, final int top, final int y, final int bottom, final Paint paint) {
    Log.d(TAG, "draw() called with: " + "canvas = [" + canvas + "], text = [" + text + "], start = [" + start + "], end = [" + end + "], x = [" + x + "], top = [" + top + "], y = [" + y + "], bottom = [" + bottom + "], paint = [" + paint + "]");
    final int save = canvas.save();
    canvas.translate(x, top);
    if (mBorder!=null) {
      mBorder.setBounds(0, 0, (int) Math.ceil(mLayout.getLineMax(0)+mPadding.left+mPadding.right), bottom);
      mBorder.draw(canvas);
    }
    canvas.translate(mPadding.left, -top +y+ mLayout.getLineAscent(0));
    mLayout.draw(canvas);
    canvas.restoreToCount(save);
  }

  /**
   * Create a new span representing the reference. This method will attempt to display a user friendly label. If a define exists
   * purely to pass forward a result from another activity, display it as that (but don't forget about the define).
   * @param context The context for resolving
   * @param define The define referred to, if known and applicable.
   * @param variableReference The variable to refer. This is either a define ref or a result ref
   * @param borderDrawableId The id of the border drawable
   * @return The spanned.
   */
  public static Spanned newVarSpanned(final Context context, final XmlDefineType define, final VariableReference variableReference, final int borderDrawableId) {
    CharSequence label;
    if (define!=null && CollectionUtil.isNullOrEmpty(define.getContent()) && (StringUtil.isNullOrEmpty(define.getPath())||StringUtil.isEqual(".", define.getPath()))) {
      label = VariableReference.newDefineReference(define).getLabel();
    } else if (StringUtil.isNullOrEmpty(variableReference.getVariableName()) && (StringUtil.isNullOrEmpty(variableReference.getXPath())|| ".".equals(variableReference.getXPath()))) {
      label = VariableReference.newResultReference(define.getRefNode(), define.getRefName(), define.getPath()).getLabel();
    } else {
      label = variableReference.getLabel();
    }
    final SpannableStringBuilder builder = new SpannableStringBuilder(label);
    builder.setSpan(new VariableSpan(context, variableReference, borderDrawableId), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    return SpannableString.valueOf(builder);
  }

  public VariableReference getReference() {
    return mReference;
  }
}
