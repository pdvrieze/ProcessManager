package nl.adaptivity.process.clientProcessModel;


public interface SerializerAdapter {

  void addNamespace(String pPrefix, String pNamespace);

  void startTag(String pNamespace, String pName);

  void endTag(String pNamespace, String pName);

  void addAttribute(String pName, String pValue);

}
