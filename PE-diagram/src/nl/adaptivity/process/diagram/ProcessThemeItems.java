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
    public int getEffectiveState(int state) {
      if ((state&STATE_TOUCHED)!=0) { return STATE_TOUCHED; }
      int result = effectiveStateHelper(state);
      if (result>=0) { return result; }
      return super.getEffectiveState(state);
    }

  },
  INNERLINE(DrawableProcessModel.STROKEWIDTH*0.85, state(STATE_DEFAULT, 0, 0, 0x20, 0xb0)),
  BACKGROUND(state(STATE_DEFAULT, 255, 255, 255)) {

    @Override
    public int getEffectiveState(int state) {
      return STATE_DEFAULT;
    }

  },
  ENDNODEOUTERLINE(DrawableProcessModel.ENDNODEOUTERSTROKEWIDTH, LINE),
  LINEBG(LINE),
  DIAGRAMTEXT(DrawableProcessModel.STROKEWIDTH, DrawableProcessModel.DIAGRAMTEXT_SIZE, state(STATE_DEFAULT, 0, 0, 0)),
  DIAGRAMLABEL(DrawableProcessModel.STROKEWIDTH, DrawableProcessModel.DIAGRAMLABEL_SIZE, state(STATE_DEFAULT, 0, 0, 0)),
  ;

  private StateSpecifier[] mSpecifiers;
  private boolean mFill;
  private ProcessThemeItems mParent;
  private double mStroke;
  private double mFontSize=Double.NaN;

  private ProcessThemeItems(double stroke, ProcessThemeItems parent) {
    mFill = false;
    mStroke = stroke;
    mParent = parent;
  }

  private ProcessThemeItems(ProcessThemeItems parent) {
    mFill = true;
    mParent = parent;
  }

  private ProcessThemeItems(double stroke, StateSpecifier... specifiers) {
    mSpecifiers = specifiers;
    mFill = false;
    mStroke = stroke;
  }

  private ProcessThemeItems(double stroke, double fontSize, StateSpecifier... specifiers) {
    mSpecifiers = specifiers;
    mFill = true;
    mStroke = stroke;
    mFontSize = fontSize;
  }

  private ProcessThemeItems(StateSpecifier... specifiers) {
    mSpecifiers = specifiers;
    mFill = true;
  }

  private static StateSpecifier stateStroke(int state, int r, int g, int b, int a, double strokeMultiplier) {
    return new StrokeStateSpecifier(state, r, g, b, a, strokeMultiplier);
  }

  private static StateSpecifier state(int state, int r, int g, int b) {
    return new StateSpecifier(state, r, g, b, 255);
  }

  // This method can be useful when colors with alpha are desired.
  private static StateSpecifier state(int state, int r, int g, int b, int a) {
    return new StateSpecifier(state, r, g, b, a);
  }

  @Override
  public int getItemNo() {
    return ordinal();
  }

  @Override
  public int getEffectiveState(int state) {
    if (mParent!=null) { return mParent.getEffectiveState(state); }
    final int result = effectiveStateHelper(state);
    return result>=0 ? result : state;
  }

  int effectiveStateHelper(int state) {
    if ((state&STATE_CUSTOM1)!=0) { return STATE_CUSTOM1; }
    if ((state&STATE_CUSTOM2)!=0) { return STATE_CUSTOM2; }
    if ((state&STATE_CUSTOM3)!=0) { return STATE_CUSTOM3; }
    if ((state&STATE_CUSTOM4)!=0) { return STATE_CUSTOM4; }
    return -1;
  }

  @Override
  public <PEN_T extends Pen<PEN_T>> PEN_T createPen(DrawingStrategy<?, PEN_T, ?> strategy, int state) {
    StateSpecifier specifier = getSpecifier(state);
    PEN_T result;
    result = strategy.newPen().setColor(specifier.mRed, specifier.mGreen, specifier.mBlue, specifier.mAlpha);
    if (! mFill) {
      if (mParent!=null) {
        result.setStrokeWidth((mStroke<=0d ? mParent.mStroke : mStroke) * specifier.getStrokeMultiplier());
      } else {
        result.setStrokeWidth(mStroke * specifier.getStrokeMultiplier());
      }
    }
    if (! Double.isNaN(mFontSize)) {
      result.setFontSize(mFontSize);
    }
    return result;
  }


  private StateSpecifier getSpecifier(int state) {
    if (mParent!=null) { return mParent.getSpecifier(state); }
    for(StateSpecifier candidate: mSpecifiers) {
      if (candidate.mState==state) {
        return candidate;
      }
    }
    StateSpecifier bestCandidate = mSpecifiers[0];
    for(StateSpecifier candidate: mSpecifiers) {
      if ((candidate.mState&state)==candidate.mState && candidate.mState>bestCandidate.mState) {
        bestCandidate = candidate;
      }
    }
    return bestCandidate;
  }


  private static class StateSpecifier {

    private int mState;
    private int mRed;
    private int mGreen;
    private int mBlue;
    private int mAlpha;

    public StateSpecifier(int state, int r, int g, int b, int a) {
      mState = state;
      mRed = r;
      mGreen = g;
      mBlue = b;
      mAlpha = a;
    }

    public double getStrokeMultiplier() {
      return 1d;
    }

  }

  private static class StrokeStateSpecifier extends StateSpecifier {

    private final double mStrokeMultiplier;

    public StrokeStateSpecifier(int state, int r, int g, int b, int a, double strokeMultiplier) {
      super(state, r, g, b, a);
      mStrokeMultiplier = strokeMultiplier;
    }

    @Override
    public double getStrokeMultiplier() {
      return mStrokeMultiplier;
    }

  }

}
