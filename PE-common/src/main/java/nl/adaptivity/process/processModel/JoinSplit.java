package nl.adaptivity.process.processModel;


public interface JoinSplit<T extends ProcessNode<T>> {

  void setMax(int max);

  int getMax();

  void setMin(int min);

  int getMin();

}