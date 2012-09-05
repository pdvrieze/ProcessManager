package nl.adaptivity.jbi;

import java.util.Set;

import javax.activation.DataHandler;
import javax.xml.transform.Source;


public interface NormalizedMessage {

  DataHandler getAttachment(String pKey);

  Set<String> getAttachmentNames();

  void addAttachment(String pKey, DataHandler pValue);

  void removeAttachment(String pKey);

  void setContent(Source pResult);

  Source getContent();

}
