package nl.adaptivity.darwin.gwt.client;

import nl.adaptivity.gwt.base.client.Clickable;
import nl.adaptivity.gwt.base.client.CompletionListener;
import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.*;


public class Darwin implements EntryPoint {

  private static final String LOGIN_LOCATION = "/accounts/login.php";

  private class LoginReceivedCallback implements RequestCallback {

    @Override
    public void onResponseReceived(final Request pRequest, final Response pResponse) {
      final String text = pResponse.getText();
      final int cpos = text.indexOf(':');
      String result, payload;
      if (cpos >= 0) {
        result = text.substring(0, cpos);
        payload = text.substring(cpos + 1);
      } else {
        result = text;
        payload = null;
      }

      if ("login".equals(result) && (payload != null)) {
        aUsername = payload;
        closeDialogs();
        updateLoginPanel();
        requestRefreshMenu();
      } else if ("logout".equals(result)) {
        aUsername = null;
        closeDialogs();
        updateLoginPanel();
        requestRefreshMenu();
      } else if ("error".equals(result)) {
        closeDialogs();
        error("Error validating login: " + payload, null);
      } else if ("invalid".equals(result)) {
        updateDialogTitle("Log in - Credentials invalid");
        aLoginContent.password.setValue("");
      } else {
        closeDialogs();
        error("Invalid response received from login form :" + pResponse.getStatusCode(), null);
      }

    }

    @Override
    public void onError(final Request pRequest, final Throwable pException) {
      error("Error validating credentials:" + pException.getMessage(), pException);
    }

  }

  private class LoginHandler implements ClickHandler {

    @Override
    public void onClick(final ClickEvent pEvent) {
      pEvent.stopPropagation(); // We handle the propagation.
      final com.google.gwt.user.client.Element dialogBase = dialogContent.getElement();
      final InputElement username = InputElement.as(XMLUtil.descendentWithAttribute(dialogBase, "name", "username"));
      final InputElement password = InputElement.as(XMLUtil.descendentWithAttribute(dialogBase, "name", "password"));

      RequestBuilder rBuilder;
      rBuilder = new RequestBuilder(RequestBuilder.POST, LOGIN_LOCATION);
      rBuilder.setHeader("Accept", "text/plain");
      rBuilder.setHeader("Content-Type", "application/x-www-form-urlencoded");
      final String postData = "username=" + URL.encode(username.getValue()) + "&password=" + URL.encode(password.getValue());
      try {
        rBuilder.sendRequest(postData, new LoginReceivedCallback());
      } catch (final RequestException e) {
        error("Could not send login request", e);
        closeDialogs();
      }

    }

  }

  private class DialogCloseHandler implements ClickHandler {

    @Override
    public void onClick(final ClickEvent pEvent) {
      closeDialogs();
    }

  }

  private class LoginoutClickHandler implements ClickHandler {

    @Override
    public void onClick(final ClickEvent pEvent) {
      if (aUsername == null) {
        loginDialog();
        // Login
      } else {
        try {
          RequestBuilder rBuilder;
          rBuilder = new RequestBuilder(RequestBuilder.GET, "/accounts/logout");
          rBuilder.setHeader("Accept", "application/binary");
          rBuilder.sendRequest(null, new LoginReceivedCallback());
        } catch (final RequestException e) {
          error("Could not log out", e);
        }
      }
    }

  }

  private class HistoryChangeHandler implements ValueChangeHandler<String> {

    @Override
    public void onValueChange(final ValueChangeEvent<String> pEvent) {
      final String newValue = pEvent.getValue();
      navigateTo(newValue, false);
    }

  }

  private class MenuClickHandler implements ClickHandler {

    @Override
    public void onClick(final ClickEvent pEvent) {
      final EventTarget target = pEvent.getNativeEvent().getEventTarget();
      navigateTo(target.<Element> cast().getAttribute("href"), true);
    }

  }

  private class MenuReceivedCallback implements RequestCallback {

    @Override
    public void onResponseReceived(final Request pRequest, final Response pResponse) {
      final String text = pResponse.getText();
      aMenu.setInnerHTML(text);
      updateMenuElements();
    }

    @Override
    public void onError(final Request pRequest, final Throwable pException) {
      log("Error updating the menu", pException);
    }

  }

