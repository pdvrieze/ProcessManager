package net.devrieze.util;

import java.util.*;


public class PrefixMap<V> extends AbstractCollection<PrefixMap.Entry<V>> {


  private static class NodeIterator<T> implements Iterator<Entry<T>> {

    Deque<Node<T>> stack;

    public NodeIterator(final Node<T> pRoot) {
      stack = new ArrayDeque<>();
      stack.push(pRoot);
      getLeftMost();
    }

    private boolean getLeftMost() {
      final Node<T> top = stack.peek();
      if (top.getLeft() != null) {
        stack.push(top.getLeft());
        if (getLeftMost()) {
          return true;
        }
      }
      if (top.value != null) {
        return true;
      }
      if (top.getBelow() != null) {
        stack.push(top.getBelow());
        if (getLeftMost()) {
          return true;
        }
      }
      if (top.getRight() != null) {
        stack.push(top.getRight());
        if (getLeftMost()) {
          return true;
        }
      }

      stack.pop();
      return false;
    }

    private boolean getNext() {
      Node<T> top = stack.peek();
      if (top.getBelow() != null) {
        stack.push(top.getBelow());
        if (getLeftMost()) {
          return true;
        }
      }
      if (top.getRight() != null) {
        stack.push(top.getRight());
        if (getLeftMost()) {
          return true;
        }
      }
      while (stack.size() > 1) {
        top = stack.pop();
        final Node<T> parent = stack.peek();
        if (top == parent.getLeft()) {
          if (parent.value != null) {
            return true;
          } else {
            if (parent.getBelow() != null) {
              stack.push(parent.getBelow());
              if (getLeftMost()) {
                return true;
              }
            }
          }
        }
        if (((top == parent.getLeft()) || (top == parent.getBelow())) && (parent.getRight() != null)) {
          stack.push(parent.getRight());
          if (getLeftMost()) {
            return true;
          }
        }
        // Must have been right arm.
      }
      // Now finished
      stack.pop();
      return false;
    }

    @Override
    public boolean hasNext() {
      return stack.size() > 0;
    }

    @Override
    public Entry<T> next() {
      final Node<T> n = stack.peek();
      if ((n == null) || (n.value == null)) {
        throw new IllegalStateException("Invalid next value in iterator");
      }
      final Entry<T> result = new Entry<>(n);
      getNext();

      return result;
    }

    @Override
    public void remove() {
      // TODO Auto-generated method stub
      //
      throw new UnsupportedOperationException("Not yet implemented");

    }

  }

  private static class CompareResult {

    public static final CompareResult LEFT = new CompareResult(null, XCompareResult.XLEFT);

    @SuppressWarnings("unused")
    public static final CompareResult ABOVE = new CompareResult(null, XCompareResult.XABOVE);

    public static final CompareResult EQUAL = new CompareResult(null, XCompareResult.XEQUAL);

    public static final CompareResult BELOW = new CompareResult(null, XCompareResult.XBELOW);

    public static final CompareResult RIGHT = new CompareResult(null, XCompareResult.XRIGHT);

    final CharSequence commonPrefix;

    final XCompareResult cmp;

    private CompareResult(final CharSequence pCommonPrefix, final XCompareResult pCmp) {
      commonPrefix = pCommonPrefix ==null ? null : pCommonPrefix;
      cmp = pCmp;
    }

    /**
     * Check whether the comparison corresponds to the given index.
     * @param pIndex The positional index
     * @return <code>true</code> when it is, <code>false</code> when not.
     */
    @SuppressWarnings("unused")
    public boolean is(int pIndex) {
      return cmp.is(pIndex);
    }

    public boolean isOpposite(int pIndex) {
      return cmp.isOpposite(pIndex);
    }

    public boolean isLeft() {
      return cmp.isLeft();
    }

    public boolean isAbove() {
      return cmp.isAbove();
    }

    public boolean isEqual() {
      return cmp.isEqual();
    }

    public boolean isBelow() {
      return cmp.isBelow();
    }

    public boolean isEqOrBelow() {
      return cmp.isEqOrBelow();
    }

    @SuppressWarnings("unused")
    public boolean isEqOrAbove() {
      return cmp.isEqOrBelow();
    }

