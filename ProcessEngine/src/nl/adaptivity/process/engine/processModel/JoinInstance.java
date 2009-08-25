package nl.adaptivity.process.engine.processModel;


public class JoinInstance extends ProcessNodeInstance {

  public JoinInstance(Join pNode) {
    super(pNode, null);
  }

  private int aComplete = 0;
  private int aSkipped = 0;

  public void incComplete() {
    aComplete++;
  }

  public int getTotal() {
    return aComplete + aSkipped;
  }

  public int getComplete() {
    return aComplete;
  }

  public void incSkipped() {
    aSkipped++;
  }
  
  @Override
  public Join getNode() {
    return (Join) super.getNode();
  }

}
