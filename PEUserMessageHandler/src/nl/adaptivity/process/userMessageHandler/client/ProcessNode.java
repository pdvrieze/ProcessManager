package nl.adaptivity.process.userMessageHandler.client;


public abstract class ProcessNode {

  private String aId;

  protected ProcessNode(String pId) {
    aId = pId;
  }

  public String getId() {
    return aId;
  }

}
