package nl.adaptivity.darwin.gwt.client;

import java.util.ArrayList;
import java.util.List;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.*;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;


public class PresentationGroupPanel extends Composite implements RequestCallback {






  private class SlotUpdateHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {
      updateSlotClicked();
    }

  }

  private static class SlotCell extends AbstractCell<Slot> {

    @Override
    public void render(com.google.gwt.cell.client.Cell.Context context, Slot value, SafeHtmlBuilder sb) {
      sb.appendHtmlConstant("<div class='slotcell'>");
      sb.appendEscaped(value.getDescription());
      sb.appendHtmlConstant("</div>");
    }

  }

  private static class Slot {

    private final long mHSlot;
    private final String mDescription;

    public Slot(long hSlot, String description) {
      mHSlot = hSlot;
      mDescription = description;
    }

    public long getHSlot() {
      return mHSlot;
    }

    public String getDescription() {
      return mDescription;
    }

  }

  private static class CandidateCell extends AbstractCell<Candidate> {

    @Override
    public void render(com.google.gwt.cell.client.Cell.Context context, Candidate value, SafeHtmlBuilder sb) {
      sb.appendHtmlConstant("<div class=\"usercell\"><span class=\"fullname\">");
      sb.appendEscaped(value.getFullname());
      sb.appendHtmlConstant("</span> <span class=\"username\">(");
      sb.appendEscaped(value.getUsername());
      sb.appendHtmlConstant(")</span></div>");
    }

  }

  private static class Candidate {

    private final String mUsername;
    private final String mFullname;

    public Candidate(String username, String fullname) {
      mUsername = username;
      mFullname = fullname;
    }

    public String getUsername() {
      return mUsername;
    }

    public String getFullname() {
      return mFullname;
    }

  }

  private static final String PRESENTATION_GROUP_LOCATION = "/common/wsgroup.php";
  private static final String SLOT_LOCATION = "/common/wsslots.php";
  private static PresentationGroupPanelUiBinder uiBinder = GWT.create(PresentationGroupPanelUiBinder.class);

  interface PresentationGroupPanelUiBinder extends UiBinder<Widget, PresentationGroupPanel> {/* uibinder */}

  @UiField
  HTMLPanel contentHolder;
  private ArrayList<Candidate> mCandidates;
  private CellList<Candidate> mCandidateList;
  private Button mInviteButton;
  private Label mFeedbackLabel;
  private MultiSelectionModel<Candidate> mCandidateSelectionModel;
  private String mUsername;
  private long mGroupHandle;
  private String mSlotDesc;
  private long mSlotHandle;
  private String mSlotDate;
  private TextBox mTopicEditBox;
  private ScrollPanel mSlotChoiceContainer;
  private HandlerRegistration mUpdateSlotHandlerRegistration;
  private SingleSelectionModel<Slot> mSlotSelectionModel;
  public PresentationGroupPanel() {
    initWidget(uiBinder.createAndBindUi(this));
    RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, PRESENTATION_GROUP_LOCATION);
    try {
      rb.sendRequest(null, this);
    } catch (RequestException e) {
      contentHolder.getElement().setInnerHTML("Failure trying to send request for group info<br/>"+e.getMessage());
    }
  }

  public void setActiveUserName(String username) {
    mUsername = username;
  }

  @Override
  public void onResponseReceived(Request request, Response response) {
    Document xmlResponse = XMLParser.parse(response.getText());
    Element root = xmlResponse.getDocumentElement();
    if ("candidates".equals(XMLUtil.localName(root.getNodeName()))) {
      addCandidatePanel(root);
    } else if ("group".equals(XMLUtil.localName(root.getNodeName()))) {
      addGroupPanel(root);
    } else if ("error".equals(XMLUtil.localName(root.getNodeName()))) {
      if (mFeedbackLabel!=null) {
        mFeedbackLabel.setText("Error setting group: "+XMLUtil.getTextChildren(root));
      }
    } else {
      contentHolder.clear();
      contentHolder.getElement().setInnerHTML("Response to group info query not understood");
    }
  }

  private void addCandidatePanel(Element candidates) {
    contentHolder.clear();
    contentHolder.getElement().setInnerHTML("<h3>You are not in a group yet!</h3>\n<div style=\"margin-bottom: 1ex\">Please use the control key to select all group members (you have already been selected)</div>\n");

    mCandidates = new ArrayList<Candidate>();
    for(Node candidate=candidates.getFirstChild(); candidate!=null; candidate = candidate.getNextSibling()) {
      if (XMLUtil.isLocalPart("candidate", candidate)) {
        Element elem = (Element) candidate;
        String username = XMLUtil.getAttributeValue(elem, "user");
        String fullname = XMLUtil.getTextChildren(elem);
        mCandidates.add(new Candidate(username, fullname));
      }
    }
    mCandidateList = new CellList<Candidate>(new CandidateCell());
    mCandidateList.setRowCount(mCandidates.size(), true); // We are exact
    mCandidateList.setRowData(mCandidates);
    mCandidateList.setTitle("Select a presentation group mate");
    mCandidateSelectionModel = new MultiSelectionModel<Candidate>();
    mCandidateList.setSelectionModel(mCandidateSelectionModel);
    for(Candidate c:mCandidates) {
      if (c.getUsername().equals(mUsername)) {
        mCandidateSelectionModel.setSelected(c, true);
      }
    }

    ScrollPanel scrollPanel = new ScrollPanel(mCandidateList);
    scrollPanel.setStylePrimaryName("userchoicescrollcontainer");

    contentHolder.add(scrollPanel);

    if (mFeedbackLabel==null) {
      mFeedbackLabel = new Label();
    }
    contentHolder.add(mFeedbackLabel);


    FlowPanel buttonPanel = new FlowPanel();
    mInviteButton = new Button("Form group");
    buttonPanel.add(mInviteButton);
    contentHolder.add(buttonPanel);
    mInviteButton.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        event.stopPropagation();
        inviteClicked(event);
      }
    });

  }

  protected void inviteClicked(ClickEvent event) {
    if (mCandidateSelectionModel.getSelectedSet().size()==0) {
      mFeedbackLabel.setText("Please select your group");
      return;
    }
    SafeHtmlBuilder group = new SafeHtmlBuilder();
    group.appendHtmlConstant("<group>\n");
    for(Candidate c:mCandidateSelectionModel.getSelectedSet()) {
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
      mFeedbackLabel.setText("Failure to submit group change!");
      GWT.log("Failure to submit group change", e);
    }
  }

  private void addGroupPanel(Element group) {
    mCandidates = new ArrayList<Candidate>();

    mGroupHandle = XMLUtil.getLongAttr(group, "handle", -1);
    mSlotHandle = XMLUtil.getLongAttr(group, "slot", -1);
    if (mSlotHandle>=0) {
      mSlotDesc = group.getAttribute("slotdesc");
      mSlotDate = group.getAttribute("slotdate");
    }
    String topic = XMLUtil.getAttributeValue(group, "topic");

    SafeHtmlBuilder members = new SafeHtmlBuilder();
    members.appendHtmlConstant("<h3>You have selected your group</h3>\n<div class=\"wsgroup\">Your group consists of: ");
    boolean first = true;

    for(Node member=group.getFirstChild(); member!=null; member = member.getNextSibling()) {
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
    contentHolder.addStyleName("presentationContentHolder");

    contentHolder.getElement().setInnerHTML(members.toSafeHtml().asString());

    FlowPanel vpanel = new FlowPanel();
    Label label1 = new Label("This is your group information, please update it as needed");
    label1.setStyleName("groupeditpanelhead");
    vpanel.add(label1);

    Label topicLabel = new Label("Presentation topic: ");
    topicLabel.setStyleName("gwttextboxlabel");
    mTopicEditBox = new TextBox();
    mTopicEditBox.setWidth("40em");
    if (topic!=null) {
      mTopicEditBox.setText(topic);
    }
    {
      HorizontalPanel hpanel = new HorizontalPanel();
      hpanel.add(topicLabel);
      hpanel.add(mTopicEditBox);
      vpanel.add(hpanel);
    }

    Label slotChoiceLabel = new Label("Choose your slot");
    slotChoiceLabel.setStyleName("groupeditpanelhead");
    vpanel.add(slotChoiceLabel);

    mSlotChoiceContainer = new ScrollPanel();
    mSlotChoiceContainer.setWidget(new WaitWidget());
    mSlotChoiceContainer.setStylePrimaryName("slotchoicescrollcontainer");
    vpanel.add(mSlotChoiceContainer);

    RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, SLOT_LOCATION);

    try {
      rb.sendRequest(null, new RequestCallback() {

        @Override
        public void onResponseReceived(Request request, Response response) {
          handleSlotResponse(response);
        }

        @Override
        public void onError(Request request, Throwable exception) {
          GWT.log("Error getting available slots", exception);
          mSlotChoiceContainer.setWidget(new Label("Failure to get available slots"));
        }
      });
    } catch (RequestException e) {
      GWT.log("Error getting available slots", e);
      mSlotChoiceContainer.setWidget(new Label("Failure to get available slots"));
    }

    Button updateButton = new Button("Update");
    mUpdateSlotHandlerRegistration = updateButton.addClickHandler(new SlotUpdateHandler());

    vpanel.add(updateButton);

    if (mFeedbackLabel==null) {
      mFeedbackLabel = new Label();
    } else {
      mFeedbackLabel.removeFromParent();
    }
    vpanel.add(mFeedbackLabel);

    contentHolder.add(vpanel);
  }

  protected void handleSlotResponse(Response response) {
    if (response.getStatusCode()<200 || response.getStatusCode()>=300) {
      mSlotChoiceContainer.setWidget(new Label("Failure to get available slots ("+response.getStatusCode()+": "+response.getStatusText()+")"));
      return;
    }
    Document xmlResponse = XMLParser.parse(response.getText());
    Element root = xmlResponse.getDocumentElement();

    Slot currentSlot=null;

    List<Slot> slots = new ArrayList<Slot>();
    for(Node member=root.getFirstChild(); member!=null; member = member.getNextSibling()) {
      if (XMLUtil.isLocalPart("slot", member)) {
        Element elem = (Element) member;
        long hSlot = XMLUtil.getLongAttr(elem, "handle", -1);
        String description = XMLUtil.getAttributeValue(elem, "description");
        Slot slot = new Slot(hSlot, description);
        if (hSlot==mSlotHandle) {
          currentSlot = slot;
        }
        slots.add(slot);
      }
    }

    CellList<Slot> slotChoiceList = new CellList<PresentationGroupPanel.Slot>(new SlotCell());
    mSlotSelectionModel = new SingleSelectionModel<Slot>();
    slotChoiceList.setSelectionModel(mSlotSelectionModel);
    slotChoiceList.setRowData(slots);
    if (currentSlot!=null) { mSlotSelectionModel.setSelected(currentSlot, true); }



    mSlotChoiceContainer.setWidget(slotChoiceList);

  }

  public void updateSlotClicked() {
    Slot selected = mSlotSelectionModel.getSelectedObject();

    SafeHtmlBuilder data = new SafeHtmlBuilder();
    if (selected==null) {
      data.appendHtmlConstant("<slot>");
    } else {
      data.appendHtmlConstant("<slot handle=\""+selected.getHSlot()+"\">");
    }
    data.appendEscaped(mTopicEditBox.getText())
        .appendHtmlConstant("</slot>");

    RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, SLOT_LOCATION);
    rb.setHeader("Content-Type", "text/xml");
    try {
      rb.sendRequest(data.toSafeHtml().asString(), new RequestCallback() {

        @Override
        public void onResponseReceived(Request request, Response response) {
          Document xmlResponse = XMLParser.parse(response.getText());
          Element root = xmlResponse.getDocumentElement();
          if (XMLUtil.isLocalPart("error", root)) {
            mFeedbackLabel.setText("Error: "+XMLUtil.getTextChildren(root));
          } else if (XMLUtil.isLocalPart("group", root)) {
            mTopicEditBox.setText(XMLUtil.getAttributeValue(root, "topic"));
            mSlotHandle = XMLUtil.getLongAttr(root, "slot", -1);
            mFeedbackLabel.setText("Information updated");
          }
       }

        @Override
        public void onError(Request request, Throwable exception) {
          mFeedbackLabel.setText("Error requesting update: "+exception.getMessage());
          GWT.log("Error requesting update", exception);
        }
      });
    } catch (RequestException e) {
      mFeedbackLabel.setText("Error requesting update: "+e.getMessage());
      GWT.log("Error requesting update", e);
    }
  }

  @Override
  public void onError(Request request, Throwable exception) {
    contentHolder.getElement().setInnerHTML("Failure getting info<br/>"+exception.getMessage());
  }

}
