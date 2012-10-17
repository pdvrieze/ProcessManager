package nl.adaptivity.darwin.gwt.client;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;


public class PresentationGroupPanel extends Composite implements RequestCallback {




  private static class Slot {

    private final long aHSlot;
    private final String aDescription;

    public Slot(long pHSlot, String pDescription) {
      aHSlot = pHSlot;
      aDescription = pDescription;
    }

    public long getHSlot() {
      return aHSlot;
    }

    public String getDescription() {
      return aDescription;
    }

  }

  private static class CandidateCell extends AbstractCell<Candidate> {

    @Override
    public void render(com.google.gwt.cell.client.Cell.Context pContext, Candidate pValue, SafeHtmlBuilder pSb) {
      pSb.appendHtmlConstant("<div class=\"usercell\"><span class=\"fullname\">");
      pSb.appendEscaped(pValue.getFullname());
      pSb.appendHtmlConstant("</span> <span class=\"username\">(");
      pSb.appendEscaped(pValue.getUsername());
      pSb.appendHtmlConstant(")</span></div>");
    }

  }

  private static class Candidate {

    private final String aUsername;
    private final String aFullname;

    public Candidate(String pUsername, String pFullname) {
      aUsername = pUsername;
      aFullname = pFullname;
    }

    public String getUsername() {
      return aUsername;
    }

    public String getFullname() {
      return aFullname;
    }

  }

  private static final String PRESENTATION_GROUP_LOCATION = "/common/wsgroup.php";
  private static final String SLOT_LOCATION = "/common/wsslots.php";
  private static PresentationGroupPanelUiBinder uiBinder = GWT.create(PresentationGroupPanelUiBinder.class);

  interface PresentationGroupPanelUiBinder extends UiBinder<Widget, PresentationGroupPanel> {/* uibinder */}

  @UiField
  HTMLPanel contentHolder;
  private ArrayList<Candidate> aCandidates;
  private CellList<Candidate> aCandidateList;
  private Button aInviteButton;
  private Label aFeedbackLabel;
  private MultiSelectionModel<Candidate> aCandidateSelectionModel;
  private String aUsername;
  private long aGroupHandle;
  private String aSlotDesc;
  private long aSlotHandle;
  private String aSlotDate;
  private TextBox aTopicEditBox;
  private ScrollPanel aSlotChoiceContainer;

