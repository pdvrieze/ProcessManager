package nl.adaptivity.ws;

import nl.adaptivity.messaging.MessagingException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Created by pdvrieze on 28/11/15.
 */
public abstract class WsMethodWrapper {

  protected final Object mOwner;
  protected final Method mMethod;
  protected Object[] mParams;
  protected Object mResult;

  public WsMethodWrapper(final Object owner, final Method method) {
    mOwner = owner;
    mMethod = method;
  }

  public void exec() {
    if (mParams == null) {
      throw new IllegalArgumentException("Argument unmarshalling has not taken place yet");
    }
    try {
      mResult = mMethod.invoke(mOwner, mParams);
    } catch (@NotNull final IllegalArgumentException | IllegalAccessException e) {
      throw new MessagingException(e);
    } catch (@NotNull final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      throw new MessagingException(cause != null ? cause : e);
    }
  }
}
