package nl.adaptivity.diagram;


public interface DiagramPath<S extends DrawingStrategy<S>> {

  DiagramPath<S> moveTo(double pX, double pY);

  DiagramPath<S> lineTo(double pX, double pY);

  void close();

}
