package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.xml.client.XMLParser;


public class ProcessEditPanel extends Composite {

  private boolean aEditInstance;
  private ProcessModel aProcessModel;
  private ProcessInstance aProcessInstance;

  public ProcessEditPanel() {
    // TODO actual content
    Label label = new Label("ProcessEditPanel");
    initWidget(label);
  }

  public void setInstance(boolean pInstance) {
    aEditInstance = pInstance;
  }

  public void reset() {
    aProcessModel = null;
    aProcessInstance = null;
    // TODO Reset visual state
    //
  }

  public void init(Response pResponse) {
    aProcessModel = ProcessModel.fromXml(XMLParser.parse(pResponse.getText()));
    // TODO Auto-generated method stub
    //

  }

}
