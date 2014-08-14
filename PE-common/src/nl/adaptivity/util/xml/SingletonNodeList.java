package nl.adaptivity.util.xml;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SingletonNodeList implements NodeList {

  private final Node node;

  public SingletonNodeList(Node pNode) {
    node = pNode;
  }

  @Override
  public Node item(int pIndex) {
    if (pIndex!=0) { return null; }
    return node;
  }

  @Override
  public int getLength() {
    return 1;
  }

}