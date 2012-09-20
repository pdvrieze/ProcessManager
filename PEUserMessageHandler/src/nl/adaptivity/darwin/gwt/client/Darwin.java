package nl.adaptivity.darwin.gwt.client;

import nl.adaptivity.gwt.base.client.Clickable;
import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.*;


public class Darwin implements EntryPoint {







  
  private class LoginReceivedCallback implements RequestCallback {

    @Override
    public void onResponseReceived(Request pRequest, Response pResponse) {
      String text = pResponse.getText();
      int cpos = text.indexOf(':');
      String result, payload;
      if (cpos>=0) {
        result = text.substring(0,cpos);
        payload = text.substring(cpos+1);
      } else {
        result = text;
        payload=null;
      }
      
      if ("login".equals(result) && (payload!=null)) {
        aUsername = payload;
        closeDialogs();
        updateLoginPanel();
      } else if ("logout".equals(result)) {
        aUsername = null;
        closeDialogs();
        updateLoginPanel();
      } else if ("error".equals(result)) {
        closeDialogs();
        error("Error validating login: "+payload, null);
      } else if ("invalid".equals(result)) {
        updateDialogTitle("Log in - Credentials invalid");
        aLoginContent.password.setValue("");
      }
      
    }

    @Override
    public void onError(Request pRequest, Throwable pException) {
      error("Error validating credentials:"+pException.getMessage(), pException);
    }

  }

