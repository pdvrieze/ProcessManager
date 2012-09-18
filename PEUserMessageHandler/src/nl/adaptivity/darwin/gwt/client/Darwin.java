package nl.adaptivity.darwin.gwt.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.http.client.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Widget;


public class Darwin implements EntryPoint {

  
  private class MenuReceivedCallback implements RequestCallback {

    @Override
    public void onResponseReceived(Request pRequest, Response pResponse) {
      final String text = pResponse.getText();
      aMenu.setInnerHTML(text);
      updateMenuElements();
    }

    @Override
    public void onError(Request pRequest, Throwable pException) {
      getLogger().log(Level.SEVERE, "Error updating the menu", pException);
    }

  }

  interface DarwinUiBinder extends UiBinder<Widget, Darwin> { /* Dynamic gwt */}

  private DivElement aMenu;
  private String aLocation;
  private String aUsername;

  @Override
  public void onModuleLoad() {
    Document document = Document.get();
    aMenu = (DivElement) document.getElementById("menu");
    aLocation= "/";
    Element usernameSpan = document.getElementById("username");
    if (usernameSpan!=null) {
      aUsername = usernameSpan.getInnerText();
    }
    
    requestRefreshMenu();
  }

  /**
   * Make the menu elements active and add an onClick Listener.
   */
  public void updateMenuElements() {
    /*
    for (Element item=aMenu.getFirstChildElement(); item!=null; item = item.getNextSiblingElement()) {
      
    }
    // TODO Auto-generated method stub
    // 
    throw new UnsupportedOperationException("Not yet implemented");
    */
  }

  private void requestRefreshMenu() {
    RequestBuilder rBuilder;
    rBuilder = new RequestBuilder(RequestBuilder.GET, "/common/menu.php?location="+URL.encode(aLocation));
    try {
      rBuilder.sendRequest(null, new MenuReceivedCallback());
    } catch (RequestException e) {
      getLogger().log(Level.SEVERE, "Could not update menu", e);
    }
  }

  private Logger getLogger() {
    return Logger.getLogger("darwin");
  }

}
