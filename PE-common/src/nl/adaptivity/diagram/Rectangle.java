package nl.adaptivity.diagram;


public final class Rectangle {

  public final double left;
  public final double top;
  public final double width;
  public final double height;

  public Rectangle(double top, double left, double height, double width) {
    this.top = top;
    this.left = left;
    this.height = height;
    this.width = width;
  }

  public float leftf() {
    return (float) left;
  }

  public float topf() {
    return (float) top;
  }

  public float rightf() {
    return (float) (left+width);
  }

  public float bottomf() {
    return (float) (top+height);
  }

  public float widthf() {
    return (float) width;
  }

  public float heightf() {
    return (float) height;
  }

  public Rectangle offsetScaled(double pXOffset, double pYOffset, double pScale) {
    return new Rectangle(top*pScale+pXOffset, left*pScale+pYOffset, width*pScale, height*pScale);
  }

}
