package nl.adaptivity.process.engine;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

import javax.activation.DataHandler;
import javax.xml.transform.Source;


public interface NormalizedMessage {

  @NotNull
  Source getContent();

  void setContent(Source result);

  @NotNull
  DataHandler getAttachment(String key);

  void addAttachment(String string, DataHandler dataHandler);

  void removeAttachment(String key);

  @NotNull
  Set<String> getAttachmentNames();

}
