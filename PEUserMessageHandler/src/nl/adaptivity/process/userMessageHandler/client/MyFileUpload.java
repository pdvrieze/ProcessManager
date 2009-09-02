package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FileUpload;

/**
 * An extended upload that can have a change handler.
 * @author Paul de Vrieze
 *
 */
public class MyFileUpload extends FileUpload {

  public MyFileUpload() {
    super();
  }

  public MyFileUpload(Element pElement) {
    super(pElement);
  }

  public void addChangeHandler(ChangeHandler pHandler) {
    addDomHandler(pHandler, ChangeEvent.getType());
  }
  
}
