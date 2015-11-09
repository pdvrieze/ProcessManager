package nl.adaptivity.xml;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import java.util.Iterator;
import java.util.Map;


/**
 * Class that gathers namespace queries and records them in the given map.
 * Created by pdvrieze on 20/10/15.
 */
public class GatheringNamespaceContext implements NamespaceContext {

  private final NamespaceContext aParentContext;
  private final Map<String, String> aResultMap;

  public GatheringNamespaceContext(final NamespaceContext pParentContext, final Map<String, String> pResultMap) {
    aParentContext = pParentContext;
    aResultMap = pResultMap;
  }

  @Override
  public String getNamespaceURI(final String prefix) {
    String namespaceURI = aParentContext.getNamespaceURI(prefix);
    if (namespaceURI != null && !(XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))) {
      aResultMap.put(prefix, namespaceURI);
    }
    return namespaceURI;
  }

  @Override
  public String getPrefix(final String namespaceURI) {
    String prefix = aParentContext.getNamespaceURI(namespaceURI);
    if (prefix != null && !(XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI) || XMLConstants.XML_NS_URI.equals(namespaceURI))) {
      aResultMap.put(prefix, namespaceURI);
    }
    return prefix;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterator<String> getPrefixes(final String namespaceURI) {
    if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI) || XMLConstants.XML_NS_URI.equals(namespaceURI)) {
      for (Iterator<String> it = aParentContext.getPrefixes(namespaceURI); it.hasNext(); ) {
        aResultMap.put(it.next(), namespaceURI);
      }
    }
    return aParentContext.getPrefixes(namespaceURI);
  }
}
