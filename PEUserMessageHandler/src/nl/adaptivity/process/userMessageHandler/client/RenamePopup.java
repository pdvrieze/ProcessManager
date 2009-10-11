package nl.adaptivity.process.userMessageHandler.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;


public class RenamePopup extends PopupPanel implements ClickHandler {





  public enum PopupState {
    INITIALISED,
    ACTIVE,
    COMPLETE,
    CANCELLED
  }

  public static class RenameEvent extends GwtEvent<RenameHandler> {
    private static Type<RenameHandler> TYPE;
    private final String aNewValue;

    public RenameEvent(String pNewValue) {
      aNewValue = pNewValue;
    }

    public static Type<RenameHandler> getType() {
      if (TYPE==null) { TYPE = new Type<RenameHandler>(); }
      return TYPE;
    }

    @Override
    protected void dispatch(RenameHandler pHandler) {
      pHandler.onRename(this);
    }

    @Override
    public Type<RenameHandler> getAssociatedType() {
      return getType();
    }

    public String getNewValue() {
      return aNewValue;
    }

  }

  public interface RenameHandler extends EventHandler {

    void onRename(RenameEvent pRenameEvent);

  }

  private static final int BUTTONWIDTH = 100;
  private static final int HEIGHT = 200;
  private static final int WIDTH = 300;
  private final String aName;
  private Button aOkButton;
  private Button aCancelButton;
  private TextBox aInputField;
  private PopupState aState = PopupState.INITIALISED;

  public RenamePopup(String pName) {
    super(true, true);
    aName = pName;

    setWidth("300px");
    setHeight("200px");
    int x = (Window.getClientWidth()-WIDTH)/2;
    int y = (Window.getClientHeight()-HEIGHT)/2;
    setPopupPosition(x, y);
    Widget content = getContentWidget();
    setWidget(content);
  }

  private Widget getContentWidget() {
    AbsolutePanel mainPanel = new AbsolutePanel();
    VerticalPanel mainContent = new VerticalPanel();
    mainContent.add(new Label("Enter new name of the "+aName));
    aInputField = new TextBox();
    mainContent.add(aInputField);
    {
      final int offsetWidth = (WIDTH*2)/3;
      aInputField.setWidth(offsetWidth+"px");
      int x = (WIDTH-offsetWidth)/2;
      final int offsetHeight = 38;
      GWT.log("OffsetWidth: "+offsetWidth + " OffsetHeight: "+offsetHeight, null);
      int y = (HEIGHT-offsetHeight)/2;
      mainPanel.add(mainContent, x, y);
    }

    HorizontalPanel buttonPanel = new HorizontalPanel();
    aOkButton = new Button("Rename");
    aOkButton.setWidth(BUTTONWIDTH+"px");
    aOkButton.addClickHandler(this);
    aCancelButton = new Button("Cancel");
    aCancelButton.setWidth(BUTTONWIDTH+"px");
    aCancelButton.addClickHandler(this);
    buttonPanel.add(aOkButton);
    buttonPanel.add(aCancelButton);

    {
      int x = WIDTH-(2*BUTTONWIDTH)-5;
      int y = HEIGHT-28;
      mainPanel.add(buttonPanel, x, y);
    }

    return mainPanel;
  }

  @Override
  public void show() {
    aState = PopupState.ACTIVE;
    super.show();
  }

  @Override
  public void onClick(ClickEvent pEvent) {
    if (pEvent.getSource()==aOkButton) {
      aState = PopupState.COMPLETE;
      hide();
      fireRenameHandler(aInputField.getValue());
    } else if (pEvent.getSource()==aCancelButton) {
      aState = PopupState.CANCELLED;
      hide();
    }
  }

  public String getValue() {
    return aInputField.getValue();
  }

  private void fireRenameHandler(String pNewValue) {
    fireEvent(new RenameEvent(pNewValue));
  }

  public HandlerRegistration addRenameHandler(RenameHandler pHandler) {
    return addHandler(pHandler, RenameEvent.getType());
  }

  public PopupState getState() {
    return aState ;
  }

}
