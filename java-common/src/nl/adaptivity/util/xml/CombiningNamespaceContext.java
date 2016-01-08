package nl.adaptivity.util.xml;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.util.HashSet;
import java.util.Iterator;


/**
 * Created by pdvrieze on 28/08/15.
 */
public class CombiningNamespaceContext implements NamespaceContext {

  private final NamespaceContext mPrimary;
  private final NamespaceContext mSecondary;

  public CombiningNamespaceContext(final NamespaceContext primary, final NamespaceContext secondary) {
    mPrimary = primary;
    mSecondary = secondary;
  }

  @Override
  public String getNamespaceURI(final String prefix) {
    String namespaceURI = mPrimary.getNamespaceURI(prefix);
    if (namespaceURI==null || XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
      return mSecondary.getNamespaceURI(prefix);
    }
    return namespaceURI;
  }

  @Override
  public String getPrefix(final String namespaceURI) {
    String prefix = mPrimary.getPrefix(namespaceURI);
    if (prefix == null || (XMLConstants.NULL_NS_URI.equals(namespaceURI)&& XMLConstants.DEFAULT_NS_PREFIX.equals(prefix))) {
      return mSecondary.getPrefix(namespaceURI);
    }
    return prefix;
  }

  @Override
  public Iterator getPrefixes(final String namespaceURI) {
    Iterator<String> prefixes1 = mPrimary.getPrefixes(namespaceURI);
    Iterator<String> prefixes2 = mSecondary.getPrefixes(namespaceURI);
    HashSet prefixes = new HashSet();
    while (prefixes1.hasNext()) {
      prefixes.add(prefixes1.next());
    }
    while (prefixes2.hasNext()) {
      prefixes.add(prefixes2.next());
    }
    return prefixes.iterator();
  }
}
