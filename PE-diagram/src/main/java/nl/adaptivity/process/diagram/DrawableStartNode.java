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

package nl.adaptivity.process.diagram;
import nl.adaptivity.diagram.*;
import nl.adaptivity.process.clientProcessModel.ClientStartNode;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.StartNode;
import nl.adaptivity.process.util.Identified;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static nl.adaptivity.process.diagram.RootDrawableProcessModel.*;



public class DrawableStartNode extends ClientStartNode<DrawableProcessNode, DrawableProcessModel> implements DrawableProcessNode{

  public static class Builder extends ClientStartNode.Builder<DrawableProcessNode, DrawableProcessModel> implements DrawableProcessNode.Builder {

    public Builder() { }

    public Builder(final boolean compat) {
      super(compat);
    }

    public Builder(@Nullable final Identified successor, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results) {
      super(successor, id, label, x, y, defines, results);
    }

    public Builder(@NotNull final StartNode<?, ?> node) {
      super(node);
    }

    @NotNull
    @Override
    public DrawableStartNode build(@NotNull final DrawableProcessModel newOwner) {
      return new DrawableStartNode(this, newOwner);
    }
  }

  private static final double REFERENCE_OFFSET_X = STARTNODERADIUS + (STROKEWIDTH / 2);
  private static final double REFERENCE_OFFSET_Y = STARTNODERADIUS + (STROKEWIDTH / 2);
  public static final String IDBASE = "start";
  private int mState = STATE_DEFAULT;

  public DrawableStartNode(final DrawableProcessModel ownerModel) {
    this(ownerModel, false);
  }

  public DrawableStartNode(final DrawableProcessModel ownerModel, final boolean compat) {
    super(ownerModel, compat);
  }

  public DrawableStartNode(final DrawableProcessModel ownerModel, String id, final boolean compat) {
    super(ownerModel, id, compat);
  }

  public DrawableStartNode(DrawableStartNode orig, final boolean compat) {
    super(orig, compat);
    mState = orig.mState;
  }

  public DrawableStartNode(@NotNull final StartNode.Builder<?, ?> builder, @Nullable final DrawableProcessModel newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  @Override
  public Builder builder() {
    return new Builder(this);
  }

  @Override
  public DrawableStartNode clone() {
    if (getClass()==DrawableStartNode.class) {
      return new DrawableStartNode(this, isCompat());
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @NotNull
  public static Builder deserialize(@NotNull final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new Builder(true), in);
  }

  @Deprecated
  @NotNull
  public static DrawableStartNode deserialize(final DrawableProcessModel ownerModel, @NotNull final XmlReader in) throws XmlException {
    return XmlUtil.<DrawableStartNode.Builder>deserializeHelper(new DrawableStartNode.Builder(true), in).build(ownerModel);
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-REFERENCE_OFFSET_X, getY()-REFERENCE_OFFSET_Y, STARTNODERADIUS * 2 + STROKEWIDTH, STARTNODERADIUS * 2 + STROKEWIDTH);
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
  public boolean isWithinBounds(final double x, final double y) {
    final double realradius= STARTNODERADIUS + (STROKEWIDTH / 2);
    return ((Math.abs(x-getX())<=realradius) && (Math.abs(y-getY())<=realradius));
  }

  @Override
  public Drawable getItemAt(double x, double y) {
    return isWithinBounds(x, y) ? this : null;
  }

  @Override
  public int getState() {
    return mState ;
  }

  @Override
  public void setState(int state) {
    mState = state;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
    if (hasPos()) {
      final double realradius= STARTNODERADIUS + (STROKEWIDTH / 2);
      PEN_T fillPen = canvas.getTheme().getPen(ProcessThemeItems.LINEBG, mState & ~STATE_TOUCHED);

      if ((mState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = canvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        canvas.drawCircle(realradius, realradius, STARTNODERADIUS, touchedPen);
      }

      canvas.drawFilledCircle(realradius, realradius, realradius, fillPen);
    }
  }

  @Override
  public String getIdBase() {
    return IDBASE;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void drawLabel(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds, double left, double top) {
    DrawableUtil.defaultDrawLabel(this, canvas, clipBounds, left, top);
  }

  public static DrawableStartNode from(StartNode<?, ?> n, final boolean compat) {
    DrawableStartNode result = new DrawableStartNode((DrawableProcessModel) null, compat);
    copyProcessNodeAttrs(n, result);
    result.setDefines(n.getDefines());
    return result;
  }

}
