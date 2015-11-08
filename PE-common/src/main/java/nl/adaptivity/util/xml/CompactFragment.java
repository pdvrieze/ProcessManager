package nl.adaptivity.util.xml;

import java.util.Arrays;
import java.util.Collections;


/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.
 */
public class CompactFragment {

  private final SimpleNamespaceContext namespaces;
  private final char[] content;

  public CompactFragment(final Iterable<Namespace> pNamespaces, final char[] pContent) {
    namespaces = SimpleNamespaceContext.from(pNamespaces);
    content = pContent;
  }

  /** Convenience constructor for content without namespaces. */
  public CompactFragment(final String pString) {
    this(Collections.<Namespace>emptyList(), pString.toCharArray());
  }

  public SimpleNamespaceContext getNamespaces() {
    return namespaces;
  }

  public char[] getContent() {
    return content;
  }

  @Override
  public boolean equals(final Object pO) {
    if (this == pO) return true;
    if (pO == null || getClass() != pO.getClass()) return false;

    CompactFragment that = (CompactFragment) pO;

    if (!namespaces.equals(that.namespaces)) return false;
    return Arrays.equals(content, that.content);

  }

  @Override
  public int hashCode() {
    int result = namespaces.hashCode();
    result = 31 * result + Arrays.hashCode(content);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CompactFragment{");
    sb.append("namespaces=[");
    {
      int nsCount = namespaces.size();
      for(int i = 0; i< nsCount; ++i) {
        if (i>0) { sb.append(", "); }
        sb.append(namespaces.getPrefix(i)).append(" -> ").append(namespaces.getNamespaceURI(i));
      }
    }
    sb.append(']');
    if (content!=null) {
      sb.append(", content=").append(new String(content));
    }
    sb.append('}');
    return sb.toString();
  }

  public String getContentString() {
    return new String(getContent());
  }
}
