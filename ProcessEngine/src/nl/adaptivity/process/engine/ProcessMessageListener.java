package nl.adaptivity.process.engine;


public interface ProcessMessageListener {

  void fireMessage(Message pMessage);

  void fireFinishedInstance(long pHandle);

  void cancelInstance(long pInstance);

}
