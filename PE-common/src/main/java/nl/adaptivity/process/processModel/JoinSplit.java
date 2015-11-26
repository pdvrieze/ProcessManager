package nl.adaptivity.process.processModel;


public interface JoinSplit<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> extends ProcessNode<T,M> {

  void setMax(int max);

  int getMax();

  void setMin(int min);

  int getMin();

}