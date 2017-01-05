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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.*;
import nl.adaptivity.process.clientProcessModel.ClientSplitNode;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.ModelCommon;
import nl.adaptivity.process.processModel.Split;
import nl.adaptivity.process.util.Identified;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static nl.adaptivity.process.diagram.RootDrawableProcessModel.*;


public class DrawableSplit extends ClientSplitNode<DrawableProcessNode, DrawableProcessModel> implements Split<DrawableProcessNode, DrawableProcessModel>, DrawableJoinSplit {
 
  public static class Builder extends ClientSplitNode.Builder<DrawableProcessNode, DrawableProcessModel> implements DrawableJoinSplit.Builder {

    public Builder() { }

    public Builder(@NotNull final Collection<? extends Identified> predecessors, @NotNull final Collection<? extends Identified> successors, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, final int min, final int max) {
      super(predecessors, successors, id, label, x, y, defines, results, min, max);
    }

    public Builder(@NotNull final Split<?, ?> node) {
      super(node);
    }

    @NotNull
    @Override
    public DrawableSplit build(@NotNull final DrawableProcessModel newOwner) {
      return new DrawableSplit(this, newOwner);
    }

  }

  private static final double ARROWHEADDX = JOINWIDTH*0.2;
  private static final double ARROWHEADDY = JOINWIDTH*0.2;
  private static final double ARROWHEADADJUST = (0.5*STROKEWIDTH)*Math.sqrt(0.5/(Math.sin(ARROWHEADANGLE)* Math.sin(ARROWHEADANGLE)));
  /** The y coordinate if the line were horizontal. */
  private static final double ARROWDFAR = ARROWLEN*Math.sin(0.25*Math.PI-ARROWHEADANGLE);
  /** The x coordinate if the line were horizontal. */
  private static final double ARROWDNEAR = ARROWLEN*Math.cos(0.25*Math.PI-ARROWHEADANGLE);
  private static final double INLEN = Math.sqrt(ARROWHEADDX*ARROWHEADDX+ARROWHEADDY*ARROWHEADDY);
  public static final String IDBASE = "split";
  private final DrawableJoinSplitDelegate mDrawableJoinSplitDelegate;


  public DrawableSplit(final DrawableProcessModel ownerModel) {
    super(new Builder(), ownerModel);
    mDrawableJoinSplitDelegate = new DrawableJoinSplitDelegate();
  }

  public DrawableSplit(Split<?,?> orig) {
    super(orig.builder(), null);
    if (orig instanceof DrawableSplit) {
      mDrawableJoinSplitDelegate = new DrawableJoinSplitDelegate(((DrawableSplit) orig).mDrawableJoinSplitDelegate);
    } else {
      mDrawableJoinSplitDelegate = new DrawableJoinSplitDelegate();
    }
  }

  public DrawableSplit(@NotNull final Split.Builder<?, ?> builder, @NotNull final DrawableProcessModel newOwnerModel) {
    super(builder, newOwnerModel);
    mDrawableJoinSplitDelegate = new DrawableJoinSplitDelegate();
  }

  @NotNull
  @Override
  public Builder builder() {
    return new Builder(this);
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public DrawableSplit clone() {
    if (getClass()==DrawableSplit.class) {
      return new DrawableSplit(this);
    }
    throw new RuntimeException(new CloneNotSupportedException());
  }

  @Deprecated
  @NotNull
  public static DrawableSplit deserialize(final DrawableProcessModel ownerModel, @NotNull final XmlReader in) throws XmlException {
    return XmlUtil.<DrawableSplit.Builder>deserializeHelper(new DrawableSplit.Builder(), in).build(ownerModel);
  }

  @NotNull
  public static Builder deserialize(@NotNull final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new Builder(), in);
  }

  @Override
  public boolean isCompat() {
    return false;
  }

  public static DrawableSplit from(Split<?, ?> elem) {
    return new DrawableSplit(elem);
  }

