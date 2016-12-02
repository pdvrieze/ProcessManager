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


import nl.adaptivity.process.clientProcessModel.ClientJoinSplit;
import org.jetbrains.annotations.NotNull;

import static nl.adaptivity.process.diagram.DrawableProcessModel.*;


public interface DrawableJoinSplit extends ClientJoinSplit<DrawableProcessNode, DrawableProcessModel>, DrawableProcessNode{

  interface Builder extends DrawableProcessNode.Builder, ClientJoinSplit.Builder<DrawableProcessNode, DrawableProcessModel> {

    @NotNull
    @Override
    DrawableJoinSplit build(@NotNull DrawableProcessModel newOwner);
  }

  @NotNull
  @Override
  Builder builder();

  boolean CURVED_ARROWS=true;
  boolean TEXT_DESC=true;

  double STROKEEXTEND = Math.sqrt(2)*STROKEWIDTH;
  double REFERENCE_OFFSET_X = (JOINWIDTH + STROKEEXTEND) / 2;
  double REFERENCE_OFFSET_Y = (JOINHEIGHT + STROKEEXTEND) / 2;
  double HORIZONTALDECORATIONLEN = JOINWIDTH*0.4;
  double CENTERX = (JOINWIDTH+STROKEEXTEND)/2;
  double CENTERY = (JOINHEIGHT+STROKEEXTEND)/2;
  double ARROWHEADANGLE = (35*Math.PI)/180;
  double ARROWLEN = JOINWIDTH*0.15;
  double ARROWCONTROLRATIO=0.85;

  /** Determine whether the node represents an or split. */
  boolean isOr();

  /** Determine whether the node represents an xor split. */
  boolean isXor();

  /** Determine whether the node represents an and split. */
  boolean isAnd();

}