  @UiTemplate("darwindialog.ui.xml")
  interface DarwinDialogBinder extends UiBinder<Widget, Darwin> { /*
                                                                   * gwt
                                                                   * generated
                                                                   */};

  private static DarwinDialogBinder darwinDialogBinder = GWT.create(DarwinDialogBinder.class);

  //  interface DarwinUiBinder extends UiBinder<Widget, Darwin> { /* Dynamic gwt */}

  private DivElement aMenu;

  private String aLocation;

  private String aUsername;

  private RootPanel aContentPanel;

  private ResizeLayoutPanel aContentLayoutPanel;

  private ClickHandler aMenuClickHandler;

  @UiField
  Label dialogTitle;

  @UiField
  FlowPanel dialogContent;

  private ClickHandler aDialogCloseHandler;

  private LoginContent aLoginContent;

  private HandlerRegistration aLoginoutRegistration;

  private Element aBanner;

  @Override
  public void onModuleLoad() {
    final String initToken = History.getToken();
    if (initToken.length() == 0) {
      History.newItem("/", false);
    }


    final Document document = Document.get();
    aMenu = (DivElement) document.getElementById("menu");
    aLocation = "/";
    final Element usernameSpan = document.getElementById("username");
    if (usernameSpan != null) {
      aUsername = usernameSpan.getInnerText();
    } else {
      aUsername = null;
    }
    aContentPanel = RootPanel.get("content");

    updateContentTab();

    requestRefreshMenu();

    updateLogin(document);

    History.addValueChangeHandler(new HistoryChangeHandler());

    aBanner = document.getElementById("banner");

    hideBanner();
  }

  private void hideBanner() {
    // Remove the banner
    if (aBanner != null) {
      aBanner.setAttribute("style", "display:none");
    }
  }

  private void showBanner() {
    if (aBanner != null) {
      aBanner.removeAttribute("style");
    }
  }

  public void error(final String pMessage, final Throwable pException) {
    GWT.log("Error: " + pMessage, pException);
    String message;
    if (pException == null) {
      message = pMessage;
    } else {
      message = pMessage + "<br />" + pException.getMessage();
    }
    modalDialog(message);
  }

  private void loginDialog() {
    aLoginContent = new LoginContent();
    if (aDialogCloseHandler == null) {
      aDialogCloseHandler = new DialogCloseHandler();
    }
    dialog("Log in", aLoginContent);
    final Clickable cancel = Clickable.wrapNoAttach(aLoginContent.cancel);

    // This must be after dialog, otherwise cancelButton will not be attached (and can not get a handler)
    cancel.addClickHandler(aDialogCloseHandler);

    final Clickable login = Clickable.wrapNoAttach(aLoginContent.login);
    login.addClickHandler(new LoginHandler());
    //    aLoginContent.redirect.setValue(Window.Location.getHref());
  }

  private void modalDialog(final String pString) {
    final Button closeButton = new Button("Ok");
    closeButton.addClickHandler(aDialogCloseHandler);
    dialog("Message", new Label(pString), closeButton);
  }

  private void updateDialogTitle(final String pString) {
    dialogTitle.setText(pString);
  }

  private void dialog(final String pTitle, final Widget... pContents) {
    final Widget dialog = darwinDialogBinder.createAndBindUi(this);
    dialogTitle.setText(pTitle);
    RootPanel.get();
    for (final Widget w : pContents) {
      dialogContent.add(w);
    }
    aContentPanel.add(dialog);
  }

  private void closeDialogs() {
    for (int i = 0; i < aContentPanel.getWidgetCount(); ++i) {
      final Widget w = aContentPanel.getWidget(i);
      final String[] styleNames = w.getStyleName().split(" ");
      for (final String styleName : styleNames) {
        if ("dialog".equals(styleName)) {
          w.removeFromParent();
          --i; // Decrease so adding will move us to our current position.
          break;
        }
      }
    }
  }

  public void navigateTo(final String pNewLocation, final boolean addHistory) {
    if (!aLocation.equals(pNewLocation)) {
      aLocation = pNewLocation;
      updateContentTab();
      if (addHistory) {
        History.newItem(pNewLocation, false);
      }
    }
  }

