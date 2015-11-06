package nl.adaptivity.util.xml;

/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.
 */
public class CompactFragment {

  private final SimpleNamespaceContext namespaces;
  private final char[] content;

  public CompactFragment(final SimpleNamespaceContext pNamespaces, final char[] pContent) {
    namespaces = pNamespaces;
    content = pContent;
  }

  public SimpleNamespaceContext getNamespaces() {
    return namespaces;
  }

  public char[] getContent() {
    return content;
  }
}
