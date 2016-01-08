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

    private final String mNewValue;

    public InputCompleteEvent(final String newValue, final boolean success) {
      if (success) {
        mNewValue = newValue;
      } else {
        mNewValue = null;
      }
    }

    public static Type<InputCompleteHandler> getType() {
      if (TYPE == null) {
        TYPE = new Type<InputCompleteHandler>();
      }
      return TYPE;
    }

    @Override
    protected void dispatch(final InputCompleteHandler handler) {
      handler.onComplete(this);
    }

    @Override
    public Type<InputCompleteHandler> getAssociatedType() {
      return getType();
    }

    public String getNewValue() {
      return mNewValue;
    }

    public boolean isSuccess() {
      return mNewValue != null;
    }

  }

  public interface InputCompleteHandler extends EventHandler {

    void onComplete(InputCompleteEvent inputCompleteEvent);

  }

  private static final int BUTTONWIDTH = 100;

  private static final int HEIGHT = 200;

  private static final int WIDTH = 300;

  private Button mOkButton;

  private Button mCancelButton;

  private TextBox mInputField;

  private PopupState mState = PopupState.INITIALISED;

  public TextInputPopup(final String query, final String okButtonLabel) {
    super(true, true);

    setWidth(WIDTH + "px");
    setHeight(HEIGHT + "px");
    final int x = (Window.getClientWidth() - WIDTH) / 2;
    final int y = (Window.getClientHeight() - HEIGHT) / 2;
    setPopupPosition(x, y);
    final Widget content = getContentWidget(query, okButtonLabel);
    setWidget(content);

    mInputField.addKeyPressHandler(this);
  }

  private Widget getContentWidget(final String query, final String okButtonLabel) {
    final AbsolutePanel mainPanel = new AbsolutePanel();
    final VerticalPanel mainContent = new VerticalPanel();
    mainContent.add(new Label(query));
    mInputField = new TextBox();
    mainContent.add(mInputField);
    {
      final int offsetWidth = (WIDTH * 2) / 3;
      mInputField.setWidth(offsetWidth + "px");
      final int x = (WIDTH - offsetWidth) / 2;
      final int offsetHeight = 38;
      GWT.log("OffsetWidth: " + offsetWidth + " OffsetHeight: " + offsetHeight, null);
      final int y = (HEIGHT - offsetHeight) / 2;
      mainPanel.add(mainContent, x, y);
    }

    final HorizontalPanel buttonPanel = new HorizontalPanel();
    mOkButton = new Button(okButtonLabel);
    mOkButton.setWidth(BUTTONWIDTH + "px");
    mOkButton.addClickHandler(this);
    mCancelButton = new Button("Cancel");
    mCancelButton.setWidth(BUTTONWIDTH + "px");
    mCancelButton.addClickHandler(this);
    buttonPanel.add(mOkButton);
    buttonPanel.add(mCancelButton);

    {
      final int x = WIDTH - (2 * BUTTONWIDTH) - 5;
      final int y = HEIGHT - 28;
      mainPanel.add(buttonPanel, x, y);
    }

    return mainPanel;
  }

  @Override
  public void show() {
    mState = PopupState.ACTIVE;
    super.show();
    mInputField.setFocus(true);
  }

  @Override
  public void onClick(final ClickEvent event) {
    if (event.getSource() == mOkButton) {
      onComplete();
    } else if (event.getSource() == mCancelButton) {
      onCancel();
    }
  }

  private void onComplete() {
    mState = PopupState.COMPLETE;
    hide();
    fireRenameHandler(mInputField.getValue(), true);
  }

  private void onCancel() {
    mState = PopupState.CANCELLED;
    hide();
    fireRenameHandler(mInputField.getValue(), false);
  }

  @Override
  public void onKeyPress(final KeyPressEvent event) {
    if (event.getSource() == mInputField) {
      if (event.getCharCode() == KeyCodes.KEY_ENTER) {
        onComplete();
      } else if (event.getCharCode() == KeyCodes.KEY_ESCAPE) {
        onCancel();
      }
    }
  }

  public String getValue() {
    return mInputField.getValue();
  }

  private void fireRenameHandler(final String newValue, final boolean success) {
    fireEvent(new InputCompleteEvent(newValue, success));
  }

  public HandlerRegistration addInputCompleteHandler(final InputCompleteHandler handler) {
    return addHandler(handler, InputCompleteEvent.getType());
  }

  public PopupState getState() {
    return mState;
  }

}
