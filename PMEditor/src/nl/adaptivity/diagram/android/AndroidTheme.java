package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.ThemeItem;
import nl.adaptivity.process.diagram.ProcessThemeItems;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;


public class AndroidTheme implements Theme<AndroidStrategy, AndroidPen, AndroidPath> {

  private static final int SHADE_STATE_MASK = nl.adaptivity.diagram.Drawable.STATE_SELECTED | nl.adaptivity.diagram.Drawable.STATE_TOUCHED;
  public static final float SHADER_RADIUS = 18f;

  public static final int TOUCHED_SHADE_COLOR=Color.argb(0xb0, 0xff, 0xec, 0x1a);
  public static final int SELECTED_SHADE_COLOR=Color.argb(0xb0, 23, 166, 255);


  private final AndroidStrategy aStrategy;
  private SparseArray<SparseArray<AndroidPen>> aPens;

  public AndroidTheme(AndroidStrategy pStrategy){
    aStrategy = pStrategy;
    aPens = new SparseArray<>();
  }

  @Override
  public AndroidPen getPen(ThemeItem pItem, int pState) {
    int itemState = pItem.getEffectiveState(pState);
    int themeState = overrideState(pItem, pState, itemState);
    SparseArray<AndroidPen> statePens = aPens.get(pItem.getItemNo());
    if (statePens==null) {
      statePens = new SparseArray<>();
      aPens.append(pItem.getItemNo(), statePens);
    }

    AndroidPen result = statePens.get(themeState);
    if (result==null) {
      result = pItem.createPen(aStrategy, itemState);
      result = overrideTheme(result, pItem, themeState);
      statePens.append(themeState, result);
    }

    return result;
  }

  /**
   * Override the state provided by the themeItem.
   * @param pItem The item for which to override the state.
   * @param pState The state present.
   * @param pItemState The effective state of the item from the item's perspective
   * @return
   */
  private int overrideState(ThemeItem pItem, int pState, int pItemState) {
    if (pItem instanceof ProcessThemeItems) {
      switch ((ProcessThemeItems) pItem) {
        case BACKGROUND:
        case ENDNODEOUTERLINE:
        case LINEBG:
          return pItemState | (pState & SHADE_STATE_MASK);
        case LINE:
        default:
          return pItemState;
      }
    }
    return pItemState;
  }

  /**
   * Add a method that allows the theme from PE-diagram to be overridden for android. The current purpose
   * is to enable blur shadows.
   * @param pPen The pen to override.
   * @param pItem The item for which the pen is.
   * @param pState The state of the item.
   * @return The overridden pen. Optimally this is actually the same pen passed in.
   */
  private AndroidPen overrideTheme(AndroidPen pPen, ThemeItem pItem, int pState) {
    if (pItem instanceof ProcessThemeItems) {
      switch ((ProcessThemeItems) pItem) {
        case BACKGROUND:
        case ENDNODEOUTERLINE:
        case LINEBG:
          break;
        case LINE:
        default:
          return pPen;
      }
      if ((pState & nl.adaptivity.diagram.Drawable.STATE_SELECTED)>0) {
        pPen.setShadowLayer(AndroidExtraThemeItem.SHADER_RADIUS, AndroidExtraThemeItem.SELECTED_SHADE_COLOR);
      } else if ((pState & nl.adaptivity.diagram.Drawable.STATE_TOUCHED)>0) {
        pPen.setShadowLayer(AndroidExtraThemeItem.SHADER_RADIUS, AndroidExtraThemeItem.TOUCHED_SHADE_COLOR);
      }
    }
    return pPen;
  }

}
