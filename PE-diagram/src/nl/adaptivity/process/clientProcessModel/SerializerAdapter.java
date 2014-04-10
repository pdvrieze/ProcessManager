package nl.adaptivity.process.clientProcessModel;


public interface SerializerAdapter {

  void addNamespace(String pPrefix, String pNamespace);

  void startTag(String pNamespace, String pName, boolean pAddWs);

  void endTag(String pNamespace, String pName, boolean pAddWs);

  void addAttribute(String pName, String pValue);

  void text(String pString);

  void ignorableWhitespace(String pString);

}
