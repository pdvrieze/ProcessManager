package nl.adaptivity.gwt.base.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;


public class Clickable extends Widget {

  public static Clickable wrap(final Element pElement) {
    assert Document.get().getBody().isOrHasChild(pElement);

    final Clickable clickable = new Clickable(pElement);

    clickable.onAttach();

    RootPanel.detachOnWindowClose(clickable);

    return clickable;

  }

  public Clickable(final Element pElement) {
    setElement(pElement);
  }

  public HandlerRegistration addClickHandler(final ClickHandler pClickHandler) {
    return addDomHandler(pClickHandler, ClickEvent.getType());
  }

  public static Clickable wrapNoAttach(final Element pElement) {
    assert Document.get().getBody().isOrHasChild(pElement);

    final Clickable clickable = new Clickable(pElement);
    clickable.onAttach();
    return clickable;
  }

}
