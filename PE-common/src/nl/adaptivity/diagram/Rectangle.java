package nl.adaptivity.diagram;


public final class Rectangle implements Cloneable{

  public double left;
  public double top;
  public double width;
  public double height;

  public Rectangle(double left, double top, double width, double height) {
    this.top = top;
    this.left = left;
    this.height = height;
    this.width = width;
  }

  @Override
  public Rectangle clone() {
    try {
      return (Rectangle) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public final float leftf() {
    return (float) left;
  }

  public final float topf() {
    return (float) top;
  }

  public final double right() {
    return left+width;
  }

  public final float rightf() {
    return (float) right();
  }

  public final double bottom() {
    return top+height;
  }

  public final float bottomf() {
    return (float) bottom();
  }

  public final float widthf() {
    return (float) width;
  }

  public final float heightf() {
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
  public final Rectangle offsetScaled(double pXOffset, double pYOffset, double pScale) {
    return new Rectangle((left+pXOffset)*pScale, (top+pYOffset)*pScale, width*pScale, height*pScale);
  }

  @Override
  public final String toString() {
    return "Rectangle [l=" + left + ", t=" + top + ", w=" + width + ", h=" + height + "]";
  }

  public final void set(double pLeft, double pTop, double pWidth, double pHeight) {
    left = pLeft;
    top = pTop;
    width = pWidth;
    height = pHeight;
  }

  public final void set(Rectangle pBounds) {
    left = pBounds.left;
    top = pBounds.top;
    width = pBounds.width;
    height = pBounds.height;
  }

  public final void extendBounds(Rectangle pBounds) {
    double newleft = Math.min(left,  pBounds.left);
    double newtop = Math.min(top,  pBounds.left);
    width = Math.max(right(),  pBounds.right())-newleft;
    height = Math.max(bottom(),  pBounds.bottom())-newtop;
    left = newleft;
    top = newtop;
  }

  public final boolean contains(double pX, double pY) {
    return (pX>=left) &&
           (pY>=top) &&
           (pX<=left+width) &&
           (pY<=top+height);
  }

}
