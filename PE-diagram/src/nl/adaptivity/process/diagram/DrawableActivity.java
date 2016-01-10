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

package nl.adaptivity.process.diagram;
import nl.adaptivity.diagram.*;
import nl.adaptivity.diagram.Canvas.TextPos;
import nl.adaptivity.process.clientProcessModel.ClientActivityNode;
import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import org.jetbrains.annotations.NotNull;

import static nl.adaptivity.process.diagram.DrawableProcessModel.*;



public class DrawableActivity extends ClientActivityNode<DrawableProcessNode, DrawableProcessModel> implements DrawableProcessNode {

  private static final double REFERENCE_OFFSET_X = (ACTIVITYWIDTH+STROKEWIDTH)/2;
  private static final double REFERENCE_OFFSET_Y = (ACTIVITYHEIGHT+STROKEWIDTH)/2;
  public static final String IDBASE = "ac";
  private int mState = STATE_DEFAULT;
  private static Rectangle _bounds;

  public DrawableActivity(final DrawableProcessModel owner) {
    this(owner, false);
  }

  public DrawableActivity(final DrawableProcessModel owner, final boolean compat) {
    super(owner, compat);
  }

  public DrawableActivity(final DrawableProcessModel owner, String id, final boolean compat) {
    super(owner, id, compat);
  }

  public DrawableActivity(Activity<?, ?> orig, final boolean compat) {
    super(orig, compat);
    if (orig instanceof DrawableActivity) {
      mState = ((DrawableActivity) orig).mState;
    }
  }

  @Override
  public DrawableActivity clone() {
    if (getClass()==DrawableActivity.class) {
      return new DrawableActivity(this, isCompat());
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @NotNull
  public static DrawableActivity deserialize(final DrawableProcessModel ownerModel, @NotNull final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new DrawableActivity(ownerModel, true), in);
  }

  @Override
  public Rectangle getBounds() {

    return new Rectangle(getX()-REFERENCE_OFFSET_X, getY()-REFERENCE_OFFSET_Y, ACTIVITYWIDTH + STROKEWIDTH, ACTIVITYHEIGHT + STROKEWIDTH);
  }

  @Override
  public void translate(double dX, double dY) {
    setX(getX() + dX);
    setY(getY() + dY);
  }

  @Override
  public void setPos(double left, double top) {
    setX(left+REFERENCE_OFFSET_X);
    setY(left+REFERENCE_OFFSET_Y);
  }

  @Override
  public Drawable getItemAt(double x, double y) {
    double hwidth = (ACTIVITYWIDTH+STROKEWIDTH)/2;
    double hheight = (ACTIVITYHEIGHT+STROKEWIDTH)/2;
    return ((Math.abs(x-getX())<=hwidth) && (Math.abs(y-getY())<=hheight)) ? this : null;
  }

  @Override
  public int getState() {
    return mState;
  }

  @Override
  public void setState(int state) {
    if (state==mState) { return ; }
    mState = state;
    if (getOwnerModel() != null) {
      getOwnerModel().notifyNodeChanged(this);
    }
  }

  @Override
  public String getIdBase() {
    return IDBASE;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
    if (hasPos()) {
      PEN_T linePen = canvas.getTheme().getPen(ProcessThemeItems.LINE, mState & ~STATE_TOUCHED);
      PEN_T bgPen = canvas.getTheme().getPen(ProcessThemeItems.BACKGROUND, mState);

      if (_bounds==null) { _bounds = new Rectangle(STROKEWIDTH/2,STROKEWIDTH/2, ACTIVITYWIDTH, ACTIVITYHEIGHT); }

      if ((mState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = canvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        canvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, touchedPen);
      }
      canvas.drawFilledRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, bgPen);
      canvas.drawRoundRect(_bounds, ACTIVITYROUNDX, ACTIVITYROUNDY, linePen);
    }
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void drawLabel(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds, double left, double top) {
    if (hasPos()) {
      PEN_T textPen = canvas.getTheme().getPen(ProcessThemeItems.DIAGRAMLABEL, mState);
      String label = getLabel();
      if (label == null) { label = getName(); }
      if (label == null && getOwnerModel() != null) {
        label = '<' + getId() + '>';
        textPen.setTextItalics(true);
      } else if (label != null) {
        textPen.setTextItalics(false);
      }
      if (label != null) {
        double topCenter = ACTIVITYHEIGHT + STROKEWIDTH + textPen.getTextLeading() / 2;
        canvas.drawText(TextPos.ASCENT, REFERENCE_OFFSET_X, topCenter, label, Double.MAX_VALUE, textPen);
      }
    }
  }

  public static DrawableActivity from(Activity<?, ?> elem, final boolean compat) {
    DrawableActivity result = new DrawableActivity(elem, compat);
    copyProcessNodeAttrs(elem, result);
    result.setName(elem.getName());
    result.setLabel(elem.getLabel());
    result.setCondition(elem.getCondition());
    result.setDefines(elem.getDefines());
    result.setResults(elem.getResults());
    result.setMessage(elem.getMessage());
    return result;
  }



}
