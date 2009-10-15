package nl.adaptivity.gwt.ext.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;


public class TextInputPopup extends PopupPanel implements ClickHandler, KeyPressHandler {

  public enum PopupState {
    INITIALISED,
    ACTIVE,
    COMPLETE,
    CANCELLED
  }

  public static class InputCompleteEvent extends GwtEvent<InputCompleteHandler> {
    private static Type<InputCompleteHandler> TYPE;
    private final String aNewValue;

    public InputCompleteEvent(String pNewValue, boolean pSuccess) {
      if (pSuccess) {
        aNewValue = pNewValue;
      } else {
        aNewValue = null;
      }
    }

    public static Type<InputCompleteHandler> getType() {
      if (TYPE==null) { TYPE = new Type<InputCompleteHandler>(); }
      return TYPE;
    }

    @Override
    protected void dispatch(InputCompleteHandler pHandler) {
      pHandler.onComplete(this);
    }

    @Override
    public Type<InputCompleteHandler> getAssociatedType() {
      return getType();
    }

    public String getNewValue() {
      return aNewValue;
    }

    public boolean isSuccess() {
      return aNewValue!=null;
    }

  }

  public interface InputCompleteHandler extends EventHandler {

    void onComplete(InputCompleteEvent pInputCompleteEvent);

  }

  private static final int BUTTONWIDTH = 100;
  private static final int HEIGHT = 200;
  private static final int WIDTH = 300;
  private Button aOkButton;
  private Button aCancelButton;
  private TextBox aInputField;
  private PopupState aState = PopupState.INITIALISED;

  public TextInputPopup(String pQuery, String pOkButtonLabel) {
    super(true, true);

    setWidth(WIDTH+"px");
    setHeight(HEIGHT+"px");
    int x = (Window.getClientWidth()-WIDTH)/2;
    int y = (Window.getClientHeight()-HEIGHT)/2;
    setPopupPosition(x, y);
    Widget content = getContentWidget(pQuery, pOkButtonLabel);
    setWidget(content);

    aInputField.addKeyPressHandler(this);
  }

  private Widget getContentWidget(String pQuery, String pOkButtonLabel) {
    AbsolutePanel mainPanel = new AbsolutePanel();
    VerticalPanel mainContent = new VerticalPanel();
    mainContent.add(new Label(pQuery));
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
    aOkButton = new Button(pOkButtonLabel);
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
    aInputField.setFocus(true);
  }

  @Override
  public void onClick(ClickEvent pEvent) {
    if (pEvent.getSource()==aOkButton) {
      onComplete();
    } else if (pEvent.getSource()==aCancelButton) {
      onCancel();
    }
  }

  private void onComplete() {
    aState = PopupState.COMPLETE;
    hide();
    fireRenameHandler(aInputField.getValue(), true);
  }

  private void onCancel() {
    aState = PopupState.CANCELLED;
    hide();
    fireRenameHandler(aInputField.getValue(), false);
  }

  @Override
  public void onKeyPress(KeyPressEvent pEvent) {
    if (pEvent.getSource()==aInputField) {
      if (pEvent.getCharCode()==KeyCodes.KEY_ENTER) {
        onComplete();
      } else if (pEvent.getCharCode()==KeyCodes.KEY_ESCAPE) {
        onCancel();
      }
    }
  }

  public String getValue() {
    return aInputField.getValue();
  }

  private void fireRenameHandler(String pNewValue, boolean pSuccess) {
    fireEvent(new InputCompleteEvent(pNewValue, pSuccess));
  }

  public HandlerRegistration addInputCompleteHandler(InputCompleteHandler pHandler) {
    return addHandler(pHandler, InputCompleteEvent.getType());
  }

  public PopupState getState() {
    return aState ;
  }

}
