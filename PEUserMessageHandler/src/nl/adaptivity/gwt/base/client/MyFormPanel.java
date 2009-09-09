package nl.adaptivity.gwt.base.client;

import nl.adaptivity.gwt.base.client.impl.MyFormPanelImpl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.FormElement;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.impl.FormPanelImplHost;


public class MyFormPanel extends SimplePanel implements FormPanelImplHost{

  public static class ResetEvent extends GwtEvent<ResetHandler> {
    
    private static Type<ResetHandler> TYPE;
    
    static Type<ResetHandler> getType() {
      if (TYPE == null) {
        TYPE= new Type<ResetHandler>();
      }
      return TYPE;
    }

    @Override
    protected void dispatch(ResetHandler pHandler) {
      pHandler.onReset(this);
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<ResetHandler> getAssociatedType() {
      return getType();
    }
    
  }
  
  public interface ResetHandler extends EventHandler {

    void onReset(ResetEvent pResetEvent);

  }

  /**
   * Fired when a form has been submitted successfully.
   */
  public static class SubmitCompleteEvent extends
      GwtEvent<SubmitCompleteHandler> {
    /**
     * The event type.
     */
    private static Type<SubmitCompleteHandler> TYPE;

    /**
     * Handler hook.
     * 
     * @return the handler hook
     */
    static Type<SubmitCompleteHandler> getType() {
      if (TYPE == null) {
        TYPE = new Type<SubmitCompleteHandler>();
      }
      return TYPE;
    }

    private Document resultDoc;

    /**
     * Create a submit complete event.
     * 
     * @param resultsDoc the results from submitting the form
     */
    protected SubmitCompleteEvent(Document resultsDoc) {
      this.resultDoc = resultsDoc;
    }

    @Override
    public final Type<SubmitCompleteHandler> getAssociatedType() {
      return TYPE;
    }

    /**
     * Gets the result text of the form submission.
     * 
     * @return the result html, or <code>null</code> if there was an error
     *         reading it
     * @tip The result html can be <code>null</code> as a result of submitting a
     *      form to a different domain.
     */
    public Document getResults() {
      return resultDoc;
    }

    @Override
    protected void dispatch(SubmitCompleteHandler handler) {
      handler.onSubmitComplete(this);
    }
  }

  /**
   * Handler for {@link MyFormPanel.SubmitCompleteEvent} events.
   */
  public interface SubmitCompleteHandler extends EventHandler {
    /**
     * Fired when a form has been submitted successfully.
     * 
     * @param event the event
     */
    void onSubmitComplete(MyFormPanel.SubmitCompleteEvent event);
  }

  /**
   * Fired when the form is submitted.
   */
  public static class SubmitEvent extends GwtEvent<SubmitHandler> {
    /**
     * The event type.
     */
    private static Type<SubmitHandler> TYPE = new Type<SubmitHandler>();

    /**
     * Handler hook.
     * 
     * @return the handler hook
     */
    static Type<SubmitHandler> getType() {
      if (TYPE == null) {
        TYPE = new Type<SubmitHandler>();
      }
      return TYPE;
    }

    private boolean canceled = false;

    /**
     * Cancel the form submit. Firing this will prevent a subsequent
     * {@link MyFormPanel.SubmitCompleteEvent} from being fired.
     */
    public void cancel() {
      this.canceled = true;
    }

    @Override
    public final Type<MyFormPanel.SubmitHandler> getAssociatedType() {
      return TYPE;
    }

    /**
     * Gets whether this form submit will be canceled.
     * 
     * @return <code>true</code> if the form submit will be canceled
     */
    public boolean isCanceled() {
      return canceled;
    }

    @Override
    protected void dispatch(MyFormPanel.SubmitHandler handler) {
      handler.onSubmit(this);
    }

    /**
     * This method is used for legacy support and should be removed when
     * {@link FormHandler} is removed.
     * 
     * @deprecated Use {@link MyFormPanel.SubmitEvent#cancel()} instead
     */
    @Deprecated
    void setCanceled(boolean canceled) {
      this.canceled = canceled;
    }
  }

