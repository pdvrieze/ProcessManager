package nl.adaptivity.diagram;


public interface DiagramPath<PATH_T extends DiagramPath<PATH_T>> {

  PATH_T moveTo(double pX, double pY);

  PATH_T lineTo(double pX, double pY);

  void close();

}
