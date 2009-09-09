package nl.adaptivity.gwt.base.client;

import com.google.gwt.user.client.ui.FocusWidget;


public interface IWidgetController {

  /**
   * Add a widget to be automatically enabled based on whether an item is allowed.
   * This also sets the current enabled state based on the current list state. 
   */
  public void addControlledWidget(FocusWidget pWidget);

  public boolean removeControlledWidget(FocusWidget pWidget);

}