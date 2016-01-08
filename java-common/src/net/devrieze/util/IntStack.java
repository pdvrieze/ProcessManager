/*
 * Created on Feb 4, 2004
 *
 */

package net.devrieze.util;

/**
 * A linked list based stack implementation for characters.
 * 
 * @author Paul de Vrieze
 * @version 0.1 $Revision$
 */
public final class IntStack {

  private static final class Node {

    /** The next node. */
    private final Node mNext;

    /** The value this node contains. */
    private final int mValue;

    /**
     * Create a new node.
     * 
     * @param pValue The value in the node.
     * @param pNext The next element. This may be {@code NULL}if there is no
     *          next element.
     */
    private Node(final int pValue, final Node pNext) {
      mNext = pNext;
      mValue = pValue;
    }

    /**
     * A string representation of the node.
     * 
     * @return A string representation
     */
    @Override
    public String toString() {
      if (mNext != null) {
        return mNext.toString() + ", " + Integer.toString(mValue);
      }

      return Integer.toString(mValue);
    }
  }

  private Node mTop;

  /**
   * Returns <code>true</code> if the stack is empty.
   * 
   * @return boolean
   */
  public boolean isEmpty() {
    return mTop == null;
  }

  /**
   * Get the value at the top of the stack.
   * 
   * @return The value at the top
   */
  public int peek() {
    return mTop.mValue;
  }

  /**
   * Get the top character from the stack.
   * 
   * @return the top character
   */
  public int pop() {
    final int val = mTop.mValue;
    mTop = mTop.mNext; /* fails if empty */

    return val;
  }

  /**
   * Push a value onto the stack.
   * 
   * @param pValue the value to be pushed
   */
  public void push(final int pValue) {
    final Node node = new Node(pValue, mTop);
    mTop = node;
  }

  /**
   * Get a String representation for the stack.
   * 
   * @return The string representation
   */
  @Override
  public String toString() {
    return "[" + mTop.toString() + "]";
  }
}
