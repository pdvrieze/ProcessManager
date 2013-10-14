package nl.adaptivity.diagram;


public interface DrawingStrategy<S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> {

  public PEN_T newPen();

  public PATH_T newPath();

}
