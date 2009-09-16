package nl.adaptivity.gwt.ext.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.*;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;


public class RemoteListBox extends ControllingListBox implements RequestCallback {

  
  
  
  private static class ListElement {

    final String value;
    final String text;

    public ListElement(String pValue, String pText) {
      value = pValue;
      text = pText;
    }

  }

  public static class UpdateEvent extends GwtEvent<UpdateEventHandler>{

    private static Type<UpdateEventHandler> TYPE;

    private final Throwable aException;

    public UpdateEvent(Throwable pException) {
      aException = pException;
    }

    public static Type<UpdateEventHandler> getType() {
      if (TYPE == null) {
        TYPE= new Type<UpdateEventHandler>();
      }
      return TYPE;
    }

    @Override
    protected void dispatch(UpdateEventHandler pHandler) {
      pHandler.onUpdateRemoteList(this);
    }

    @Override
    public Type<UpdateEventHandler> getAssociatedType() {
      return getType();
    }
    
    public boolean isException() {
      return aException!=null;
    }
    
    public Throwable getException() {
      return aException;
    }

  }

  public interface UpdateEventHandler extends EventHandler{

    void onUpdateRemoteList(UpdateEvent pUpdateEvent);

  }

  private Method aMethod = RequestBuilder.GET;
  private String aUrl;
  private String aRootElement;
  private String aListElement;
  private String aTextElement;
  private String aValueElement;
  private boolean aStarted = false;
  
  public RemoteListBox(String pUrl) {
    aUrl = pUrl;
  }
  
  public void start() {
    if (aRootElement == null || aListElement==null || aTextElement==null || aValueElement==null) {
      throw new IllegalStateException("Can not start updating from a remote system when the proper properties for parsing the result are not set");
    }
    requestUpdate();
    aStarted  = true;
  }
  
  public void update() {
    if (aStarted) {
      requestUpdate();
    }
  }
  
  public void stop() {
    aStarted = false;
  }

  private void requestUpdate() {
    RequestBuilder rBuilder = new RequestBuilder(aMethod, getUrl());
    
    try {
      rBuilder.sendRequest(null, this);
    } catch (RequestException e) {
      fireUpdateError(e);
    }
  }

  private void fireUpdateError(Throwable pException) {
    fireEvent(new UpdateEvent(pException));
    GWT.log("Error: "+pException.getMessage(), pException);
  }

  public void setMethod(Method method) {
    aMethod = method;
  }

  public Method getMethod() {
    return aMethod;
  }

  public void setUrl(String url) {
    aUrl = url;
  }

  public String getUrl() {
    return aUrl;
  }
  
  public String getRootElement() {
    return aRootElement;
  }

  public void setRootElement(String pRootElement) {
    aRootElement = pRootElement;
  }
  
  public String getListElement() {
    return aListElement;
  }
  
  public void setListElement(String pListElement) {
    aListElement = pListElement;
  }
  
  public String getTextElement() {
    return aTextElement;
  }
  
  public void setTextElement(String pTextElement) {
    aTextElement = pTextElement;
  }
  
  public String getValueElement() {
    return aValueElement;
  }
  
  public void setValueElement(String pValueElement) {
    aValueElement = pValueElement;
  }

  public HandlerRegistration addUpdateEventHandler(UpdateEventHandler pHandler) {
    return addHandler(pHandler, UpdateEvent.getType());
  }

  @Override
  public void onError(Request pRequest, Throwable pException) {
    fireUpdateError(pException);
  }

  @Override
  public void onResponseReceived(Request pRequest, Response pResponse) {
    if (Response.SC_OK == pResponse.getStatusCode()) {
      updateList(asListElements(pResponse.getText()));
    } else if (pResponse.getStatusCode()>=400){
      fireUpdateError(new RemoteListException(pResponse.getStatusCode(), pResponse.getStatusText()));
    }
  }

  private void updateList(List<ListElement> pListElements) {
    int selectedIndex = getSelectedIndex();
    String selected = selectedIndex>=0 ? getValue(selectedIndex) : null;
    clear();
    
    int newSelected = -1;
    int i=0;
    for(ListElement ref:pListElements) {
      addItem(ref.text, ref.value);
      if (ref.value.equals(selected)) {
        newSelected = i;
      }
      ++i;
    }
    if (newSelected>=0) {
      setSelectedIndex(newSelected);
    }
  }

