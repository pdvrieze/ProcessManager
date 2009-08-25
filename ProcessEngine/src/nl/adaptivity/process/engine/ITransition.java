package nl.adaptivity.process.engine;


public interface ITransition {

  boolean conditionHolds();

  void follow();

}