  private class LoginHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent pEvent) {
      com.google.gwt.user.client.Element dialogBase = dialogContent.getElement();
      InputElement username = InputElement.as(XMLUtil.descendentWithAttribute(dialogBase, "name", "username"));
      InputElement password = InputElement.as(XMLUtil.descendentWithAttribute(dialogBase, "name", "password"));

      RequestBuilder rBuilder;
      rBuilder = new RequestBuilder(RequestBuilder.POST, "/accounts/login");
      rBuilder.setHeader("Accept", "application/binary");
      rBuilder.setHeader("Content-Type", "application/x-www-form-urlencoded");
      String postData = "username="+URL.encode(username.getValue())+"&password="+URL.encode(password.getValue());
      try {
        rBuilder.sendRequest(postData, new LoginReceivedCallback());
      } catch (RequestException e) {
        error("Could not send login request", e);
        closeDialogs();
      }


    }

  }

  private class DialogCloseHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent pEvent) {
      closeDialogs();
    }

  }

  private class LoginoutClickHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent pEvent) {
      if (aUsername==null) {
        loginDialog();
        // Login
      } else {
        try {
          RequestBuilder rBuilder;
          rBuilder = new RequestBuilder(RequestBuilder.GET, "/accounts/logout");
          rBuilder.setHeader("Accept", "application/binary");
          rBuilder.sendRequest(null, new LoginReceivedCallback());
        } catch (RequestException e) {
          error ("Could not log out", e);
        }
      }
    }

  }

  private class HistoryChangeHandler implements ValueChangeHandler<String> {

    @Override
    public void onValueChange(ValueChangeEvent<String> pEvent) {
      String newValue = pEvent.getValue();
      navigateTo(newValue, false);
    }

  }

  private class MenuClickHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent pEvent) {
      EventTarget target = pEvent.getNativeEvent().getEventTarget();
      navigateTo(target.<Element>cast().getAttribute("href"), true);
    }

  }

  private class MenuReceivedCallback implements RequestCallback {

    @Override
    public void onResponseReceived(Request pRequest, Response pResponse) {
      final String text = pResponse.getText();
      aMenu.setInnerHTML(text);
      updateMenuElements();
    }

    @Override
    public void onError(Request pRequest, Throwable pException) {
      log("Error updating the menu", pException);
    }

  }

  @UiTemplate("darwindialog.ui.xml")
  interface DarwinDialogBinder extends UiBinder<Widget, Darwin> { /* gwt generated */ };
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

  @Override
  public void onModuleLoad() {
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      History.newItem("/", false);
    }


    Document document = Document.get();
    aMenu = (DivElement) document.getElementById("menu");
    aLocation= "/";
    Element usernameSpan = document.getElementById("username");
    if (usernameSpan!=null) {
      aUsername = usernameSpan.getInnerText();
    } else {
      aUsername = null;
    }
    aContentPanel = RootPanel.get("content");

    updateContentTab();

    requestRefreshMenu();

    updateLogin(document);

    History.addValueChangeHandler(new HistoryChangeHandler());
  }

  public void error(String pMessage, Throwable pException) {
    GWT.log("Error: "+pMessage, pException);
    String message;
    if (pException==null) {
      message = pMessage;
    } else {
      message = pMessage+"<br />"+pException.getMessage();
    }
    modalDialog(message);
  }

  private void modalDialog(String pString) {
    Button closeButton = new Button("Ok");
    closeButton.addClickHandler(aDialogCloseHandler);
    dialog("Message", new Label(pString), closeButton);
    // TODO Auto-generated method stub

  }

  private void loginDialog() {
    aLoginContent = new LoginContent();
    if (aDialogCloseHandler==null) { aDialogCloseHandler = new DialogCloseHandler(); }
    dialog("Log in", aLoginContent);
    Clickable cancel = Clickable.wrapNoAttach(aLoginContent.cancel);

    cancel.addClickHandler(aDialogCloseHandler);

    Clickable login = Clickable.wrapNoAttach(aLoginContent.login);
    login.addClickHandler(new LoginHandler());
    // This must be after dialog, otherwise cancelButton will not be attached (and can not get a handler)
//    loginContent.cancelButton.addClickHandler(aDialogCloseHandler);
  }

  public void updateDialogTitle(String pString) {
    dialogTitle.setText(pString);
  }

  private void dialog(String pTitle, Widget... pContents) {
    Widget dialog = darwinDialogBinder.createAndBindUi(this);
    dialogTitle.setText(pTitle);
    RootPanel.get();
    for(Widget w: pContents) {
      dialogContent.add(w);
    }
    aContentPanel.add(dialog);
  }

  private void closeDialogs() {
    for(int i = 0; i<aContentPanel.getWidgetCount(); ++i) {
      Widget w = aContentPanel.getWidget(i);
      String[] styleNames = w.getStyleName().split(" ");
      for (String styleName:styleNames) {
        if ("dialog".equals(styleName)) {
          w.removeFromParent();
          --i; // Decrease so adding will move us to our current position.
          break;
        }
      }
    }
  }

  public void navigateTo(String pNewLocation, boolean addHistory) {
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
    } else if (aLocation.equals("/about")) {
      setAboutPanel();
    }
  }

  private void setActionPanel() {
    GWT.runAsync(new RunAsyncCallback() {

      @Override
      public void onSuccess() {
        aContentPanel.clear();
        aContentPanel.add(new Label("Action Panel"));
      }

      @Override
      public void onFailure(Throwable pReason) {
        aContentPanel.clear();
        aContentPanel.add(new Label("Could not load action panel module"));
      }
    });
  }

  private void setInboxPanel() {
    GWT.runAsync(new RunAsyncCallback() {

      @Override
      public void onSuccess() {
        aContentPanel.clear();
        aContentPanel.add(new Label("Inbox Panel"));
        aContentLayoutPanel=null;
      }

      @Override
      public void onFailure(Throwable pReason) {
        aContentPanel.clear();
        aContentPanel.add(new Label("Could not load inbox panel module"));
      }
    });
  }

  private void setAboutPanel() {
    GWT.runAsync(new RunAsyncCallback() {

      @Override
      public void onSuccess() {
        aContentPanel.clear();
        aContentPanel.add(new AboutPanel());
      }

      @Override
      public void onFailure(Throwable pReason) {
        aContentPanel.clear();
        aContentPanel.add(new Label("Could not load about page module"));
      }
    });
  }



  /**
   * Make the menu elements active and add an onClick Listener.
   */
  public void updateMenuElements() {
    if (aMenuClickHandler == null) { aMenuClickHandler = new MenuClickHandler(); }
    for (Element item=aMenu.getFirstChildElement(); item!=null; item = item.getNextSiblingElement()) {
      InlineLabel l = InlineLabel.wrap(item);
      l.addClickHandler(aMenuClickHandler);
    }
  }
  
  private void updateLogin(Document document) {
    Clickable loginout =Clickable.wrap(document.getElementById("logout"));
    loginout.getElement().removeAttribute("href");
    aLoginoutRegistration = loginout.addClickHandler(new LoginoutClickHandler());
  
  
  }

  private void updateLoginPanel() {
    aLoginoutRegistration.removeHandler();
    Document document = Document.get();

    DivElement loginPanel = document.getElementById("login").cast();
    if (aUsername!=null) {
      loginPanel.setInnerHTML("<span id=\"username\">"+aUsername+"</span><a id=\"logout\">logout</a>");
    } else {
      loginPanel.setInnerHTML("<a id=\"logout\">login</a>");
    }
    Clickable loginout =Clickable.wrap(document.getElementById("logout"));
    aLoginoutRegistration = loginout.addClickHandler(new LoginoutClickHandler());
    
  }
  

  private void requestRefreshMenu() {
    RequestBuilder rBuilder;
    rBuilder = new RequestBuilder(RequestBuilder.GET, "/common/menu.php?location="+URL.encode(aLocation));
    try {
      rBuilder.sendRequest(null, new MenuReceivedCallback());
    } catch (RequestException e) {
      log("Could not update menu", e);
    }
  }

  private void log(String pMessage, Throwable pThrowable) {
    GWT.log(pMessage, pThrowable);
  }

  private void log(String pMessage) {
    GWT.log(pMessage);
  }

}