  private List<ListElement> asListElements(String pText) {
    final Document myResponse;
    
    myResponse = XMLParser.parse(pText);
    ArrayList<ListElement> result = new ArrayList<ListElement>();
    
    Node root = myResponse.getFirstChild();
    if (root.getNodeName().equals(aRootElement)) {
      Node child = root.getFirstChild();
      while(child!=null) {
        if (aListElement.equals(child.getNodeName()) ){
          final NamedNodeMap attributes = child.getAttributes();
          final String text;
          final String value;
          if (aTextElement.startsWith("@")) {
            Node elem = attributes.getNamedItem(aTextElement.substring(1));
            text = elem == null ? null : elem.getNodeValue();
          } else if (aTextElement.startsWith("=")) {
            try {
              text=parseParam(child, aTextElement.substring(1));
            } catch (RuntimeException e) {
              GWT.log("Error", e);
              throw e;
            }
          } else {
            Node candidate = child.getFirstChild();
            while (candidate!= null && (!aTextElement.equals(candidate.getNodeName()))) {
              candidate = candidate.getNextSibling();
            }
            text = candidate == null ? null : candidate.getNodeValue();
          }
          if (aValueElement.startsWith("@")) {
            Node elem = attributes.getNamedItem(aValueElement.substring(1));
            value = elem == null ? null : elem.getNodeValue();
          } else if (aValueElement.startsWith("=")) {
            try {
              value=parseParam(child, aValueElement.substring(1));
            } catch (RuntimeException e) {
              GWT.log("Error", e);
              throw e;
            }
          } else {
            value = getSubNodeValue(child, aValueElement);
          }
          if (text!=null && value!=null) {
            result.add(new ListElement(value, text));
          }
        }
        child = child.getNextSibling();
      }
      return result;
    }
    
    return new ArrayList<ListElement>(0);
  }

  private String parseParam(Node pNode, String pSpec) {
    StringBuilder result = new StringBuilder(pSpec.length()*2);
    int i =0;
    int j = 0;
    while (j<pSpec.length()) {
      final char c = pSpec.charAt(j);
      if (c=='\\' && (j+1<pSpec.length())) {
        result.append(pSpec.substring(i, j));
        ++j;
        i=j;
      } else if (c=='$') {
        result.append(pSpec.substring(i, j));
        if (j+3<pSpec.length() && pSpec.charAt(j+1)=='{') {
          j+=2;
          i=j;
          while (j<pSpec.length() && pSpec.charAt(j)!='}') {
            ++j;
          }
          result.append(getSubNodeValue(pNode, pSpec.substring(i, j)));
          ++j;
        } else {
          ++j;
          i=j;
          while (j<pSpec.length() && isChar(pSpec.charAt(j))) {
            ++j;
          }
          result.append(getSubNodeValue(pNode, pSpec.substring(i, j)));
        }
        
        i=j;
      } else if (c=='@') {
        result.append(pSpec.substring(i, j));
        if (j+3<pSpec.length() && pSpec.charAt(j+1)=='{') {
          j+=2;
          i=j;
          while (j<pSpec.length() && pSpec.charAt(j)!='}') {
            ++j;
          }
          result.append(getAttributeValue(pNode, pSpec.substring(i, j)));
          ++j;
        } else {
          ++j;
          i=j;
          while (j<pSpec.length() && isChar(pSpec.charAt(j))) {
            ++j;
          }
          result.append(getAttributeValue(pNode, pSpec.substring(i, j)));
        }
        
        i=j;
      } else {
        ++j;
      }
    }
    result.append(pSpec.substring(i,j));
    return result.toString();
  }

  private String getSubNodeValue(Node pNode, String pName) {
    final String value;
    Node candidate = pNode.getFirstChild();
    while (candidate!= null && (!pName.equals(candidate.getNodeName()))) {
      candidate = candidate.getNextSibling();
    }
    value = candidate == null ? null : candidate.getNodeValue();
    if (value==null) {
      GWT.log("subnode "+pName+" could not be resolved", null);
    }
    return value;
  }

  private Object getAttributeValue(Node pNode, String pName) {
    final Node val = pNode.getAttributes().getNamedItem(pName);
    if (val==null) {
      GWT.log("Attribute "+pName+" could not be resolved", null);
    }
    return val;
  }

  private boolean isChar(char c) {
    return Character.isLetterOrDigit(c);
  }
  
}
