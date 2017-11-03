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

package nl.adaptivity.android.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.databinding.BindingMethod;
import android.databinding.BindingMethods;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.*;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import nl.adaptivity.process.editor.android.BuildConfig;
import nl.adaptivity.process.editor.android.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * A class that provides the capabilities needed for an material icon button
 * Created by pdvrieze on 19/12/15.
 */
@BindingMethods({
        @BindingMethod(type=IconButton.class,
                       attribute="iconTint",
                       method="setDrawableTint"),
        @BindingMethod(type=IconButton.class,
                attribute = "iconRes",
                method="setIconResource"),
        @BindingMethod(type=IconButton.class,
          attribute = "iconSrc",
          method="setIconDrawable")
})
public class IconButton extends ViewGroup {


  @SuppressWarnings("ClassNameSameAsAncestorName")
  public static class LayoutParams extends ViewGroup.LayoutParams {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ ROLE_ICON, ROLE_TITLE, ROLE_SUBTITLE})
    @interface LayoutRole {}
    public static final int ROLE_ICON = 0;
    public static final int ROLE_TITLE= 1;
    public static final int ROLE_SUBTITLE = 2;

    @LayoutRole
    public int role;

    public LayoutParams(@NonNull final Context c, final AttributeSet attrs) {
      super(c, attrs);
      final TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.IconButtonLP);
      //noinspection ResourceType
      role = a.getInt(R.styleable.IconButtonLP_role, ROLE_TITLE);
      a.recycle();
    }

    public LayoutParams(@LayoutRole final int role) {
      super(role==ROLE_ICON ? WRAP_CONTENT : MATCH_PARENT, role==ROLE_ICON ? MATCH_PARENT : WRAP_CONTENT);
      this.role = role;
    }

    public LayoutParams(final ViewGroup.LayoutParams source) {
      super(source);
      if (source instanceof LayoutParams) {
        role = ((LayoutParams)source).role;
      }
    }
  }

  @TargetApi(17)
  private static final class Compat17 {

    public static int densityDpi(final Configuration configuration) {
      return configuration.densityDpi;
    }
  }

  public static final int DEFAULT_ICON_PADDING_DP = 32;
  public static final int DEFAULT_ICON_WIDTH_DP = 24;
  public static final int DEFAULT_ICON_HEIGHT_DP = 24;

  private           int            mIconPadding;
  private           int            mIconWidth;
  private           int            mIconHeight;
  @Nullable private Drawable       mIconDrawable;
  private           ColorStateList mDrawableTint;

  public IconButton(final Context context) {
    super(context);
  }

  public IconButton(@NonNull final Context context, final AttributeSet attrs) {
    this(context, attrs, R.attr.iconButtonStyle);
  }

  public IconButton(@NonNull final Context context, final AttributeSet attrs, @AttrRes final int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    applyAttrs(context, attrs, defStyleAttr, R.style.Widget_IconButton);
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  public IconButton(@NonNull final Context context, final AttributeSet attrs, final int defStyleAttr, @StyleRes
  final int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    applyAttrs(context, attrs, defStyleAttr, defStyleRes);
  }

  private static int densityDpi(@NonNull Context context) {
    if (VERSION.SDK_INT >= 17) {
      return Compat17.densityDpi(context.getResources().getConfiguration());
    } else {
      return context.getResources().getDisplayMetrics().densityDpi;
    }
  }

  private void applyAttrs(final Context context, final AttributeSet attrs, final int defStyleAttr, @StyleRes
  final int defStyleRes) {
    final TypedArray a   = context.obtainStyledAttributes(attrs, R.styleable.IconButton, defStyleAttr, defStyleRes == 0 ? R.style.Widget_IconButton : defStyleRes);
    final int        dpi = densityDpi(context);
    mIconPadding = a.getDimensionPixelOffset(R.styleable.IconButton_iconPadding, DEFAULT_ICON_PADDING_DP * 160 / dpi);
    mIconWidth = a.getDimensionPixelSize(R.styleable.IconButton_iconWidth, 0);
    mIconHeight = a.getDimensionPixelSize(R.styleable.IconButton_iconHeight, 0);
    mDrawableTint = a.getColorStateList(R.styleable.IconButton_iconTint);
    setIconDrawable(a.getDrawable(R.styleable.IconButton_iconSrc));
    a.recycle();
  }

  @Override
  protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
    View iconView = null;
    View titleView = null;
    View subtitleView = null;
    for (int i = 0; i < getChildCount(); i++) {
      final View child = getChildAt(i);
      iconView = getIfMatches(child, LayoutParams.ROLE_ICON, iconView, "Setting multiple icons in an IconButton will not work");
      titleView = getIfMatches(child, LayoutParams.ROLE_TITLE, titleView, "Setting multiple titles in an IconButton will not work");
      subtitleView = getIfMatches(child, LayoutParams.ROLE_SUBTITLE, subtitleView, "Setting multiple subtitles in an IconButton will not work");
    }
    if (mIconDrawable!=null && iconView!=null) {
      if (BuildConfig.DEBUG) {
        throw new IllegalStateException("IconButton can not display both an icon child and an iconSrc");
      }
      iconView=null;
    }

    final int hPadding = ViewCompat.getPaddingStart(this) + ViewCompat.getPaddingEnd(this);
    final int dpi = densityDpi(getContext());
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int innerWidth = MeasureSpec.getSize(widthMeasureSpec) - hPadding;
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    final int innerHeightMode = heightMode==MeasureSpec.EXACTLY ? MeasureSpec.AT_MOST : heightMode;
    final int vPadding = getPaddingTop() + getPaddingBottom();
    final int innerHeight = MeasureSpec.getSize(heightMeasureSpec) - vPadding;

    int usedWidth = hPadding;
    int usedHeight = vPadding;

    final ViewGroup.LayoutParams iconLp = iconView == null ? null : iconView.getLayoutParams();
    final int                    iconWidth;
    if (mIconWidth > 0) {
      iconWidth = mIconWidth;
    } else if (iconView!=null) {
      if (mIconWidth > 0) {
        iconWidth = mIconWidth;
      } else {
        if (iconLp.width > 0) {
          iconWidth = iconLp.width;
        } else {
          iconWidth = DEFAULT_ICON_WIDTH_DP * dpi / 160;
        }
      }
    } else {
      iconWidth = DEFAULT_ICON_WIDTH_DP * dpi / 160;
    }
    usedWidth += iconWidth + mIconPadding; // the width available for the text views

    if (titleView!=null) {
      titleView.measure(MeasureSpec.makeMeasureSpec(innerWidth-usedWidth, widthMode), MeasureSpec.makeMeasureSpec(innerHeight-usedHeight,innerHeightMode));
      usedHeight+=titleView.getMeasuredHeight();
      if (subtitleView==null) {
        usedWidth+=titleView.getMeasuredWidth(); // record with;
      }
    }
    if (subtitleView!=null) {
      subtitleView.measure(MeasureSpec.makeMeasureSpec(innerWidth, widthMode), MeasureSpec.makeMeasureSpec(innerHeight-usedHeight,innerHeightMode));
      usedHeight+=subtitleView.getMeasuredHeight();
      if (titleView!=null) {
        usedWidth += Math.max(titleView.getMeasuredWidth(), subtitleView.getMeasuredWidth());
      } else {
        usedWidth += subtitleView.getMeasuredWidth();
      }
    }

    int iconHeight;
    if (mIconHeight>0) {
      iconHeight = mIconHeight;
    } else if (iconView!=null) {
      if (iconLp.height>0) {
        iconHeight = iconLp.height;
      } else {
        iconHeight = DEFAULT_ICON_HEIGHT_DP * dpi / 160;
      }
    } else {
      iconHeight = DEFAULT_ICON_HEIGHT_DP * dpi / 160;
    }
    iconHeight = Math.min(innerHeight, iconHeight);
    if (iconView!=null) {
      iconView.measure(MeasureSpec.makeMeasureSpec(iconWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(iconHeight, MeasureSpec.EXACTLY));
    }

    {
      int measuredWidth = usedWidth;
      int measuredHeight = Math.max(iconHeight+vPadding, usedHeight);
      switch (widthMode) {
        case MeasureSpec.EXACTLY:
          measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
          break;
        case MeasureSpec.AT_MOST:
          measuredWidth = Math.min(MeasureSpec.getSize(widthMeasureSpec), measuredWidth);
          break;
        case MeasureSpec.UNSPECIFIED:
          // do nothing
      }
      switch (heightMode) {
        case MeasureSpec.EXACTLY:
          measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
          break;
        case MeasureSpec.AT_MOST:
          measuredHeight = Math.min(MeasureSpec.getSize(heightMeasureSpec), measuredHeight);
          break;
        case MeasureSpec.UNSPECIFIED:
          // do nothing
      }
      setMeasuredDimension(measuredWidth, measuredHeight);
    }

  }

  @Nullable
  private View getIfMatches(final View candidate, final int role, @Nullable final View origView, final String errorMessage) {
    final LayoutParams lp = (LayoutParams) candidate.getLayoutParams();
    if (lp.role==role) {
      if (origView!=null) {
        throw new IllegalStateException(errorMessage);
      }
      return candidate;
    }
    return origView;
  }


  @Override
  protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
    View iconView = null;
    View titleView = null;
    View subtitleView = null;
    for (int i = 0; i < getChildCount(); i++) {
      final View child = getChildAt(i);
      iconView = getIfMatches(child, LayoutParams.ROLE_ICON, iconView, "Setting multiple icons in an IconButton will not work");
      titleView = getIfMatches(child, LayoutParams.ROLE_TITLE, titleView, "Setting multiple titles in an IconButton will not work");
      subtitleView = getIfMatches(child, LayoutParams.ROLE_SUBTITLE, subtitleView, "Setting multiple subtitles in an IconButton will not work");
    }
    final boolean ltr =(ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_LTR);
    @Dimension(unit=Dimension.PX)int layoutOffsetX = ltr ? ViewCompat.getPaddingStart(this) : (r-l) - ViewCompat.getPaddingStart(this);
    if (iconView!=null) {
      @Dimension(unit=Dimension.PX) final int cl = ltr ? (layoutOffsetX) : layoutOffsetX - iconView.getMeasuredWidth();
      @Dimension(unit=Dimension.PX) final int cr = cl + iconView.getMeasuredWidth();
      @Dimension(unit=Dimension.PX) final int ct = ((b - t) - iconView.getMeasuredHeight()) / 2;
      @Dimension(unit=Dimension.PX) final int cb = ct + iconView.getMeasuredHeight();
      iconView.layout(cl, ct, cr, cb);
      if (ltr) {
        layoutOffsetX += iconView.getMeasuredWidth() + mIconPadding;
      } else {
        layoutOffsetX -= iconView.getMeasuredWidth() + mIconPadding;
      }
    } else {
      if (mIconDrawable!=null) {
        final int cl = ltr ? (layoutOffsetX) : layoutOffsetX - mIconWidth;
        final int cr = cl + mIconWidth;
        final int ct = ((b - t) - mIconHeight) / 2;
        final int cb = ct + mIconHeight;
        mIconDrawable.setBounds(cl, ct, cr, cb);
      }
      if (ltr) {
        layoutOffsetX += mIconWidth + mIconPadding;
      } else {
        layoutOffsetX -= mIconWidth + mIconPadding;
      }
    }
    if (titleView!=null || subtitleView!=null) {
      final int cl;
      final int cr;
      {
        final int w;
        if (titleView != null) {
          if (subtitleView != null) {
            w = Math.max(titleView.getMeasuredWidth(), subtitleView.getMeasuredWidth());
          } else {
            w = titleView.getMeasuredWidth();
          }
        } else {
          w = subtitleView.getMeasuredWidth();
        }
        cl = ltr ? layoutOffsetX : (layoutOffsetX - w);
        cr = cl + w;
      }


      if (titleView != null) {
        final int ct;
        if (subtitleView != null) {
          ct = ((b - t) - (titleView.getMeasuredHeight() + subtitleView.getMeasuredHeight())) / 2;
        } else {
          ct = ((b - t) - (titleView.getMeasuredHeight())) / 2;
        }
        final int cb = ct + titleView.getMeasuredHeight();
        titleView.layout(cl, ct, cr, cb);
        if (subtitleView != null) {
          final int st = cb;
          final int sb = ct + subtitleView.getMeasuredHeight();
          subtitleView.layout(cl, st, cr, sb);
        }
      } else { // subtitleview is always true in this case
        final int ct = ((b - t) - subtitleView.getMeasuredHeight()) / 2;
        final int cb = ct + subtitleView.getMeasuredHeight();
        subtitleView.layout(cl, ct, cr, cb);
      }
    }
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    for (int i = 0; i < getChildCount(); i++) {
      getChildAt(i).setEnabled(enabled);
    }
  }

  public int getIconPadding() {
    return mIconPadding;
  }

  public void setIconPadding(final int iconPadding) {
    mIconPadding = iconPadding;
  }

  public int getIconWidth() {
    return mIconWidth;
  }

  public void setIconWidth(final int iconWidth) {
    mIconWidth = iconWidth;
  }

  public int getIconHeight() {
    return mIconHeight;
  }

  public void setIconHeight(final int iconHeight) {
    mIconHeight = iconHeight;
  }

  @Nullable
  public Drawable getIconDrawable() {
    return mIconDrawable;
  }

  public void setIconDrawable(@Nullable final Drawable iconDrawable) {
    mIconDrawable = iconDrawable;
    if (iconDrawable!=null) {
      DrawableCompat.setLayoutDirection(iconDrawable, ViewCompat.getLayoutDirection(this));
      applyDrawableTint();
      requestLayout(); // Layout will set the positions
    }
  }

  private void applyDrawableTint() {
    mIconDrawable.setState(getDrawableState());
    if (mDrawableTint != null && (mDrawableTint.isStateful() || mDrawableTint.getDefaultColor() != 0)) {
      DrawableCompat.setTintList(mIconDrawable, mDrawableTint);
    }
  }

  public void setIconResource(@DrawableRes final int iconRes) {
    setIconDrawable(iconRes==0 ? null : getResources().getDrawable(iconRes));
  }

  public ColorStateList getDrawableTint() {
    return mDrawableTint;
  }

  public void setDrawableTint(@ColorInt final int color) {
    mDrawableTint = ColorStateList.valueOf(color);
  }

  public void setDrawableTint(final ColorStateList drawableTint) {
    mDrawableTint = drawableTint;
    if (mIconDrawable!=null) {
      applyDrawableTint();
    }
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();
    if (mIconDrawable!=null) {
      mIconDrawable.setState(getDrawableState());
    }
  }

  @Override
  public void onDraw(final Canvas c) {
    super.onDraw(c);
    if (mIconDrawable!=null) {
      mIconDrawable.draw(c);
    }
  }

  @Override
  protected boolean checkLayoutParams(final ViewGroup.LayoutParams p) {
    if (! (p instanceof LayoutParams)) { return false; }
    final LayoutParams lp = (LayoutParams) p;
    return lp.role>=0 && lp.role<3 && super.checkLayoutParams(p);
  }

  @NonNull
  @Override
  public ViewGroup.LayoutParams generateLayoutParams(final AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  @NonNull
  @Override
  protected ViewGroup.LayoutParams generateLayoutParams(final ViewGroup.LayoutParams p) {
    return new LayoutParams(p);
  }

  @NonNull
  @Override
  protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(LayoutParams.ROLE_TITLE);
  }
}
