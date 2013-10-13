package nl.adaptivity.diagram;


public interface DiagramPath {

  DiagramPath moveTo(double pX, double pY);

  DiagramPath lineTo(double pX, double pY);

  void close();

}