  /**
   * Handler for {@link MyFormPanel.SubmitEvent} events.
   */
  public interface SubmitHandler extends EventHandler {
    /**
     *Fired when the form is submitted.
     * 
     * <p>
     * The MyFormPanel must <em>not</em> be detached (i.e. removed from its parent
     * or otherwise disconnected from a {@link RootPanel}) until the submission
     * is complete. Otherwise, notification of submission will fail.
     * </p>
     * 
     * @param event the event
     */
    void onSubmit(MyFormPanel.SubmitEvent event);
  }

  /**
   * Used with {@link #setEncoding(String)} to specify that the form will be
   * submitted using MIME encoding (necessary for {@link FileUpload} to work
   * properly).
   */
  public static final String ENCODING_MULTIPART = "multipart/form-data";

  /**
   * Used with {@link #setEncoding(String)} to specify that the form will be
   * submitted using traditional URL encoding.
   */
  public static final String ENCODING_URLENCODED = "application/x-www-form-urlencoded";

  /**
   * Used with {@link #setMethod(String)} to specify that the form will be
   * submitted using an HTTP GET request.
   */
  public static final String METHOD_GET = "get";

  /**
   * Used with {@link #setMethod(String)} to specify that the form will be
   * submitted using an HTTP POST request (necessary for {@link FileUpload} to
   * work properly).
   */
  public static final String METHOD_POST = "post";

  private static int formId = 0;
  private static MyFormPanelImpl impl = GWT.create(MyFormPanelImpl.class);

  /**
   * Creates a MyFormPanel that wraps an existing &lt;form&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * <p>
   * The specified form element's target attribute will not be set, and the
   * {@link FormSubmitCompleteEvent} will not be fired.
   * </p>
   * 
   * @param element the element to be wrapped
   */
  public static MyFormPanel wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    MyFormPanel formPanel = new MyFormPanel(element);

    // Mark it attached and remember it for cleanup.
    formPanel.onAttach();
    RootPanel.detachOnWindowClose(formPanel);

    return formPanel;
  }

  /**
   * Creates a MyFormPanel that wraps an existing &lt;form&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * <p>
   * If the createIFrame parameter is set to <code>true</code>, then the wrapped
   * form's target attribute will be set to a hidden iframe. If not, the form's
   * target will be left alone, and the FormSubmitComplete event will not be
   * fired.
   * </p>
   * 
   * @param element the element to be wrapped
   * @param createIFrame <code>true</code> to create an &lt;iframe&gt; element
   *          that will be targeted by this form
   */
  public static MyFormPanel wrap(Element element, boolean createIFrame) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    MyFormPanel formPanel = new MyFormPanel(element, createIFrame);

    // Mark it attached and remember it for cleanup.
    formPanel.onAttach();
    RootPanel.detachOnWindowClose(formPanel);