    public boolean isRight() {
      return cmp.isRight();
    }

    public CompareResult invert() {
      return new CompareResult(commonPrefix, cmp.invert());
    }

    @Override
    public String toString() {
      if (commonPrefix == null) {
        return cmp.toString();
      } else {
        return cmp.toString() + "[\"" + commonPrefix + "\"]";
      }
    }

  }

  private enum XCompareResult {
    XLEFT(LEFTIDX),
    XABOVE(-1),
    XEQUAL(BELOWIDX),
    XBELOW(BELOWIDX),
    XRIGHT(RIGHTIDX);

    public final int index;

    XCompareResult(int pIndex) {
      index = pIndex;
    }

    public XCompareResult invert() {
      switch (this) {
        case XLEFT:
          return XRIGHT;
        case XABOVE:
          return XBELOW;
        case XEQUAL:
          return XEQUAL;
        case XBELOW:
          return XABOVE;
        case XRIGHT:
          return XLEFT;
        default:
          throw new RuntimeException("Should be unreachable");
      }
    }

    public boolean is(int pIndex) {
      return pIndex==index;
    }

    public boolean isOpposite(int pIndex) {
      return 2-pIndex==index;
    }

    public boolean isLeft() {
      return this == XLEFT;
    }

    public boolean isAbove() {
      return this == XABOVE;
    }

    public boolean isEqual() {
      return this == XEQUAL;
    }

    public boolean isBelow() {
      return this == XBELOW;
    }

    public boolean isEqOrBelow() {
      return (this == XEQUAL) || (this == XBELOW);
    }

    public boolean isRight() {
      return this == XRIGHT;
    }
  }

  private static final int LEFTIDX=0;
  private static final int BELOWIDX=1;
  private static final int RIGHTIDX=2;

  private static final class Node<T> {

    @SuppressWarnings("unchecked")
    private Node<T>[] mChildren = new Node[3];

    public final CharSequence prefix;

    public T value;

    private int mCount;

    public Node(final CharSequence pPrefix) {
      prefix = pPrefix;
    }

    public Node(final Node<T> pOrig) {
      for(int i=mChildren.length-1;i>=0; --i) {
        Node src = pOrig.mChildren[i];
        if (src!=null) {
          mChildren[i] = new Node(src);
        }
      }
      value = pOrig.value;
      prefix = pOrig.prefix;
      mCount = pOrig.mCount;

    }

    public Node(final CharSequence pPrefix, final T pValue) {
      if (pValue == null) {
        throw new NullPointerException();
      }
      prefix = pPrefix;
      value = pValue;
      mCount = 1;
    }

    public int add(CompareResult pCompare, Node<T> child) {
      int expectedCount = 0;

      assert (expectedCount = mCount + child.mCount)!=Integer.MIN_VALUE;
      assert pCompare.cmp == prefixCompare(prefix, child.prefix).cmp : "Accuracy of cached comparisons";
      assert ! pCompare.isAbove() : "Don't add a child that should be a parent";

      final int result = addChild(pCompare.cmp.index, child);

      assert mCount == expectedCount : "Correct child counts";
      return result;
    }

    protected int addChild(int pos, Node<T> child) {
      Node<T> current = mChildren[pos];
      if (current==null) {
        setChild(pos, child);
        return child.mCount;
      } else{
        CompareResult cmp = prefixCompare(0, current.prefix, child.prefix);
        if ((pos!=BELOWIDX && cmp.isOpposite(pos))||cmp.isAbove()) {
          int result = child.mCount;
          interpose(pos, cmp.invert(), child);
          rebalance(pos);
          return result;
        } else {

          if (pos==BELOWIDX && (cmp.commonPrefix!=null && (cmp.commonPrefix.length()> prefix.length() && cmp.commonPrefix.length()<getBelow().prefix.length()))){
            // introduce a new intermediate parent
            current = new Node<>(cmp.commonPrefix);
            cmp = CompareResult.BELOW;
            interpose(pos, CompareResult.BELOW, current);
            assert current.mCount == current.getBelow().mCount + (current.getLeft()==null ? 0 : current.getLeft().mCount) + (current.getRight()==null ? 0 : current.getRight().mCount);
          }

          final int addcnt = current.add(cmp, child);
          mCount+=addcnt;
          rebalance(pos);
          return addcnt;
        }
      }
    }

