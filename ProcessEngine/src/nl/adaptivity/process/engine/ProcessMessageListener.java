package nl.adaptivity.process.engine;


public interface ProcessMessageListener {

  void fireMessage(ExtMessage pMessage);

  void fireFinishedInstance(long pHandle);

  void cancelInstance(long pInstance);

}
