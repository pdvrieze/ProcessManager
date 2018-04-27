/*
 * Copyright (c) 2018.
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

package nl.adaptivity.diagram

/**
 * A cache implementation that allows drawing strategy related items to be stored.
 * This cache assumes that the strategy count is very low (1 or 2) and the item count
 * does not really change (and is low).

 * @author Paul de Vrieze
 */
class ItemCache {

  private var strategies = arrayOfNulls<DrawingStrategy<*, *, *>>(0)
  private var pens = arrayOf<Array<Pen<*>?>?>(arrayOfNulls<Pen<*>>(1))
  private var paths = arrayOf<Array<DiagramPath<*>?>?>(arrayOfNulls<DiagramPath<*>>(1))
  private var pathLists = arrayOf<Array<List<*>?>?>(arrayOfNulls<List<*>>(1))

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> setPen(strategy: S, index: Int, pen: PEN_T) {
    setPen(ensureStrategyIndex(strategy), index, pen)
  }

  private fun <PEN_T : Pen<PEN_T>> setPen(strategyIdx: Int, penIdx: Int, pen: PEN_T) {
    pens = pens.ensureArrayLength(strategyIdx + 1)

    val strategyPens = pens[strategyIdx].ensureArrayLength(penIdx + 1)

    strategyPens[penIdx] = pen
    pens[strategyIdx] = strategyPens
  }

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> setPath(strategy: S, index: Int, path: PATH_T) {
    setPath(ensureStrategyIndex(strategy), index, path)
  }

  private fun <PATH_T : DiagramPath<PATH_T>> setPath(strategyIdx: Int, pathIdx: Int, path: PATH_T) {
    if (strategyIdx<0) {
      setPath(strategies.size, pathIdx, path)
    } else {
      paths = paths.ensureArrayLength(strategyIdx + 1)
      val sPaths = paths[strategyIdx].ensureArrayLength(pathIdx + 1)

      sPaths[pathIdx] = path
      paths[strategyIdx] = sPaths
    }
  }

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> setPathList(strategy: S, index: Int, pathList: List<PATH_T>) {
    setPathList(ensureStrategyIndex(strategy), index, pathList)
  }

  private fun <PATH_T : DiagramPath<PATH_T>> setPathList(strategyIdx: Int, pathListIdx: Int, pathList: List<PATH_T>) {
    if (strategyIdx<0) {
      setPathList(strategies.size, pathListIdx, pathList)
    } else {
      pathLists = pathLists.ensureArrayLength(strategyIdx + 1)
      val sPathLists = pathLists[strategyIdx].ensureArrayLength(pathListIdx + 1)
      pathLists[strategyIdx] = sPathLists
      sPathLists[pathListIdx] = pathList as List<*>
    }
  }

  inline private fun <reified V> Array<V?>?.ensureArrayLength(length: Int) = when {
    this==null -> arrayOfNulls<V>(length)
    else -> this.ensureArrayLengthNotNull(length)
  }

  private fun <V> Array<V?>.ensureArrayLengthNotNull(length: Int): Array<V?> {
    val array = this
    val srcLen = array.size
    if (srcLen < length) {
        return array + arrayOfNulls(length - srcLen)
    }
    return array
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

  private fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> getStrategyIndex(strategy: S): Int {
    return strategies.indexOfFirst { it===strategy }
  }

  private fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> ensureStrategyIndex(strategy: S): Int {
    var strategyIdx = getStrategyIndex(strategy)
    if (strategyIdx < 0) {
      strategyIdx = strategies.size
      val newStrategies = strategies.ensureArrayLengthNotNull(strategyIdx+1)
      newStrategies[strategyIdx] = strategy
      strategies = newStrategies
    }
    return strategyIdx
  }

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> getPen(strategy: S, index: Int, alternate: PEN_T.()->Unit): PEN_T {
    val strategyIdx = getStrategyIndex(strategy)
    return getPen<PEN_T>(strategyIdx, index) ?: strategy.newPen().apply { alternate(); setPen(strategyIdx, index, this) }
  }

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> getPen(strategy: S, index: Int): PEN_T? {
    val strategyIdx = getStrategyIndex(strategy)

    return getPen<PEN_T>(strategyIdx, index)
  }

  private fun <PEN_T : Pen<PEN_T>> getPen(strategyIdx: Int, penIdx: Int): PEN_T? {
    if (strategyIdx < 0 || strategyIdx >= pens.size || penIdx >= pens[strategyIdx]!!.size) {
      return null
    }
    return pens[strategyIdx]!![penIdx] as PEN_T?
  }

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> getPath(strategy: S, index: Int, alternate:PATH_T.()->Unit): PATH_T {
    val strategyIdx = getStrategyIndex(strategy)
    return getPath<S, PEN_T, PATH_T>(strategy, index) ?: strategy.newPath().apply { alternate(); setPath(strategyIdx, index, this) }
  }

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> getPath(strategy: S, index: Int): PATH_T? {
    val strategyIdx = getStrategyIndex(strategy)
    return getPath<PATH_T>(strategyIdx, index)
  }

  private fun <PATH_T : DiagramPath<PATH_T>> getPath(strategyIdx: Int, pathIdx: Int): PATH_T? {
    if (strategyIdx < 0 || strategyIdx >= paths.size || pathIdx >= paths[strategyIdx]!!.size) {
      return null
    }
    return paths[strategyIdx]!![pathIdx] as PATH_T?
  }

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> getPathList(strategy: S, index: Int, alternate: (S)->List<PATH_T>): List<PATH_T> {
    val strategyIdx = getStrategyIndex(strategy)
    return getPathList<S, PEN_T, PATH_T>(strategyIdx, index) ?: alternate(strategy).apply { setPathList(strategyIdx, index, this) }
  }

  fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> getPathList(strategy: S, index: Int): List<PATH_T>? {
    val strategyIdx = getStrategyIndex(strategy)
    return getPathList<S, PEN_T, PATH_T>(strategyIdx, index)
  }

  private fun <S : DrawingStrategy<S, PEN_T, PATH_T>, PEN_T : Pen<PEN_T>, PATH_T : DiagramPath<PATH_T>> getPathList(strategyIdx: Int, pathListIdx: Int): List<PATH_T>? {
    if (strategyIdx < 0 || strategyIdx >= pathLists.size || pathListIdx >= pathLists[strategyIdx]!!.size) {
      return null
    }
    return pathLists[strategyIdx]!![pathListIdx] as List<PATH_T>?
  }

  /**
   * Clear all the paths at the given index.
   * @param index The index of the paths to clear
   */
  fun clearPath(index: Int) {
    for (lists in paths) {
      if (lists != null && lists!!.size > index) {
        lists[index] = null
      }
    }
  }

}
