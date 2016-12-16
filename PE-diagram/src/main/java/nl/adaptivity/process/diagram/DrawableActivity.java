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
import nl.adaptivity.process.ProcessConsts.Endpoints;
import nl.adaptivity.process.clientProcessModel.ClientActivityNode;
import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.util.Identified;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static nl.adaptivity.process.diagram.DrawableProcessModel.*;



public class DrawableActivity extends ClientActivityNode<DrawableProcessNode, DrawableProcessModel> implements DrawableProcessNode {

  public static class Builder extends ClientActivityNode.Builder<DrawableProcessNode, DrawableProcessModel> implements DrawableProcessNode.Builder {

    public Builder() { }

    public Builder(final boolean compat) { super(compat); }

    public Builder(@Nullable final Identified predecessor, @Nullable final Identified successor, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, @Nullable final XmlMessage message, @Nullable final String condition, @Nullable final String name, final boolean compat) {
      super(predecessor, successor, id, label, x, y, defines, results, message, condition, name, compat);
    }

    public Builder(@NotNull final Activity<?, ?> node) {
      super(node);
    }

    @NotNull
    @Override
    public DrawableActivity build(@Nullable final DrawableProcessModel newOwner) {
      return new DrawableActivity(this, newOwner);
    }
  }

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

  public DrawableActivity(@NotNull final Activity.Builder<?, ?> builder, @NotNull final DrawableProcessModel newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  @Override
  public Builder builder() {
    return new Builder(this);
  }

  @Override
  public DrawableActivity clone() {
    if (getClass()==DrawableActivity.class) {
      return new DrawableActivity(this, isCompat());
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Deprecated @NotNull
  public static DrawableActivity deserialize(final DrawableProcessModel ownerModel, @NotNull final XmlReader in) throws XmlException {
    return XmlUtil.<nl.adaptivity.process.diagram.DrawableActivity>deserializeHelper(new DrawableActivity(ownerModel, true), in);
  }

  @NotNull
  public static Builder deserialize(@NotNull final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new DrawableActivity.Builder(true), in);
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
      String label = getDrawnLabel(textPen);
      if (label != null && label.length()>0) {
        double topCenter = ACTIVITYHEIGHT + STROKEWIDTH + textPen.getTextLeading() / 2;
        canvas.drawText(TextPos.ASCENT, REFERENCE_OFFSET_X, topCenter, label, Double.MAX_VALUE, textPen);
      }
    }
  }

  /**
   * Get the label that would be drawn to the screen. This will set the pen to italics or not unless no label could be determined.
   * @param textPen The textPen to set to italics (or not).
   * @param <PEN_T>
   * @return The actual label.
   */
  @Nullable
  private <PEN_T extends Pen<PEN_T>> String getDrawnLabel(@NotNull final PEN_T textPen) {
    String label = getLabel();
    if (label == null) { label = getName(); }
    if (label == null && getOwnerModel() != null) {
      label = '<' + getId() + '>';
      textPen.setTextItalics(true);
    } else if (label != null) {
      textPen.setTextItalics(false);
    }
    return label;
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

  public boolean isBodySpecified() {
    return getMessage()!=null;
  }

  public boolean isUserTask() {
    XmlMessage message = XmlMessage.get(getMessage());
    return message != null && Endpoints.USER_TASK_SERVICE_DESCRIPTOR.isSameService(message.getEndpointDescriptor());
  }

  public boolean isService() {
    return isBodySpecified() && ! isUserTask();
  }

}