    public boolean remove(CompareResult pCompare, Entry<?> pEntry) {
      if(pEntry.getValue()==null) { throw new NullPointerException(); }
      int oldCount = mCount;
      assert pCompare.cmp == prefixCompare(prefix, pEntry.getPrefix()).cmp;
      assert ! pCompare.isAbove();
      assert !(pCompare.isEqual() && pEntry.getValue().equals(value));

      final boolean result = removeChild(pCompare.cmp.index, pCompare, pEntry);

      assert mCount == oldCount - (result ? 1 : 0) ;
      return result;
    }

    protected boolean removeChild(int idx, CompareResult pCompare, Entry<?> pEntry) {
      final Node<T> current = mChildren[idx];
      if (current==null) { return false; }
      CompareResult cmp = prefixCompare(0, current.prefix, pEntry.getPrefix());
      if (cmp.isEqual() && pEntry.getValue().equals(current.value)) {
        Node<T> oldNode = setChild(idx, null);
        oldNode.value=null;
        addIndividualElements(pCompare, oldNode);
        return true;
      } else {
        return reduceCount(current.remove(cmp, pEntry));
      }
    }

    private boolean reduceCount(boolean pDoReduce) {
      if (pDoReduce) { --mCount; }
      return pDoReduce;
    }

    private void rebalance(int idx) {
      final Node<T> originalRoot = mChildren[idx];
      if (originalRoot==null) { return; }
      int originalCount = originalRoot.mCount;

      final int balanceFactor = getBalanceFactor(originalRoot);

      if (balanceFactor<=-2) {
        mChildren[idx] = rotateRight(originalRoot, true);
      } else if (balanceFactor>=2) {
        mChildren[idx] = rotateLeft(originalRoot, true);
      }
      assert mChildren[idx].mCount == originalCount : "Stable child counts";
    }

    private static int getBalanceFactor(final Node<?> root) {
      int leftCnt = root.getLeft()==null ? 0 : root.getLeft().mCount;
      int rightCnt = root.getRight()==null ? 0 : root.getRight().mCount;
      assert leftCnt+rightCnt+(root.value==null ? 0 : 1) + (root.getBelow()==null ? 0 : root.getBelow().mCount) == root.mCount;
      return rightCnt-leftCnt;
    }

    private static <U> Node<U> rotateLeft(final Node<U> originalRoot, boolean testPivotBalance) {
      Node<U> pivot = originalRoot.setRight(null);
      if (testPivotBalance && getBalanceFactor(pivot)<=-1) {
        pivot = rotateRight(pivot, false);
      }

      final Node<U> newRight = pivot.setLeft(null);
      originalRoot.setRight(newRight);
      pivot.setLeft(originalRoot);
      return pivot;
    }

    private static <U> Node<U> rotateRight(final Node<U> originalRoot, boolean testPivotBalance) {
      Node<U> pivot = originalRoot.setLeft(null);
      if (testPivotBalance && getBalanceFactor(pivot)>=1) {
        pivot = rotateLeft(pivot, false);
      }

      final Node<U> newLeft = pivot.setRight(null);
      originalRoot.setLeft(newLeft);
      pivot.setRight(originalRoot);
      return pivot;
    }

    protected void interpose(int pIndex, CompareResult pCompareResult, Node<T> pNewChild) {
      // Remove the old child from this node.
      Node<T> oldChild = setChild(pIndex, null);

      // First add the old child to the new one (so newChild has the correct node count)
      pNewChild.addIndividualElements(pCompareResult, oldChild);
      final Node<T> left = pNewChild;

      // Then set the new left.
      setChild(pIndex, left);
    }

