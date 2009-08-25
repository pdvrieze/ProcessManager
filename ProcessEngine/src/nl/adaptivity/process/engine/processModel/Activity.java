package nl.adaptivity.process.engine.processModel;


public abstract class Activity extends ProcessNode{

  protected Activity(ProcessNode pPrevious) {
    super(pPrevious);
  }

  private static final long serialVersionUID = 282944120294737322L;

}
