package nl.adaptivity.darwin.gwt.client;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;


public final class GwtXmlUtil {
  private GwtXmlUtil() {}

  public static SafeHtml getTextContent(Element pElement) {
    SafeHtmlBuilder builder = new SafeHtmlBuilder();
    getTextContent(builder, pElement);
    return builder.toSafeHtml();
  }

  private static void getTextContent(SafeHtmlBuilder pBuilder, Element pElement) {
    for(Node n=pElement.getFirstChild(); n!=null; n=n.getNextSibling()) {
      switch (n.getNodeType()) {
        case Node.CDATA_SECTION_NODE:
        case Node.TEXT_NODE:
          pBuilder.appendEscaped(n.getNodeValue());
          break;
        case Node.ELEMENT_NODE:
          getTextContent(pBuilder, (Element)n);
      }
    }
  }

  public static SafeHtml serialize(NodeList pChildNodes) {
    SafeHtmlBuilder builder = new SafeHtmlBuilder();
    for(int i=0; i<pChildNodes.getLength(); ++i) {
      Node child = pChildNodes.item(i);
      serialize(builder, child);
    }
    return builder.toSafeHtml();
  }

  private static void serialize(SafeHtmlBuilder pBuilder, Node pChild) {
    pBuilder.appendHtmlConstant(pChild.toString());
  }

}
