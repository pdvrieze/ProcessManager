package nl.adaptivity.diagram;

import java.lang.reflect.Array;

/**
 * A cache implementation that allows drawing strategy related items to be stored.
 * This cache assumes that the strategy count is very low (1 or 2) and the item count
 * does not really change (and is low).
 *
 * @author Paul de Vrieze
 *
 */
public class ItemCache {

  private DrawingStrategy<?,?,?>[] aStrategies = new DrawingStrategy<?,?,?>[0];
  private Pen<?>[][] aPens = new Pen<?>[1][1];
  private DiagramPath<?>[][] aPaths = new DiagramPath<?>[1][1];

  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void setPen(S pStrategy, int pIndex, PEN_T pPen) {
    int strategyIdx = ensureStrategyIndex(pStrategy);
    aPens = ensureArrayLength(aPens, strategyIdx+1);
    Pen<?>[] sPens = aPens[strategyIdx];
    if (sPens==null) {
      sPens = new Pen<?>[pIndex+1];
    } else {
      sPens = ensureArrayLength(sPens, pIndex+1);
    }
    sPens[pIndex] = pPen;
  }

  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void setPath(S pStrategy, int pIndex, PATH_T pPath) {
    int strategyIdx = ensureStrategyIndex(pStrategy);
    aPaths = ensureArrayLength(aPaths, strategyIdx+1);
    DiagramPath<?>[] sPaths = aPaths[strategyIdx];
    if (sPaths==null) {
      sPaths = new DiagramPath<?>[pIndex+1];
    } else {
      sPaths = ensureArrayLength(sPaths, pIndex+1);
    }
    sPaths[pIndex] = pPath;
  }

  private static <V> V[] ensureArrayLength(V[] array, int length) {
    final int srcLen = Array.getLength(array);
    if (srcLen<length) {
      @SuppressWarnings("unchecked")
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

  private final <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> int getStrategyIndex(S pStrategy) {
    int strategyIdx = -1;
    int strategyLen = aStrategies.length;
    for (int i=0; i<strategyLen; ++i) {
      if (aStrategies[i]==pStrategy) {
        strategyIdx = i;
        break;
      }
    }
    return strategyIdx;
  }

  private <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> int ensureStrategyIndex(S pStrategy) {
    int strategyIdx = getStrategyIndex(pStrategy);
    if (strategyIdx<0) {
      strategyIdx=aStrategies.length;
      if (aStrategies.length<=strategyIdx) {
        DrawingStrategy<?,?,?>[] newStrategies = new DrawingStrategy<?,?,?>[strategyIdx+1];
        System.arraycopy(aStrategies, 0, newStrategies, 0, strategyIdx);
        newStrategies[strategyIdx]=pStrategy;
        aStrategies = newStrategies;
      } else {
        aStrategies[strategyIdx]=pStrategy;
      }
    }
    return strategyIdx;
  }

  @SuppressWarnings("unchecked")
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> PEN_T getPen(S pStrategy, int pIndex) {
    int strategyIdx = getStrategyIndex(pStrategy);
    if(strategyIdx<0 || strategyIdx>=aPens.length || pIndex>=aPens[strategyIdx].length) { return null; }
    return (PEN_T) aPens[strategyIdx][pIndex];
  }

  @SuppressWarnings("unchecked")
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> PATH_T getPath(S pStrategy, int pIndex) {
    int strategyIdx = getStrategyIndex(pStrategy);
    if(strategyIdx<0 || strategyIdx>=aPaths.length || pIndex>=aPaths[strategyIdx].length) { return null; }
    return (PATH_T) aPaths[strategyIdx][pIndex];
  }

  /**
   * Clear all the paths at the given index.
   * @param pIndex The index of the paths to clear
   */
  public void clearPath(int pIndex) {
    for(DiagramPath<?>[] lists:aPaths) {
      if (lists!=null && lists.length>pIndex) {
        lists[pIndex] = null;
      }
    }
  }

}
