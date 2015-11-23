package nl.adaptivity.util.xml;

import net.devrieze.util.StringUtil;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.util.Arrays;


/**
 * A utility class that helps with maintaining a namespace context in a parser.
 * Created by pdvrieze on 16/11/15.
 */
public class NamespaceHolder {

  private String[] mNamespaces = new String[10];
  private int[] mNamespaceCounts = new int[20];
  private int mDepth = 0;

  public void incDepth() {
    ++mDepth;
    if (mDepth >= mNamespaceCounts.length) {
      int[] namespaceCounts = new int[mNamespaceCounts.length * 2];
      System.arraycopy(mNamespaceCounts, 0, namespaceCounts, 0, mNamespaceCounts.length);
      mNamespaceCounts = namespaceCounts;
    }
    mNamespaceCounts[mDepth] = mNamespaceCounts[mDepth-1];
  }

  public void decDepth() { // XXX consider shrinking the arrays.
    Arrays.fill(mNamespaces, mDepth==0 ? 0 : mNamespaceCounts[mDepth - 1], mNamespaceCounts[mDepth], null); // Clear out all unused namespaces
    mNamespaceCounts[mDepth] = 0;
    --mDepth;
  }

  public void clear() {
    mNamespaces = new String[10];
    mNamespaceCounts = new int[20];
    mDepth = 0;
  }

  public void addPrefixToContext(final Namespace ns) {
    addPrefixToContext(ns.getPrefix(), ns.getNamespaceURI());
  }

  public int getDepth() {
    return mDepth;
  }

  public void addPrefixToContext(final CharSequence prefix, final CharSequence namespaceUri) {
    int nextNamespacePos = 2 * mNamespaceCounts[getDepth()];
    if (nextNamespacePos >= mNamespaces.length) {
      enlarge();
    }
    mNamespaces[nextNamespacePos] = StringUtil.toString(prefix);
    mNamespaces[nextNamespacePos + 1] = StringUtil.toString(namespaceUri);
    mNamespaceCounts[getDepth()]++;
  }

  private void enlarge() {
    String[] namespaces = new String[mNamespaces.length * 2];
    System.arraycopy(mNamespaces, 0, namespaces, 0, mNamespaceCounts[mDepth]);
    mNamespaces = namespaces;
  }

  public NamespaceContext getNamespaceContext() {
    int startPos = 0; // From first namespace
    int endPos = mNamespaceCounts[mDepth] * 2;
    String[] pairs = new String[endPos - startPos];
    System.arraycopy(mNamespaces, startPos, pairs, 0, pairs.length);
    return new SimpleNamespaceContext(pairs);
  }

  public CharSequence getNamespaceUri(final CharSequence prefix) {
    switch (prefix.toString()) {
      case XMLConstants.XML_NS_PREFIX:
        return XMLConstants.XML_NS_URI;
      case XMLConstants.XMLNS_ATTRIBUTE:
        return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

      default: {
        final int count = mNamespaceCounts[getDepth()] * 2;
        for (int i = 0; i < count; i += 2) {
          if (StringUtil.isEqual(prefix, mNamespaces[i])) {
            return mNamespaces[i + 1];
          }
        }
      }
    }
    if (prefix.length() == 0) {
      return XMLConstants.NULL_NS_URI;
    }
    return null;
  }

  public CharSequence getPrefix(final CharSequence namespaceUri) {
    switch (namespaceUri.toString()) {
      case XMLConstants.XML_NS_URI:
        return XMLConstants.XML_NS_PREFIX;
      case XMLConstants.XMLNS_ATTRIBUTE_NS_URI:
        return XMLConstants.XMLNS_ATTRIBUTE;
      case XMLConstants.NULL_NS_URI: {
        final int count = mNamespaceCounts[getDepth()] * 2;
        for (int i = 0; i < count; i += 2) {
          if (mNamespaces[i].length() == 0 && mNamespaces[i + 1].length() > 1) {
            // The default prefix is bound to a non-null namespace
            return null;
          }
        }
        return XMLConstants.DEFAULT_NS_PREFIX;
      }

      default: {
        final int count = mNamespaceCounts[getDepth()] * 2;
        for (int i = 1; i < count; i += 2) {
          if (StringUtil.isEqual(namespaceUri, mNamespaces[i])) {
            return mNamespaces[i - 1];
          }
        }
      }
    }
    return null;
  }
}
