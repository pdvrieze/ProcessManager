package nl.adaptivity.process.engine;

import java.util.Set;

import javax.activation.DataHandler;
import javax.xml.transform.Source;


@Deprecated
public interface NormalizedMessage {

  Source getContent();

  void setContent(Source pResult);

  DataHandler getAttachment(String pKey);

  void addAttachment(String pString, DataHandler pDataHandler);

  void removeAttachment(String pKey);

  Set<String> getAttachmentNames();

}
