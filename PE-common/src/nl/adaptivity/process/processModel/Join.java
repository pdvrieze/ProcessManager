package nl.adaptivity.process.processModel;



public interface Join extends ProcessNode {

  public void setMax(int max);

  public int getMax();

  public void setMin(int min);

  public int getMin();

}