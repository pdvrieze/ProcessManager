package nl.adaptivity.process.clientProcessModel;


public interface SerializerAdapter {

  void addNamespace(String prefix, String namespace);

  void startTag(String namespace, String name, boolean addWs);

  void endTag(String namespace, String name, boolean addWs);

  void addAttribute(String namespace, String name, String value);

  void text(String string);

  void ignorableWhitespace(String string);

  void cdata(String data);

  void comment(String data);

  void entityReference(String localName);

}
