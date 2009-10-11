package nl.adaptivity.process.userMessageHandler.client;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NamedNodeMap;



public class StartNode extends ProcessNode {

  private StartNode(String pId) {
    super(pId);
  }

  public static ProcessNode fromXml(Element pElement) {
    String id = null;
    NamedNodeMap attrs = pElement.getAttributes();
    int attrCount = attrs.getLength();
    for (int i=0; i<attrCount; ++i) {
      Attr attr = (Attr) attrs.item(i);
      if (XMLUtil.isNS(null, attr)) {
        if ("id".equals(attr.getName())) {
          id = attr.getValue();
        } else {
          GWT.log("Unsupported attribute in startnode "+attr.toString(), null);
        }
      }
    }
    return new StartNode(id);
  }

}
