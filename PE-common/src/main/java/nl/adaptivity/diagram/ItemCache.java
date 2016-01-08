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

package nl.adaptivity.diagram;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.List;

/**
 * A cache implementation that allows drawing strategy related items to be stored.
 * This cache assumes that the strategy count is very low (1 or 2) and the item count
 * does not really change (and is low).
 *
 * @author Paul de Vrieze
 *
 */
public class ItemCache {

  @NotNull private DrawingStrategy<?,?,?>[] mStrategies = new DrawingStrategy<?,?,?>[0];
  @NotNull private Pen<?>[][] mPens = new Pen<?>[1][1];
  @NotNull private DiagramPath<?>[][] mPaths = new DiagramPath<?>[1][1];
  @NotNull
  @SuppressWarnings("unchecked")
  private List<DiagramPath<?>>[][] mPathLists = new List[1][1];

  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void setPen(final S strategy, final int index, final PEN_T pen) {
    final int strategyIdx = ensureStrategyIndex(strategy);
    mPens = ensureArrayLength(mPens, strategyIdx+1);
    Pen<?>[] sPens = mPens[strategyIdx];
    if (sPens==null) {
      mPens[strategyIdx] = (sPens = new Pen<?>[index+1]);
    } else {
      mPens[strategyIdx] = sPens = ensureArrayLength(sPens, index+1);
    }
    sPens[index] = pen;
  }

  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void setPath(final S strategy, final int index, final PATH_T path) {
    final int strategyIdx = ensureStrategyIndex(strategy);
    mPaths = ensureArrayLength(mPaths, strategyIdx+1);
    DiagramPath<?>[] sPaths = mPaths[strategyIdx];
    if (sPaths==null) {
      mPaths[strategyIdx] = sPaths = new DiagramPath<?>[index+1];
    } else {
      mPaths[strategyIdx] = sPaths = ensureArrayLength(sPaths, index+1);
    }
    sPaths[index] = path;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void setPathList(final S strategy, final int index, final List<PATH_T> pathList) {
    final int strategyIdx = ensureStrategyIndex(strategy);
    mPathLists = ensureArrayLength(mPathLists, strategyIdx+1);
    List<DiagramPath<?>>[] sPathLists = mPathLists[strategyIdx];
    if (sPathLists==null) {
      mPathLists[strategyIdx] = sPathLists = new List[index+1];
    } else {
      mPathLists[strategyIdx] = sPathLists = ensureArrayLength(sPathLists, index+1);
    }
    sPathLists[index] = (List) pathList;
  }

  @NotNull
  private static <V> V[] ensureArrayLength(@NotNull final V[] array, final int length) {
    final int srcLen = Array.getLength(array);
    if (srcLen<length) {
      @SuppressWarnings("unchecked") final
      V[] newArray = (V[]) Array.newInstance(array.getClass().getComponentType(), length);
      System.arraycopy(array, 0, newArray, 0, srcLen);
      return newArray;
    }
    return array;
  }
//
//  private static <V> V[][] ensureArraysLength(V[][] array, int length) {
//    if (array.length<=length) {
//      @SuppressWarnings("unchecked")
//      V[][] newArray = (V[][]) Array.newInstance(array.getClass(), length, 0);
//      System.arraycopy(array, 0, newArray, 0, array.length);
//      return newArray;
//    }
//    return array;
//  }

  private <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> int getStrategyIndex(final S strategy) {
    int strategyIdx = -1;
    final int strategyLen = mStrategies.length;
    for (int i=0; i<strategyLen; ++i) {
      if (mStrategies[i]==strategy) {
        strategyIdx = i;
        break;
      }
    }
    return strategyIdx;
  }

  private <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> int ensureStrategyIndex(final S strategy) {
    int strategyIdx = getStrategyIndex(strategy);
    if (strategyIdx<0) {
      strategyIdx= mStrategies.length;
      if (mStrategies.length<=strategyIdx) {
        final DrawingStrategy<?,?,?>[] newStrategies = new DrawingStrategy<?,?,?>[strategyIdx+1];
        System.arraycopy(mStrategies, 0, newStrategies, 0, strategyIdx);
        newStrategies[strategyIdx]=strategy;
        mStrategies = newStrategies;
      } else {
        mStrategies[strategyIdx]=strategy;
      }
    }
    return strategyIdx;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> PEN_T getPen(final S strategy, final int index) {
    final int strategyIdx = getStrategyIndex(strategy);
    if(strategyIdx<0 || strategyIdx>= mPens.length || index>= mPens[strategyIdx].length) { return null; }
    return (PEN_T) mPens[strategyIdx][index];
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> PATH_T getPath(final S strategy, final int index) {
    final int strategyIdx = getStrategyIndex(strategy);
    if(strategyIdx<0 || strategyIdx>= mPaths.length || index>= mPaths[strategyIdx].length) { return null; }
    return (PATH_T) mPaths[strategyIdx][index];
  }

  @Nullable
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> List<PATH_T> getPathList(final S strategy, final int index) {
    final int strategyIdx = getStrategyIndex(strategy);
    if(strategyIdx<0 || strategyIdx>= mPathLists.length || index>= mPathLists[strategyIdx].length) { return null; }
    return (List) mPathLists[strategyIdx][index];
  }

  /**
   * Clear all the paths at the given index.
   * @param index The index of the paths to clear
   */
  public void clearPath(final int index) {
    for(final DiagramPath<?>[] lists: mPaths) {
      if (lists!=null && lists.length>index) {
        lists[index] = null;
      }
    }
  }

}
