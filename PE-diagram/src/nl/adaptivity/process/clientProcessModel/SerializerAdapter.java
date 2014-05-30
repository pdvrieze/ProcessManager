package nl.adaptivity.process.clientProcessModel;


public interface SerializerAdapter {

  void addNamespace(String pPrefix, String pNamespace);

  void startTag(String pNamespace, String pName, boolean pAddWs);

  void endTag(String pNamespace, String pName, boolean pAddWs);

  void addAttribute(String pNamespace, String pName, String pValue);

  void text(String pString);

  void ignorableWhitespace(String pString);

  void cdata(String pData);

  void comment(String pData);

  void entityReference(String pLocalName);

}
