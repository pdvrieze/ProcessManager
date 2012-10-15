package nl.adaptivity.darwin.gwt.client;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.XMLParser;


public class PresentationGroupPanel extends Composite implements RequestCallback {

  private static final String PRESENTATION_GROUP_LOCATION = "/common/wsgroup.php";
  private static PresentationGroupPanelUiBinder uiBinder = GWT.create(PresentationGroupPanelUiBinder.class);

  interface PresentationGroupPanelUiBinder extends UiBinder<Widget, PresentationGroupPanel> {}

  public PresentationGroupPanel() {
    initWidget(uiBinder.createAndBindUi(this));
  }

  @UiField
  HTMLPanel contentHolder;

  public PresentationGroupPanel(String firstName) {
    initWidget(uiBinder.createAndBindUi(this));
    RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, PRESENTATION_GROUP_LOCATION);
    try {
      rb.sendRequest(null, this);
    } catch (RequestException e) {
      contentHolder.getElement().setInnerHTML("Failure trying to send request for group info<br/>"+e.getMessage());
    }
  }

  @Override
  public void onResponseReceived(Request pRequest, Response pResponse) {
    Document xmlResponse = XMLParser.parse(pResponse.getText());
    Element root = xmlResponse.getDocumentElement();
    if ("candidates".equals(XMLUtil.localName(root.getNodeName()))) {
      addCandidatePanel(root);
    } else if ("group".equals(XMLUtil.localName(root.getNodeName()))) {
      addGroupPanel(root);
    } else {
      contentHolder.getElement().setInnerHTML("Response to group info query not understood");
    }
  }

  private void addCandidatePanel(Element pCandidates) {

    // TODO Auto-generated method stub

  }

  @Override
  public void onError(Request pRequest, Throwable pException) {
    contentHolder.getElement().setInnerHTML("Failure getting info<br/>"+pException.getMessage());
  }

}