    return formPanel;
  }

  private String frameName;
  private Element synthesizedFrame;

  /**
   * Creates a new MyFormPanel. When created using this constructor, it will be
   * submitted to a hidden &lt;iframe&gt; element, and the results of the
   * submission made available via {@link SubmitCompleteHandler}.
   * 
   * <p>
   * The back-end server is expected to respond with a content-type of
   * 'text/html', meaning that the text returned will be treated as HTML. If any
   * other content-type is specified by the server, then the result HTML sent in
   * the onFormSubmit event will be unpredictable across browsers, and the
   * {@link SubmitCompleteHandler#onSubmitComplete(MyFormPanel.SubmitCompleteEvent) onSubmitComplete} event
   * may not fire at all.
   * </p>
   * 
   * @tip The initial implementation of MyFormPanel specified that the server
   *      respond with a content-type of 'text/plain'. This has been
   *      intentionally changed to specify 'text/html' because 'text/plain'
   *      cannot be made to work properly on all browsers.
   */
  public MyFormPanel() {
    this(Document.get().createFormElement(), true);
  }

  /**
   * Creates a MyFormPanel that targets a {@link NamedFrame}. The target frame is
   * not physically attached to the form, and must therefore still be added to a
   * panel elsewhere.
   * 
   * <p>
   * When the MyFormPanel targets an external frame in this way, it will not fire
   * the FormSubmitComplete event.
   * </p>
   * 
   * @param frameTarget the {@link NamedFrame} to be targetted
   */
  public MyFormPanel(NamedFrame frameTarget) {
    this(frameTarget.getName());
  }

  /**
   * Creates a new MyFormPanel. When created using this constructor, it will be
   * submitted either by replacing the current page, or to the named
   * &lt;iframe&gt;.
   * 
   * <p>
   * When the MyFormPanel targets an external frame in this way, it will not fire
   * the FormSubmitComplete event.
   * </p>
   * 
   * @param target the name of the &lt;iframe&gt; to receive the results of the
   *          submission, or <code>null</code> to specify that the current page
   *          be replaced
   */
  public MyFormPanel(String target) {
    super(Document.get().createFormElement());
    setTarget(target);
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be a &lt;form&gt; element.
   * 
   * <p>
   * The specified form element's target attribute will not be set, and the
   * {@link FormSubmitCompleteEvent} will not be fired.
   * </p>
   * 
   * @param element the element to be used
   */
  protected MyFormPanel(Element element) {
    this(element, false);
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be a &lt;form&gt; element.
   * 
   * <p>
   * If the createIFrame parameter is set to <code>true</code>, then the wrapped
   * form's target attribute will be set to a hidden iframe. If not, the form's
   * target will be left alone, and the FormSubmitComplete event will not be
   * fired.
   * </p>
   * 
   * @param element the element to be used
   * @param createIFrame <code>true</code> to create an &lt;iframe&gt; element
   *          that will be targeted by this form
   */
  protected MyFormPanel(Element element, boolean createIFrame) {
    super(element);
    FormElement.as(element);

    if (createIFrame) {
      assert ((getTarget() == null) || (getTarget().trim().length() == 0)) : "Cannot create target iframe if the form's target is already set.";

      frameName = "FormPanel_" + (++formId);
      setTarget(frameName);

      sinkEvents(Event.ONLOAD);
    }
  }

  /**
   * Adds a {@link SubmitCompleteEvent} handler.
   * 
   * @param handler the handler
   * @return the handler registration used to remove the handler
   */
  public HandlerRegistration addSubmitCompleteHandler(
      SubmitCompleteHandler handler) {
    return addHandler(handler, SubmitCompleteEvent.getType());
  }

  /**
   * Adds a {@link SubmitEvent} handler.
   * 
   * @param handler the handler
   * @return the handler registration used to remove the handler
   */
  public HandlerRegistration addSubmitHandler(SubmitHandler handler) {
    return addHandler(handler, SubmitEvent.getType());
  }

  public HandlerRegistration addResetHandler(ResetHandler pHandler) {
    return addHandler(pHandler, ResetEvent.getType());
  }

  /**
   * Gets the 'action' associated with this form. This is the URL to which it
   * will be submitted.
   * 
   * @return the form's action
   */
  public String getAction() {
    return getFormElement().getAction();
  }

  /**
   * Gets the encoding used for submitting this form. This should be either
   * {@link #ENCODING_MULTIPART} or {@link #ENCODING_URLENCODED}.
   * 
   * @return the form's encoding
   */
  public String getEncoding() {
    return impl.getEncoding(getElement());
  }

  /**
   * Gets the HTTP method used for submitting this form. This should be either
   * {@link #METHOD_GET} or {@link #METHOD_POST}.
   * 
   * @return the form's method
   */
  public String getMethod() {
    return getFormElement().getMethod();
  }

  /**
   * Gets the form's 'target'. This is the name of the {@link NamedFrame} that
   * will receive the results of submission, or <code>null</code> if none has
   * been specified.
   * 
   * @return the form's target.
   */
  public String getTarget() {
    return getFormElement().getTarget();
  }

  /**
   * Fired when a form is submitted.
   * 
   * @return true if the form is submitted, false if canceled
   */
  public boolean onFormSubmit() {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      return onFormSubmitAndCatch(handler);
    } else {
      return onFormSubmitImpl();
    }
  }

  public void onFrameLoad() {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      onFrameLoadAndCatch(handler);
    } else {
      onFrameLoadImpl();
    }
  }

  /**
   * Resets the form, clearing all fields.
   */
  public void reset() {
    impl.reset(getElement());
    fireResetEvent();
  }

  /**
   * Sets the 'action' associated with this form. This is the URL to which it
   * will be submitted.
   * 
   * @param url the form's action
   */
  public void setAction(String url) {
    getFormElement().setAction(url);
  }

  /**
   * Sets the encoding used for submitting this form. This should be either
   * {@link #ENCODING_MULTIPART} or {@link #ENCODING_URLENCODED}.
   * 
   * @param encodingType the form's encoding
   */
  public void setEncoding(String encodingType) {
    impl.setEncoding(getElement(), encodingType);
  }

  /**
   * Sets the HTTP method used for submitting this form. This should be either
   * {@link #METHOD_GET} or {@link #METHOD_POST}.
   * 
   * @param method the form's method
   */
  public void setMethod(String method) {
    getFormElement().setMethod(method);
  }

  /**
   * Submits the form.
   * 
   * <p>
   * The MyFormPanel must <em>not</em> be detached (i.e. removed from its parent
   * or otherwise disconnected from a {@link RootPanel}) until the submission is
   * complete. Otherwise, notification of submission will fail.
   * </p>
   */
  public void submit() {
    // Fire the onSubmit event, because javascript's form.submit() does not
    // fire the built-in onsubmit event.
    if (!fireSubmitEvent()) {
      return;
    }

    impl.submit(getElement(), synthesizedFrame);
  }

  @Override
  protected void onAttach() {
    super.onAttach();

    if (frameName != null) {
      // Create and attach a hidden iframe to the body element.
      createFrame();
      Document.get().getBody().appendChild(synthesizedFrame);

      // Hook up the underlying iframe's onLoad event when attached to the DOM.
      // Making this connection only when attached avoids memory-leak issues.
      // The MyFormPanel cannot use the built-in GWT event-handling mechanism
      // because there is no standard onLoad event on iframes that works across
      // browsers.
      impl.hookEvents(synthesizedFrame, getElement(), this);
    }
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    if (synthesizedFrame != null) {
      // Unhook the iframe's onLoad when detached.
      impl.unhookEvents(synthesizedFrame, getElement());

      // And remove it from the document.
      Document.get().getBody().removeChild(synthesizedFrame);
      synthesizedFrame = null;
    }
  }

  // For unit-tests.
  Element getSynthesizedIFrame() {
    return synthesizedFrame;
  }

  private void createFrame() {
    // Attach a hidden IFrame to the form. This is the target iframe to which
    // the form will be submitted. We have to create the iframe using innerHTML,
    // because setting an iframe's 'name' property dynamically doesn't work on
    // most browsers.
    Element dummy = Document.get().createDivElement();
    dummy.setInnerHTML("<iframe src=\"javascript:''\" name='" + frameName
        + "' style='position:absolute;width:0;height:0;border:0'>");

    synthesizedFrame = dummy.getFirstChildElement();
  }

  /**
   * Fire a {@link MyFormPanel.SubmitEvent}.
   * 
   * @return true to continue, false if canceled
   */
  private boolean fireSubmitEvent() {
    MyFormPanel.SubmitEvent event = new MyFormPanel.SubmitEvent();
    fireEvent(event);
    return !event.isCanceled();
  }

  private void fireResetEvent() {
    MyFormPanel.ResetEvent event = new MyFormPanel.ResetEvent();
    fireEvent(event);
  }

  private FormElement getFormElement() {
    return getElement().cast();
  }

  private boolean onFormSubmitAndCatch(UncaughtExceptionHandler handler) {
    try {
      return onFormSubmitImpl();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
      return false;
    }
  }

  /**
   * @return true if the form is submitted, false if canceled
   */
  private boolean onFormSubmitImpl() {
    return fireSubmitEvent();
  }

  private void onFrameLoadAndCatch(UncaughtExceptionHandler handler) {
    try {
      onFrameLoadImpl();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }

  private void onFrameLoadImpl() {
    // Fire onComplete events in a deferred command. This is necessary
    // because clients that detach the form panel when submission is
    // complete can cause some browsers (i.e. Mozilla) to go into an
    // 'infinite loading' state. See issue 916.
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        fireEvent(new SubmitCompleteEvent(impl.getContents(synthesizedFrame)));
      }
    });
  }

  private void setTarget(String target) {
    getFormElement().setTarget(target);
  }

}
