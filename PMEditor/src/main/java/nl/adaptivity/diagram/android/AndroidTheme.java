package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.ThemeItem;
import nl.adaptivity.process.diagram.ProcessThemeItems;
import android.graphics.Color;
import android.util.SparseArray;


public class AndroidTheme implements Theme<AndroidStrategy, AndroidPen, AndroidPath> {

  private static final int SHADE_STATE_MASK = nl.adaptivity.diagram.Drawable.STATE_SELECTED | nl.adaptivity.diagram.Drawable.STATE_TOUCHED;
  public static final float SHADER_RADIUS = 8f;

  public static final int TOUCHED_SHADE_COLOR=Color.argb(0xb0, 0xff, 0xec, 0x1a);
  public static final int SELECTED_SHADE_COLOR=Color.argb(0xb0, 23, 166, 255);


  private final AndroidStrategy mStrategy;
  private SparseArray<SparseArray<AndroidPen>> mPens;

  public AndroidTheme(AndroidStrategy strategy){
    mStrategy = strategy;
    mPens = new SparseArray<>();
  }

  @Override
  public AndroidPen getPen(ThemeItem item, int state) {
    int itemState = item.getEffectiveState(state);
    int themeState = overrideState(item, state, itemState);
    SparseArray<AndroidPen> statePens = mPens.get(item.getItemNo());
    if (statePens==null) {
      statePens = new SparseArray<>();
      mPens.append(item.getItemNo(), statePens);
    }

    AndroidPen result = statePens.get(themeState);
    if (result==null) {
      result = item.createPen(mStrategy, itemState);
      result = overrideTheme(result, item, themeState);
      statePens.append(themeState, result);
    }

    return result;
  }

  /**
   * Override the state provided by the themeItem.
   * @param item The item for which to override the state.
   * @param state The state present.
   * @param itemState The effective state of the item from the item's perspective
   * @return
   */
  private static int overrideState(ThemeItem item, int state, int itemState) {
    if (item instanceof ProcessThemeItems) {
      switch ((ProcessThemeItems) item) {
        case BACKGROUND:
        case ENDNODEOUTERLINE:
        case LINEBG:
          return itemState | (state & SHADE_STATE_MASK);
        case LINE:
        default:
          return itemState;
      }
    }
    return itemState;
  }

  /**
   * Add a method that allows the theme from PE-diagram to be overridden for android. The current purpose
   * is to enable blur shadows.
   * @param pen The pen to override.
   * @param item The item for which the pen is.
   * @param state The state of the item.
   * @return The overridden pen. Optimally this is actually the same pen passed in.
   */
  private static AndroidPen overrideTheme(AndroidPen pen, ThemeItem item, int state) {
    if (item instanceof ProcessThemeItems) {
      switch ((ProcessThemeItems) item) {
        case BACKGROUND:
        case ENDNODEOUTERLINE:
        case LINEBG:
          break;
        case LINE:
        default:
          return pen;
      }
      if ((state & nl.adaptivity.diagram.Drawable.STATE_TOUCHED)>0) {
        pen.setShadowLayer(SHADER_RADIUS, TOUCHED_SHADE_COLOR);
      } else if ((state & nl.adaptivity.diagram.Drawable.STATE_SELECTED)>0) {
        pen.setShadowLayer(SHADER_RADIUS, SELECTED_SHADE_COLOR);
      }
    }
    return pen;
  }

}