    private void addIndividualElements(CompareResult pCompareResult, Node<T> pSource) {
      Node<T> sourceLeft = pSource.setLeft(null);
      Node<T> sourceRight = pSource.setRight(null);

      if (pSource.value!=null) {
        // We can ignore the below nodes as they would remain under the source
        this.add(pCompareResult, pSource);
      } else if (pSource.getBelow()!=null){
        // The source is a placeholder, and can be removed. In this case
        // the below nodes must also be added (these could not exist if remove just demoted this node)
        add(pCompareResult, pSource.getBelow());
      }
      if (sourceLeft!=null) {
        this.add(prefixCompare(this.prefix, sourceLeft.prefix), sourceLeft);
      }
      if (sourceRight!=null) {
        this.add(prefixCompare(this.prefix, sourceRight.prefix), sourceRight);
      }
    }

    public Node<T> setLeft(final Node<T> left) {
      return setChild(LEFTIDX, left);
    }

    public Node<T> setRight(final Node<T> right) {
      return setChild(RIGHTIDX, right);
    }

    public Node<T> setBelow(final Node<T> below) {
      return setChild(BELOWIDX, below);
    }

    private Node<T> setChild(int pIndex, Node<T> child) {
      Node<T> result = mChildren[pIndex];
      if (result != child) {
        if (result != null) {
          mCount -= result.mCount;
        }
        assert child==null || pIndex!=LEFTIDX || child.prefix.toString().compareTo(prefix.toString())<0;
        assert child==null || pIndex!=BELOWIDX || child.prefix.length()>=prefix.length();
        assert child==null || pIndex!=RIGHTIDX || child.prefix.toString().compareTo(prefix.toString())>0;
        mChildren[pIndex] = child;
        if (child != null) {
          mCount += child.mCount;
        }
      }
      return result;
    }

    public Node<T> getLeft() {
      return mChildren[LEFTIDX];
    }

    public Node<T> getRight() {
      return mChildren[RIGHTIDX];
    }

    public Node<T> getBelow() {
      return mChildren[BELOWIDX];
    }

    public int getCount() {
      return mCount;
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder();
      appendTo(result, "","");
      return result.toString();
    }

