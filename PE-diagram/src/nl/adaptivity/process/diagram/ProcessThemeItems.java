package nl.adaptivity.process.diagram;

import static nl.adaptivity.diagram.Drawable.*;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.ThemeItem;

public enum ProcessThemeItems implements ThemeItem {
  LINE(DrawableProcessModel.STROKEWIDTH, state(STATE_DEFAULT, 0, 0, 0),
                                         stateStroke(STATE_SELECTED, 0, 0, 255, 255, 2d),
                                         stateStroke(STATE_TOUCHED, 255,255,0, 127, 7d),
                                         state(STATE_CUSTOM1, 0,0,255),
                                         state(STATE_CUSTOM2, 255,255,0),
                                         state(STATE_CUSTOM3, 255, 0, 0),
                                         state(STATE_CUSTOM4, 0,255,0)){

    @Override
    public int getEffectiveState(int pState) {
      if ((pState&STATE_TOUCHED)!=0) { return STATE_TOUCHED; }
      int result = effectiveStateHelper(pState);
      if (result>=0) { return result; }
      return super.getEffectiveState(pState);
    }

  },
  BACKGROUND(state(STATE_DEFAULT, 255, 255, 255)) {

    @Override
    public int getEffectiveState(int pState) {
      return STATE_DEFAULT;
    }

  },
  ENDNODEOUTERLINE(DrawableProcessModel.ENDNODEOUTERSTROKEWIDTH, LINE),
  LINEBG(LINE),
  ;

  private StateSpecifier[] aSpecifiers;
  private boolean aFill;
  private ProcessThemeItems aParent;
  private double aStroke;

  private ProcessThemeItems(double stroke, ProcessThemeItems pParent) {
    aFill = false;
    aStroke = stroke;
    aParent = pParent;
  }

  private ProcessThemeItems(ProcessThemeItems pParent) {
    aFill = true;
    aParent = pParent;
  }

  private ProcessThemeItems(double stroke, StateSpecifier... pSpecifiers) {
    aSpecifiers = pSpecifiers;
    aFill = false;
    aStroke = stroke;
  }

  private ProcessThemeItems(StateSpecifier... pSpecifiers) {
    aSpecifiers = pSpecifiers;
    aFill = true;
  }

  private static StateSpecifier stateStroke(int pState, int r, int g, int b, int a, double strokeMultiplier) {
    return new StrokeStateSpecifier(pState, r, g, b, a, strokeMultiplier);
  }

  private static StateSpecifier state(int pState, int r, int g, int b) {
    return new StateSpecifier(pState, r, g, b, 255);
  }

  private static StateSpecifier state(int pState, int r, int g, int b, int a) {
    return new StateSpecifier(pState, r, g, b, a);
  }

  @Override
  public int getItemNo() {
    return ordinal();
  }

  @Override
  public int getEffectiveState(int pState) {
    if (aParent!=null) { return aParent.getEffectiveState(pState); }
    final int result = effectiveStateHelper(pState);
    return result>=0 ? result : pState;
  }

  int effectiveStateHelper(int pState) {
    if ((pState&STATE_CUSTOM1)!=0) { return STATE_CUSTOM1; }
    if ((pState&STATE_CUSTOM2)!=0) { return STATE_CUSTOM2; }
    if ((pState&STATE_CUSTOM3)!=0) { return STATE_CUSTOM3; }
    if ((pState&STATE_CUSTOM4)!=0) { return STATE_CUSTOM4; }
    return -1;
  }

  @Override
  public <PEN_T extends Pen<PEN_T>> PEN_T createPen(DrawingStrategy<?, PEN_T, ?> pStrategy, int pState) {
    StateSpecifier specifier = getSpecifier(pState);
    PEN_T result;
    result = pStrategy.newPen().setColor(specifier.aRed, specifier.aGreen, specifier.aBlue, specifier.aAlpha);
    if (! aFill) {
      if (aParent!=null) {
        result.setStrokeWidth((aStroke<=0d ? aParent.aStroke : aStroke) * specifier.getStrokeMultiplier());
      } else {
        result.setStrokeWidth(aStroke * specifier.getStrokeMultiplier());
      }
    }
    return result;
  }


  private StateSpecifier getSpecifier(int pState) {
    if (aParent!=null) { return aParent.getSpecifier(pState); }
    for(StateSpecifier candidate: aSpecifiers) {
      if (candidate.aState==pState) {
        return candidate;
      }
    }
    StateSpecifier bestCandidate = aSpecifiers[0];
    for(StateSpecifier candidate: aSpecifiers) {
      if ((candidate.aState&pState)==candidate.aState && candidate.aState>bestCandidate.aState) {
        bestCandidate = candidate;
      }
    }
    return bestCandidate;
  }


  private static class StateSpecifier {

    private int aState;
    private int aRed;
    private int aGreen;
    private int aBlue;
    private int aAlpha;

    public StateSpecifier(int pState, int pR, int pG, int pB, int pA) {
      aState = pState;
      aRed = pR;
      aGreen = pG;
      aBlue = pB;
      aAlpha = pA;
    }

    public double getStrokeMultiplier() {
      return 1d;
    }

  }

  private static class StrokeStateSpecifier extends StateSpecifier {

    private final double aStrokeMultiplier;

    public StrokeStateSpecifier(int pState, int pR, int pG, int pB, int pA, double pStrokeMultiplier) {
      super(pState, pR, pG, pB, pA);
      aStrokeMultiplier = pStrokeMultiplier;
    }

    public double getStrokeMultiplier() {
      return aStrokeMultiplier;
    }

  }

}