  @Override
  public int getMaxSuccessorCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public String getIdBase() {
    return IDBASE;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds) {
    mDrawableJoinSplitDelegate.draw(this, canvas, clipBounds);

    final S strategy = canvas.getStrategy();
    PATH_T path = mDrawableJoinSplitDelegate.mItems.getPath(strategy, 1);
    if (path==null) {
      path = strategy.newPath();
      if (CURVED_ARROWS) {
        path.moveTo(CENTERX-HORIZONTALDECORATIONLEN,CENTERY)
            .lineTo(CENTERX-INLEN, CENTERY)
            .cubicTo(CENTERX-INLEN*(1-ARROWCONTROLRATIO), CENTERY, CENTERX+ARROWHEADDX*(1-ARROWCONTROLRATIO)-ARROWHEADADJUST, CENTERY-ARROWHEADDY*(1-ARROWCONTROLRATIO)+ARROWHEADADJUST, CENTERX+ARROWHEADDX-ARROWHEADADJUST,CENTERY-ARROWHEADDY+ARROWHEADADJUST)
            .moveTo(CENTERX-INLEN, CENTERY)
            .cubicTo(CENTERX-INLEN*(1-ARROWCONTROLRATIO), CENTERY, CENTERX+ARROWHEADDX*(1-ARROWCONTROLRATIO)-ARROWHEADADJUST, CENTERY+ARROWHEADDY*(1-ARROWCONTROLRATIO)-ARROWHEADADJUST, CENTERX+ARROWHEADDX-ARROWHEADADJUST,CENTERY+ARROWHEADDY-ARROWHEADADJUST);
      } else {
        path.moveTo(CENTERX-HORIZONTALDECORATIONLEN,CENTERY)
            .lineTo(CENTERX, CENTERY)
            .moveTo(CENTERX+ARROWHEADDX-ARROWHEADADJUST,CENTERY-ARROWHEADDY+ARROWHEADADJUST)
            .lineTo(CENTERX, CENTERY)
            .lineTo(CENTERX+ARROWHEADDX-ARROWHEADADJUST,CENTERY+ARROWHEADDY-ARROWHEADADJUST);
      }

      path.moveTo(CENTERX+ARROWHEADDX-ARROWDNEAR,CENTERY-ARROWHEADDY+ARROWDFAR)
          .lineTo(CENTERX+ARROWHEADDX,CENTERY-ARROWHEADDY)
          .lineTo(CENTERX+ARROWHEADDX-ARROWDFAR,CENTERY-ARROWHEADDY+ARROWDNEAR)
          .moveTo(CENTERX+ARROWHEADDX-ARROWDFAR,CENTERY+ARROWHEADDY-ARROWDNEAR)
          .lineTo(CENTERX+ARROWHEADDX,CENTERY+ARROWHEADDY)
          .lineTo(CENTERX+ARROWHEADDX-ARROWDNEAR,CENTERY+ARROWHEADDY-ARROWDFAR);

      mDrawableJoinSplitDelegate.mItems.setPath(strategy, 1, path);
    }
    if (hasPos()) {
      PEN_T linePen = canvas.getTheme().getPen(ProcessThemeItems.INNERLINE, mDrawableJoinSplitDelegate.getState() & ~STATE_TOUCHED);
      canvas.drawPath(path, linePen, null);
    }
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void drawLabel(Canvas<S, PEN_T, PATH_T> canvas, Rectangle clipBounds, double left, double top) {
    DrawableUtil.defaultDrawLabel(this, canvas, clipBounds, left, top);
  }

  @Override
  public void setPos(final double left, final double top) {
    setX(left);
    setY(top);
  }

  @Override
  public Rectangle getBounds() {
    return DrawableJoinSplitDelegate.getBounds(this);
  }

  @Override
  public Drawable getItemAt(double x, double y) {
    return DrawableJoinSplitDelegate.getItemAt(this, x, y);
  }

  @Override
  public int getState() {
    return mDrawableJoinSplitDelegate.getState();
  }

  @Override
  public void setState(int state) {
    mDrawableJoinSplitDelegate.setState(state);
  }

  public String getMinMaxText() {
    return DrawableJoinSplitDelegate.getMinMaxText(this);
  }

  @Override
  public boolean isOr() {
    return DrawableJoinSplitDelegate.isOr(this);
  }

  @Override
  public boolean isXor() {
    return DrawableJoinSplitDelegate.isXor(this);
  }

  @Override
  public boolean isAnd() {
    return DrawableJoinSplitDelegate.isAnd(this);
  }

}
