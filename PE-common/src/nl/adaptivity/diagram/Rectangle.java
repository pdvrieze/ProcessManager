package nl.adaptivity.diagram;


public final class Rectangle {

  public final double left;
  public final double top;
  public final double width;
  public final double height;

  public Rectangle(double left, double top, double width, double height) {
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
    return new Rectangle((left+pXOffset)*pScale, (top+pYOffset)*pScale, width*pScale, height*pScale);
  }

  @Override
  public String toString() {
    return "Rectangle [l=" + left + ", t=" + top + ", w=" + width + ", h=" + height + "]";
  }

}
