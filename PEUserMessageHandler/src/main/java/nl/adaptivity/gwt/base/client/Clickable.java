package nl.adaptivity.gwt.base.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;


public class Clickable extends Widget {

  public static Clickable wrap(final Element element) {
    assert Document.get().getBody().isOrHasChild(element);

    final Clickable clickable = new Clickable(element);

    clickable.onAttach();

    RootPanel.detachOnWindowClose(clickable);

    return clickable;

  }

  public Clickable(final Element element) {
    setElement(element);
  }

  public HandlerRegistration addClickHandler(final ClickHandler clickHandler) {
    return addDomHandler(clickHandler, ClickEvent.getType());
  }

  public static Clickable wrapNoAttach(final Element element) {
    assert Document.get().getBody().isOrHasChild(element);

    final Clickable clickable = new Clickable(element);
    clickable.onAttach();
    return clickable;
  }

}
