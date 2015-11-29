package nl.adaptivity.ws;

/**
 * Created by pdvrieze on 28/11/15.
 */
public abstract class WsMethodWrapper {

  protected Object[] mParams;
  protected Object mResult;

  public abstract void exec();
}