    public void appendTo(StringBuilder b, String strprefix, String labelLine) {
      final int paddingLength = Math.max(0, labelLine.length()-strprefix.length()+prefix.length()-2);
      StringBuilder basePrefix = new StringBuilder().append(strprefix).append(StringUtil.charRepeat(paddingLength,' '));
      boolean inlineValue = getBelow()!=null || getLeft()!=null || getRight()!=null;

      if (getLeft()==null) {
        b.append(basePrefix).append("     [===== (");
      } else {
        b.append(basePrefix).append("[===== (");
      }
      b.append(mCount).append(')');
      if (inlineValue && value!=null) {
        b.append(" value=\"").append(value).append('"');
      }
      b.append('\n');

      StringBuilder belowPrefix = new StringBuilder().append(basePrefix).append("     ");
      belowPrefix.append("|");

      String siblingPrefix = new StringBuilder(basePrefix.length()+1).append(basePrefix).append('|').toString();


      if (getLeft()!=null) {
        getLeft().appendTo(b, siblingPrefix, siblingPrefix+" l=");
        b.append('\n').append(basePrefix).append("\\----\\\n");
      }
      if (!inlineValue) {
        b.append(labelLine).append('"').append(this.prefix).append('\"');
        b.append(StringUtil.charRepeat(Math.max(0, 2-prefix.length()-strprefix.length()), ' '));
        b.append(" ] ").append("value=\"").append(value).append("\"\n");
      } else if (getBelow()!=null){
//        b.append(belowPrefix).append('\n');
        getBelow().appendTo(b, belowPrefix.toString(), labelLine+ '"' +this.prefix+'"'+
            StringUtil.charRepeat(Math.max(0, 2-prefix.length()-strprefix.length()), ' ').toString()+" ] b=");
        b.append('\n');
      } else {
        b.append(labelLine).append('"').append(this.prefix).append('\"');
        b.append(StringUtil.charRepeat(Math.max(0, 2-prefix.length()-strprefix.length()), ' '));
        b.append(" ]\n");
      }
//      if (!inlineValue && getBelow()!=null) {
//        b.append(belowPrefix).append('\n');
//        getBelow().appendTo(b, belowPrefix.toString(), belowPrefix+" b=");
//        b.append('\n');
//      }
      if (getRight()!=null) {
        b.append(basePrefix).append("/----/\n");
        getRight().appendTo(b, siblingPrefix, siblingPrefix+" r=");
        b.append('\n');
        b.append(basePrefix).append("[=====");
      } else {
        b.append(basePrefix).append("     [=====");
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Node<T> clone() {
      return new Node<T>(this);
    }
  }

  public static final class Entry<T> implements Map.Entry<CharSequence, T> {

    private final Node<T> mNode;

    public Entry(final CharSequence pKey, final T pValue) {
      mNode = pValue == null ? new Node<T>(pKey) : new Node<>(pKey, pValue);
    }

    public Entry(final Node<T> pNode) {
      mNode = pNode;
    }

    public CharSequence getPrefix() {
      return mNode.prefix;
    }

    @Override
    public CharSequence getKey() {
      return mNode.prefix;
    }

    @Override
    public T setValue(final T pValue) {
      final T result = mNode.value;
      mNode.value = pValue;
      return result;
    }

    @Override
    public T getValue() {
      return mNode.value;
    }

    @Override
    public String toString() {
      return "[\"" + mNode.prefix + "\"->" + mNode.value + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = (prime * result) + mNode.prefix.hashCode();
      result = (prime * result) + ((mNode.value == null) ? 0 : mNode.value.hashCode());
      return result;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final Entry<?> other = (Entry<?>) obj;
      if (mNode.prefix == null) {
        if (other.mNode.prefix != null) {
          return false;
        }
      } else if (!mNode.prefix.equals(other.mNode.prefix)) {
        return false;
      }
      if (mNode.value == null) {
        if (other.mNode.value != null) {
          return false;
        }
      } else if (!mNode.value.equals(other.mNode.value)) {
        return false;
      }
      return true;
    }
  }

  private Node<V> mRoot;

  public PrefixMap() {
    mRoot = new Node<>("");
  }

  @Override
  public Iterator<Entry<V>> iterator() {
    final NodeIterator<V> iter = new NodeIterator<>(mRoot);
    return iter;
  }

  public List<Entry<V>> toList() {
    final List<Entry<V>> list = toList(new ArrayList<Entry<V>>(mRoot.getCount()), mRoot);
    assert (mRoot==null && list.size()==0) || (list.size()==mRoot.getCount());
    return list;
  }

  private List<Entry<V>> toList(final List<Entry<V>> pCollectorList, final Node<V> pNode) {
    if (pNode == null) {
      return pCollectorList;
    }
    toList(pCollectorList, pNode.getLeft());
    if (pNode.value != null) {
      pCollectorList.add(new Entry<>(pNode));
    }
    toList(pCollectorList, pNode.getBelow());
    return toList(pCollectorList, pNode.getRight());
  }

  @Override
  public int size() {
    return mRoot == null ? 0 : mRoot.getCount();
  }

  @Override
  public void clear() {
    mRoot = null;
  }

  public void put(final CharSequence prefix, final V value) {
    if ((prefix == null) || (value == null)) {
      throw new NullPointerException();
    }

    final Node<V> n = new Node<>(prefix, value);
    int cnt = put(n);
    assert cnt==1;
  }

  private int put(final Node<V> n) {
    if (mRoot==null) {
      mRoot = n;
      return 1;
    } else {
      final CompareResult cmp = prefixCompare(0, mRoot.prefix, n.prefix);
      int cnt = mRoot.add(cmp, n);
      return cnt;
    }
  }

  private static CompareResult prefixCompare(final CharSequence s1, final CharSequence s2) {
    return prefixCompare(0, s1, s2);
  }

  private static CompareResult prefixCompare(final int pIgnoreOffset, final CharSequence s1, final CharSequence s2) {
    assert (s1.length() >= pIgnoreOffset) && (s2.length() >= pIgnoreOffset) && s2.toString().startsWith(s1.subSequence(0, pIgnoreOffset).toString());
    if (s2.length() < s1.length()) {
      return prefixCompare(pIgnoreOffset, s2, s1).invert();
    }
    for (int i = pIgnoreOffset; i < s1.length(); ++i) {
      final char c = s1.charAt(i);
      final char d = s2.charAt(i);
      if (d < c) {
        return i == pIgnoreOffset ? CompareResult.LEFT : new CompareResult(s1.subSequence(0, i), XCompareResult.XLEFT);
      } else if (d > c) {
        return i == pIgnoreOffset ? CompareResult.RIGHT : new CompareResult(s1.subSequence(0, i), XCompareResult.XRIGHT);
      }
    }
    if (s1.length() == s2.length()) {
      return CompareResult.EQUAL;
    } else {
      return CompareResult.BELOW;
    }
  }

  @Override
  public boolean add(final Entry<V> entry) {
    put(entry.getPrefix(), entry.getValue());
    return true;
  }

  @Override
  public boolean contains(final Object pO) {
    if (!(pO instanceof Entry)) {
      return false;
    }
    final Entry<?> entry = (Entry<?>) pO;
    Node<V> candidate = getNodeForPrefix(mRoot, entry.getPrefix());
    while ((candidate != null) && candidate.prefix.equals(entry.getPrefix())) {
      if (candidate.value.equals(entry.getValue())) {
        return true;
      }
      candidate = candidate.getBelow();
    }
    return false;
  }

  public boolean containsKey(final Object pKey) {
    if (!(pKey instanceof CharSequence)) {
      return false;
    }
    final String key = pKey.toString();
    final Node<V> candidate = getNodeForPrefix(mRoot, key);
    if ((candidate != null) && candidate.prefix.equals(key)) {
      return true;
    }
    return false;
  }

  public boolean containsValue(final Object pKey) {
    if (pKey == null) {
      return false;
    }
    for (final Entry<V> entry : this) {
      if (pKey.equals(entry.getValue())) {
        return true;
      }
    }
    return false;
  }

  private Node<V> getNodeForPrefix(final Node<V> pNode, final CharSequence pPrefix) {
    final CompareResult comparison = prefixCompare(pNode.prefix, pPrefix);
    if (comparison.isEqual()) {
      return pNode;
    }
    if (comparison.isLeft()) {
      if (pNode.getLeft() == null) {
        return null;
      }
      return getNodeForPrefix(pNode.getLeft(), pPrefix);
    } else if (comparison.isBelow()) {
      if (pNode.getBelow() == null) {
        return null;
      }
      return getNodeForPrefix(pNode.getBelow(), pPrefix);
    } else if (comparison.isRight()) {
      if (pNode.getRight() == null) {
        return null;
      }
      return getNodeForPrefix(pNode.getRight(), pPrefix);
    } else { // above
      return pNode;
    }
  }

  @Override
  public boolean remove(final Object pO) {
    if (mRoot==null || (!(pO instanceof Entry))) {
      return false;
    }
    final Entry<?> entry = (Entry<?>) pO;

    return mRoot.remove(CompareResult.BELOW, entry);
//
//    return remove(CompareResult.BELOW, 0, mRoot, entry);
  }

  @Override
  public boolean removeAll(final Collection<?> pC) {
    boolean result = false;
    for (final Object o : pC) {
      result |= remove(o);
    }
    return result;
  }

  @Override
  public boolean retainAll(final Collection<?> pC) {
    // Implement by creating a new map with items present in both collections,
    // and then swaping the contents to this one
    final PrefixMap<V> tmp = new PrefixMap<>();
    for (final Object o : pC) {
      if (contains(o)) {
        @SuppressWarnings("unchecked")
        final Entry<V> entry = (Entry<V>) o;
        tmp.add(entry);
      }
    }
    final boolean result = size() > tmp.size();
    mRoot = tmp.mRoot;
    return result;
  }

  /**
   * Get a list of the entries whose prefix starts with the parameter.
   * @param pString The shared prefix.
   * @return The resulting set of entries.
   */
  public Collection<Entry<V>> getPrefixes(final String pString) {
    final Node<V> newRoot = getPrefixes(mRoot, pString, 0);
    final PrefixMap<V> result = new PrefixMap<>();
    result.mRoot = newRoot;
    return result;
  }

  private Node<V> getPrefixes(final Node<V> pNode, final CharSequence pString, final int pOffset) {
    if (pNode == null) {
      return null;
    }
    final CompareResult cmp = prefixCompare(pOffset, pNode.prefix, pString);
    if (cmp.isEqOrBelow()) {
      final Node<V> result = new Node<>(pNode); // copy
      result.setLeft(getPrefixes(result.getLeft(), pString, pOffset));
      result.setRight(getPrefixes(result.getRight(), pString, pOffset));//
      // update the children
      result.setBelow(getPrefixes(pNode.getBelow(), pString, pNode.prefix.length()));
      return result;
    } else if (cmp.isLeft()) {
      return getPrefixes(pNode.getLeft(), pString, pOffset);
    } else if (cmp.isRight()) {
      return getPrefixes(pNode.getRight(), pString, pOffset);
    } else { // Node prefix is longer than string
      return null;
    }
  }

  /**
   * Get a collection with all values whose keys start with the given prefix.
   * @param pPrefix The prefix
   * @return The resulting collection of values.
   */
  public Collection<V> getPrefixValues(final String pPrefix) {
    final PrefixMap<V> entryResult = (PrefixMap<V>) getPrefixes(pPrefix);
    return new ValueCollection<>(entryResult);
  }

  /**
   * Get a collection of all entries longer than or equal to the given prefix
   *
   * @param pPrefix the prefix
   * @return the collection
   */
  public Collection<Entry<V>> getLonger(final String pPrefix) {
    final Node<V> baseNode = getNodeForPrefix(mRoot, pPrefix);
    if (baseNode == null) {
      return Collections.emptyList();
    }
    final Node<V> copy = baseNode.clone();

    Node<V> left = copy.setLeft(null);
    Node<V> right = copy.setRight(null);

    final Node<V> resultNode;
    if (copy.prefix.length()==pPrefix.length()) {
      resultNode = copy;
    } else {
      resultNode = new Node<>(pPrefix);
      resultNode.add(CompareResult.BELOW, copy);

      if (left!=null) {
        CompareResult cmpLeft = prefixCompare(pPrefix, left.prefix);
        if (cmpLeft.isBelow()) {
          resultNode.add(cmpLeft, left);
        }
      }
      if (right!=null){
        CompareResult cmpRight = prefixCompare(pPrefix, right.prefix);
        if (cmpRight.isBelow()) {
          resultNode.add(cmpRight, right);
        }
      }

    }

    final PrefixMap<V> result = new PrefixMap<>();
    if (result.mRoot==null) {
      result.mRoot=new Node<>("");
    }
    result.mRoot.add(CompareResult.BELOW, resultNode);
    return result;
  }

  public Collection<V> getLongerValues(final String pPrefix) {
    final PrefixMap<V> entryResult = (PrefixMap<V>) getLonger(pPrefix);
    return new ValueCollection<>(entryResult);
  }

  /** Get all values with the given key */
  public Collection<Entry<V>> get(final String pPrefix) {
    final Node<V> baseNode = getNodeForPrefix(mRoot, pPrefix);
    if ((baseNode == null) || (!baseNode.prefix.equals(pPrefix))) {
      return Collections.<Entry<V>> emptyList();
    }
    final Node<V> resultNode = baseNode.clone();
    resultNode.setLeft(null);
    resultNode.setRight(null);


    Node<V> v = resultNode;
    while ((v.getBelow() != null) && v.getBelow().prefix.equals(pPrefix)) {
      v.setBelow(v.getBelow().clone());
      v.getBelow().setLeft(null);
      v.getBelow().setRight(null);
      v = v.getBelow();
    }
    fixCount(resultNode);

    final PrefixMap<V> resultMap = new PrefixMap<>();
    resultMap.put(resultNode);
    return resultMap;
  }

  private int fixCount(final Node<V> pNode) {
    if (pNode == null) {
      return 0;
    }
    final int newCount = fixCount(pNode.getLeft()) + fixCount(pNode.getBelow()) + fixCount(pNode.getRight()) + (pNode.value == null ? 0 : 1);
    pNode.mCount = newCount;
    return newCount;
  }

  public String toTestString() {
    return mRoot.toString();
  }

}
