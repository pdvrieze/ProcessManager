package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Theme;
import nl.adaptivity.diagram.ThemeItem;
import android.util.SparseArray;


public class AndroidTheme implements Theme<AndroidStrategy, AndroidPen, AndroidPath> {

  private final AndroidStrategy aStrategy;
  private SparseArray<SparseArray<AndroidPen>> aPens;

  public AndroidTheme(AndroidStrategy pStrategy){
    aStrategy = pStrategy;
    aPens = new SparseArray<>();
  }

  @Override
  public AndroidPen getPen(ThemeItem pItem, int pState) {
    int realState = pItem.getEffectiveState(pState);
    SparseArray<AndroidPen> statePens = aPens.get(pItem.getItemNo());
    if (statePens==null) {
      statePens = new SparseArray<>();
      aPens.append(pItem.getItemNo(), statePens);
    }

    AndroidPen result = statePens.get(realState);
    if (result==null) {
      result = pItem.createPen(aStrategy, realState);
      statePens.append(realState, result);
    }

    return result;
  }

}
