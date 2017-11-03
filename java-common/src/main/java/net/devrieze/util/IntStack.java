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

/*
 * Created on Feb 4, 2004
 *
 */

package net.devrieze.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * A linked list based stack implementation for characters.
 * 
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */
public final class IntStack {

  private static final class Node {

    /** The next node. */
    final Node next;

    /** The value this node contains. */
    final int value;

    /**
     * Create a new node.
     * 
     * @param value The value in the node.
     * @param next The next element. This may be {@code NULL}if there is no
     *          next element.
     */
    Node(final int value, final Node next) {
      this.next = next;
      this.value = value;
    }

    /**
     * A string representation of the node.
     * 
     * @return A string representation
     */
    @NotNull
    @Override
    public String toString() {
      if (next != null) {
        return next + ", " + Integer.toString(value);
      }

      return Integer.toString(value);
    }
  }

  private Node top;

  /**
   * Returns {@code true} if the stack is empty.
   * 
   * @return boolean
   */
  @Contract(pure = true)
  public boolean isEmpty() {
    return top == null;
  }

  /**
   * Get the value at the top of the stack.
   * 
   * @return The value at the top
   */
  @Contract(pure = true)
  public int peek() {
    return top.value;
  }

  /**
   * Get the top character from the stack.
   * 
   * @return the top character
   */
  public int pop() {
    final int val = top.value;
    top = top.next; /* fails if empty */

    return val;
  }

  /**
   * Push a value onto the stack.
   * 
   * @param pValue the value to be pushed
   */
  public void push(final int pValue) {
    top = new Node(pValue, top);
  }

  /**
   * Get a String representation for the stack.
   * 
   * @return The string representation
   */
  @NotNull
  @Contract(pure = true)
  @Override
  public String toString() {
    return "[" + top + "]";
  }
}
