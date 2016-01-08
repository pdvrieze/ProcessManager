package nl.adaptivity.util.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SingletonNodeList implements NodeList {

  private final Node mNode;

  public SingletonNodeList(final Node node) {
    mNode = node;
  }

  @Nullable
  @Override
  public Node item(final int index) {
    if (index!=0) { return null; }
    return mNode;
  }

  @Override
  public int getLength() {
    return 1;
  }

  @NotNull
  @Override
  public String toString() {
    return "[" + mNode.toString()+"]";
  }

}