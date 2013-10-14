package nl.adaptivity.diagram;


public interface DrawingStrategy<S extends DrawingStrategy<S>> {

  public Pen<S> newPen();

  public DiagramPath<S> newPath();

}