  public PresentationGroupPanel() {
    initWidget(uiBinder.createAndBindUi(this));
    RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, PRESENTATION_GROUP_LOCATION);
    try {
      rb.sendRequest(null, this);
    } catch (RequestException e) {
      contentHolder.getElement().setInnerHTML("Failure trying to send request for group info<br/>"+e.getMessage());
    }
  }

  public void setActiveUserName(String pUsername) {
    aUsername = pUsername;
  }

  @Override
  public void onResponseReceived(Request pRequest, Response pResponse) {
    Document xmlResponse = XMLParser.parse(pResponse.getText());
    Element root = xmlResponse.getDocumentElement();
    if ("candidates".equals(XMLUtil.localName(root.getNodeName()))) {
      addCandidatePanel(root);
    } else if ("group".equals(XMLUtil.localName(root.getNodeName()))) {
      addGroupPanel(root);
    } else if ("error".equals(XMLUtil.localName(root.getNodeName()))) {
      if (aFeedbackLabel!=null) {
        aFeedbackLabel.setText("Error setting group: "+XMLUtil.getTextChildren(root));
      }
    } else {
      contentHolder.clear();
      contentHolder.getElement().setInnerHTML("Response to group info query not understood");
    }
  }

  private void addCandidatePanel(Element pCandidates) {
    contentHolder.clear();
    contentHolder.getElement().setInnerHTML("<h3>You are not in a group yet!</h3>\n<div>Please use the control key to select all group members</div>\n");

    aCandidates = new ArrayList<Candidate>();
    for(Node candidate=pCandidates.getFirstChild(); candidate!=null; candidate = candidate.getNextSibling()) {
      if (XMLUtil.isLocalPart("candidate", candidate)) {
        Element elem = (Element) candidate;
        String username = XMLUtil.getAttributeValue(elem, "user");
        String fullname = XMLUtil.getTextChildren(elem);
        aCandidates.add(new Candidate(username, fullname));
      }
    }
    aCandidateList = new CellList<Candidate>(new CandidateCell());
    aCandidateList.setRowCount(aCandidates.size(), true); // We are exact
    aCandidateList.setRowData(aCandidates);
    aCandidateList.setPageSize(30);
    aCandidateList.setTitle("Select a presentation group mate");
    aCandidateSelectionModel = new MultiSelectionModel<Candidate>();
    aCandidateList.setSelectionModel(aCandidateSelectionModel);
    for(Candidate c:aCandidates) {
      if (c.getUsername().equals(aUsername)) {
        aCandidateSelectionModel.setSelected(c, true);
      }
    }

    ScrollPanel scrollPanel = new ScrollPanel(aCandidateList);
    scrollPanel.setHeight("30em");

    contentHolder.add(scrollPanel);

    if (aFeedbackLabel==null) {
      aFeedbackLabel = new Label();
    }
    contentHolder.add(aFeedbackLabel);


    FlowPanel buttonPanel = new FlowPanel();
    aInviteButton = new Button("Form group");
    buttonPanel.add(aInviteButton);
    contentHolder.add(buttonPanel);
    aInviteButton.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent pEvent) {
        pEvent.stopPropagation();
        inviteClicked(pEvent);
      }
    });

  }

  protected void inviteClicked(ClickEvent pEvent) {
    if (aCandidateSelectionModel.getSelectedSet().size()==0) {
      aFeedbackLabel.setText("Please select your group");
      return;
    }
    SafeHtmlBuilder group = new SafeHtmlBuilder();
    group.appendHtmlConstant("<group>\n");
    for(Candidate c:aCandidateSelectionModel.getSelectedSet()) {
      group.appendHtmlConstant("<member>");
      group.appendEscaped(c.getUsername());
      group.appendHtmlConstant("</member>\n");
    }

    group.appendHtmlConstant("</group>\n");

    RequestBuilder rb=new RequestBuilder(RequestBuilder.POST, PRESENTATION_GROUP_LOCATION);
    rb.setHeader("Content-Type", "text/xml");
    try {
      rb.sendRequest(group.toSafeHtml().asString(), this);
    } catch (RequestException e) {
      aFeedbackLabel.setText("Failure to submit group change!");
      GWT.log("Failure to submit group change", e);
    }
  }

  private void addGroupPanel(Element pGroup) {
    aCandidates = new ArrayList<Candidate>();

    aGroupHandle = XMLUtil.getLongAttr(pGroup, "handle", -1);
    aSlotHandle = XMLUtil.getLongAttr(pGroup, "slot", -1);
    if (aSlotHandle>=0) {
      aSlotDesc = pGroup.getAttribute("slotdesc");
      aSlotDate = pGroup.getAttribute("slotdate");
    }

    SafeHtmlBuilder members = new SafeHtmlBuilder();
    members.appendHtmlConstant("<h3>You have selected your group</h3>\n<div class=\"wsgroup\">Your group consists of: ");
    boolean first = true;

    for(Node member=pGroup.getFirstChild(); member!=null; member = member.getNextSibling()) {
      if (XMLUtil.isLocalPart("member", member)) {
        Element elem = (Element) member;
        String fullname = XMLUtil.getAttributeValue(elem, "fullname");
        String username = XMLUtil.getTextChildren(elem);
        if (first) { first = false; } else {
          members.appendHtmlConstant(", ");
        }
        members.appendHtmlConstant("<span class=\"fullname\">");
        members.appendEscaped(fullname);
        members.appendHtmlConstant("</span> (<span class=\"username\">");
        members.appendEscaped(username);
        members.appendHtmlConstant(")</span>\n");
      }
    }
    members.appendHtmlConstant("</div>");

    contentHolder.clear();
    contentHolder.getElement().setInnerHTML(members.toSafeHtml().asString());

    VerticalPanel vpanel = new VerticalPanel();
    Label label1 = new Label("Please update your group information");
    label1.setStyleName("groupeditpanelhead");
    vpanel.add(label1);

    FlowPanel topicedit = new FlowPanel();
    Label topicLabel = new Label("Presentation topic");
    aTopicEditBox = new TextBox();
    topicedit.add(topicLabel);
    topicedit.add(aTopicEditBox);
    vpanel.add(topicedit);

    Label slotChoiceLabel = new Label("Choose your slot");
    slotChoiceLabel.setStyleName("groupeditpanelhead");
    vpanel.add(slotChoiceLabel);

    aSlotChoiceContainer = new ScrollPanel();
    aSlotChoiceContainer.setHeight("30em");
    aSlotChoiceContainer.setWidget(new WaitWidget());
    vpanel.add(aSlotChoiceContainer);

    RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, SLOT_LOCATION);

    try {
      rb.sendRequest(null, new RequestCallback() {

        @Override
        public void onResponseReceived(Request pRequest, Response pResponse) {
          handleSlotResponse(pRequest, pResponse);
        }

        @Override
        public void onError(Request pRequest, Throwable pException) {
          GWT.log("Error getting available slots", pException);
          aSlotChoiceContainer.setWidget(new Label("Failure to get available slots"));
        }
      });
    } catch (RequestException e) {
      GWT.log("Error getting available slots", e);
      aSlotChoiceContainer.setWidget(new Label("Failure to get available slots"));
    }


  }

  protected void handleSlotResponse(Request pRequest, Response pResponse) {
    if (pResponse.getStatusCode()<200 || pResponse.getStatusCode()>=300) {
      aSlotChoiceContainer.setWidget(new Label("Failure to get available slots ("+pResponse.getStatusCode()+": "+pResponse.getStatusText()+")"));
      return;
    }
    Document xmlResponse = XMLParser.parse(pResponse.getText());
    Element root = xmlResponse.getDocumentElement();

    List<Slot> slots = new ArrayList<Slot>();
    for(Node member=root.getFirstChild(); member!=null; member = member.getNextSibling()) {
      if (XMLUtil.isLocalPart("slot", member)) {
        Element elem = (Element) member;
        long hSlot = XMLUtil.getLongAttr(elem, "handle", -1);
        String description = XMLUtil.getAttributeValue(elem, "description");
        slots.add(new Slot(hSlot, description));
      }
    }



  }

  @Override
  public void onError(Request pRequest, Throwable pException) {
    contentHolder.getElement().setInnerHTML("Failure getting info<br/>"+pException.getMessage());
  }

}
