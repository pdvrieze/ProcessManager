package nl.adaptivity.android.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.databinding.BindingMethod;
import android.databinding.BindingMethods;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
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
                       attribute="bind:iconTint",
                       method="setDrawableTint"),
        @BindingMethod(type=IconButton.class,
                attribute = "bind:iconRes",
                method="setIconResource"),
        @BindingMethod(type=IconButton.class,
          attribute = "bind:iconSrc",
          method="setIconDrawable")
})
public class IconButton extends ViewGroup {


  public static class LayoutParams extends ViewGroup.LayoutParams {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ ROLE_ICON, ROLE_TITLE, ROLE_SUBTITLE})
    @interface LayoutRole {}
    public static final int ROLE_ICON = 0;
    public static final int ROLE_TITLE= 1;
    public static final int ROLE_SUBTITLE = 2;

    @LayoutRole
    public int role;

    public LayoutParams(final Context c, final AttributeSet attrs) {
      super(c, attrs);
      TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.IconButtonLP);
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

  private static class Compat17 {

  }

  public static final int DEFAULT_ICON_PADDING_DP = 32;
  public static final int DEFAULT_ICON_WIDTH_DP = 24;
  public static final int DEFAULT_ICON_HEIGHT_DP = 24;

  private int mIconPadding;
  private int mIconWidth;
  private int mIconHeight;
  private Drawable mIconDrawable;
  private ColorStateList mDrawableTint;

  public IconButton(final Context context) {
    super(context);
  }

  public IconButton(final Context context, final AttributeSet attrs) {
    this(context, attrs, R.attr.iconButtonStyle);
  }

  public IconButton(final Context context, final AttributeSet attrs, final int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    applyAttrs(context, attrs, defStyleAttr, R.style.Widget_IconButton);
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  public IconButton(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    applyAttrs(context, attrs, defStyleAttr, defStyleRes);
  }

  private void applyAttrs(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconButton, defStyleAttr, defStyleRes == 0 ? R.style.Widget_IconButton : defStyleRes);
    int dpi = context.getResources().getConfiguration().densityDpi;
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
      View child = getChildAt(i);
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
    final int dpi = getResources().getConfiguration().densityDpi;
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int innerWidth = MeasureSpec.getSize(widthMeasureSpec) - hPadding;
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    final int innerHeightMode = heightMode==MeasureSpec.EXACTLY ? MeasureSpec.AT_MOST : heightMode;
    final int vPadding = getPaddingTop() + getPaddingBottom();
    final int innerHeight = MeasureSpec.getSize(heightMeasureSpec) - vPadding;

    int usedWidth = hPadding;
    int usedHeight = vPadding;

    ViewGroup.LayoutParams iconLp = iconView==null ? null : iconView.getLayoutParams();
    int iconWidth;
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

  private View getIfMatches(final View candidate, final int role, final View origView, final String errorMessage) {
    LayoutParams lp = (LayoutParams) candidate.getLayoutParams();
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
      View child = getChildAt(i);
      iconView = getIfMatches(child, LayoutParams.ROLE_ICON, iconView, "Setting multiple icons in an IconButton will not work");
      titleView = getIfMatches(child, LayoutParams.ROLE_TITLE, titleView, "Setting multiple titles in an IconButton will not work");
      subtitleView = getIfMatches(child, LayoutParams.ROLE_SUBTITLE, subtitleView, "Setting multiple subtitles in an IconButton will not work");
    }
    final boolean ltr =(ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_LTR);
    int layoutOffsetX = ltr ? ViewCompat.getPaddingStart(this) : (r-l) - ViewCompat.getPaddingStart(this);
    if (iconView!=null) {
      int cl = ltr ? (layoutOffsetX) : layoutOffsetX-iconView.getMeasuredWidth();
      int cr = cl + iconView.getMeasuredWidth();
      int ct = ((b-t) - iconView.getMeasuredHeight())/2;
      int cb = ct + iconView.getMeasuredHeight();
      iconView.layout(cl, ct, cr, cb);
      layoutOffsetX +=iconView.getMeasuredWidth()+mIconPadding;
    } else {
      if (mIconDrawable!=null) {
        int cl = ltr ? (layoutOffsetX) : layoutOffsetX-mIconWidth;
        int cr = cl + mIconWidth;
        int ct = ((b-t) - mIconHeight)/2;
        int cb = ct + mIconHeight;
        mIconDrawable.setBounds(cl, ct, cr, cb);
      }
      layoutOffsetX +=mIconWidth+mIconPadding;
    }
    int cl = ltr ? layoutOffsetX : ((r-l)-layoutOffsetX-titleView.getMeasuredWidth());
    int cr = cl + titleView.getMeasuredWidth();
    if (titleView!=null) {
      int ct;
      if (subtitleView!=null) {
        ct = ((b-t)-(titleView.getMeasuredHeight()+subtitleView.getMeasuredHeight()))/2;
      } else {
        ct = ((b-t)-(titleView.getMeasuredHeight()))/2;
      }
      int cb = ct+titleView.getMeasuredHeight();
      titleView.layout(cl, ct, cr, cb);
      if (subtitleView!=null) {
        ct=cb;
        cb=ct+subtitleView.getMeasuredHeight();
        subtitleView.layout(cl, ct, cr, cb);
      }
    } else if (subtitleView!=null) {
      int ct = ((b-t)-subtitleView.getMeasuredHeight())/2;
      int cb = ct+subtitleView.getMeasuredHeight();
      subtitleView.layout(cl, ct, cr, cb);
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

  public Drawable getIconDrawable() {
    return mIconDrawable;
  }

  public void setIconDrawable(final Drawable iconDrawable) {
    mIconDrawable = iconDrawable;
    if (iconDrawable!=null) {
      DrawableCompat.setLayoutDirection(iconDrawable, ViewCompat.getLayoutDirection(this));
      applyDrawableTint();
      requestLayout(); // Layout will set the positions
    }
  }

  private void applyDrawableTint() {
    if (mDrawableTint != null && (mDrawableTint.isStateful() || mDrawableTint.getDefaultColor() != 0)) {
      DrawableCompat.setTintList(mIconDrawable, mDrawableTint);
      mIconDrawable.setState(getDrawableState());
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
  public void onDraw(Canvas c) {
    super.onDraw(c);
    if (mIconDrawable!=null) {
      mIconDrawable.draw(c);
    }
  }

  @Override
  protected boolean checkLayoutParams(final ViewGroup.LayoutParams p) {
    if (! (p instanceof LayoutParams)) { return false; }
    LayoutParams lp = (LayoutParams) p;
    return lp.role>=0 && lp.role<3 && super.checkLayoutParams(p);
  }

  @Override
  public ViewGroup.LayoutParams generateLayoutParams(final AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  @Override
  protected ViewGroup.LayoutParams generateLayoutParams(final ViewGroup.LayoutParams p) {
    return new LayoutParams(p);
  }

  @Override
  protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(LayoutParams.ROLE_TITLE);
  }
}
