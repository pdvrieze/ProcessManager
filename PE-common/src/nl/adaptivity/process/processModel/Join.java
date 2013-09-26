package nl.adaptivity.process.processModel;



public interface Join<T extends ProcessNode<T>> extends ProcessNode<T> {

  public void setMax(int max);

  public int getMax();

  public void setMin(int min);

  public int getMin();

}