  private void updateContentTab() {
    if (aLocation.equals("/")) {
      setInboxPanel();
    } else if (aLocation.equals("/actions")) {
      setActionPanel();
    } else if (aLocation.equals("/processes")) {
      setProcessesPanel();
    } else if (aLocation.equals("/about")) {
      setAboutPanel();
    }
  }

  private void setInboxPanel() {
    GWT.runAsync(new RunAsyncCallback() {

      @Override
      public void onSuccess() {
        aContentPanel.clear();
        aContentPanel.add(new Label("Inbox Panel"));
        aContentLayoutPanel = null;
      }

      @Override
      public void onFailure(final Throwable pReason) {
        aContentPanel.clear();
        aContentPanel.add(new Label("Could not load inbox panel module"));
      }
    });
  }

  private void setProcessesPanel() {
    GWT.runAsync(new RunAsyncCallback() {

      @Override
      public void onSuccess() {
        aContentPanel.clear();
        aContentPanel.add(new AboutPanel());
      }

      @Override
      public void onFailure(final Throwable pReason) {
        aContentPanel.clear();
        aContentPanel.add(new Label("Could not load about page module"));
      }
    });
  }

  private void setActionPanel() {
    ActionPanel.load(getCompletionListener());

    GWT.runAsync(new RunAsyncCallback() {

      @Override
      public void onSuccess() {
        aContentPanel.clear();
        aContentPanel.add(new Label("Action Panel"));
      }

      @Override
      public void onFailure(final Throwable pReason) {
        aContentPanel.clear();
        aContentPanel.add(new Label("Could not load action panel module"));
      }
    });
  }

  private CompletionListener getCompletionListener() {
    return new CompletionListener() {

      @Override
      public void onCompletion(final UIObject pWidget) {
        hideBanner();
        if (pWidget instanceof Widget) {
          aContentPanel.add((Widget) pWidget);
        } else {
          aContentPanel.getElement().appendChild(pWidget.getElement());
        }
      }
    };
  }

  private void setAboutPanel() {
    GWT.runAsync(new RunAsyncCallback() {

      @Override
      public void onSuccess() {
        aContentPanel.clear();
        aContentPanel.add(new AboutPanel());
      }

      @Override
      public void onFailure(final Throwable pReason) {
        aContentPanel.clear();
        aContentPanel.add(new Label("Could not load about page module"));
      }
    });
  }


  /**
   * Make the menu elements active and add an onClick Listener.
   */
  public void updateMenuElements() {
    if (aMenuClickHandler == null) {
      aMenuClickHandler = new MenuClickHandler();
    }
    for (Element item = aMenu.getFirstChildElement(); item != null; item = item.getNextSiblingElement()) {
      final InlineLabel l = InlineLabel.wrap(item);
      l.addClickHandler(aMenuClickHandler);
    }
  }

  private void updateLogin(final Document document) {
    final Clickable loginout = Clickable.wrap(document.getElementById("logout"));
    loginout.getElement().removeAttribute("href");
    aLoginoutRegistration = loginout.addClickHandler(new LoginoutClickHandler());
  }

  private void updateLoginPanel() {
    aLoginoutRegistration.removeHandler();
    final Document document = Document.get();

    final DivElement loginPanel = document.getElementById("login").cast();
    if (aUsername != null) {
      loginPanel.setInnerHTML("<span id=\"username\">" + aUsername + "</span><a id=\"logout\">logout</a>");
    } else {
      loginPanel.setInnerHTML("<a id=\"logout\">login</a>");
    }
    final Clickable loginout = Clickable.wrap(document.getElementById("logout"));
    aLoginoutRegistration = loginout.addClickHandler(new LoginoutClickHandler());

  }


  private void requestRefreshMenu() {
    RequestBuilder rBuilder;
    rBuilder = new RequestBuilder(RequestBuilder.GET, "/common/menu.php?location=" + URL.encode(aLocation));
    try {
      rBuilder.sendRequest(null, new MenuReceivedCallback());
    } catch (final RequestException e) {
      log("Could not update menu", e);
    }
  }

  private void log(final String pMessage, final Throwable pThrowable) {
    GWT.log(pMessage, pThrowable);
  }

  private void log(final String pMessage) {
    GWT.log(pMessage);
  }

}
