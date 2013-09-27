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

  public double right() {
    return left+width;
  }

  public float rightf() {
    return (float) right();
  }

  public double bottom() {
    return top+height;
  }
  
  public float bottomf() {
    return (float) bottom();
  }

  public float widthf() {
    return (float) width;
  }

  public float heightf() {
    return (float) height;
  }

  /**
   * Create an offsetted rectangle. The offsets should not be prescaled. They will be scaled in the method.
   * The scaling is from the top left of the rectangle.
   * @param pXOffset The x offset.
   * @param pYOffset The y offset.
   * @param pScale The scaling needed.
   * @return A new rectangle that is moved from the original one.
   */
  public Rectangle offsetScaled(double pXOffset, double pYOffset, double pScale) {
    return new Rectangle((left+pXOffset)*pScale, (top+pYOffset)*pScale, width*pScale, height*pScale);
  }

  @Override
  public String toString() {
    return "Rectangle [l=" + left + ", t=" + top + ", w=" + width + ", h=" + height + "]";
  }

}
