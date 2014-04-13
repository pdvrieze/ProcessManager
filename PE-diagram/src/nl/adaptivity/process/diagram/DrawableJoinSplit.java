package nl.adaptivity.process.diagram;


import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Canvas.TextPos;
import nl.adaptivity.diagram.DiagramPath;
import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.DrawingStrategy;
import nl.adaptivity.diagram.ItemCache;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientJoinSplit;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import static nl.adaptivity.process.diagram.DrawableProcessModel.*;


public abstract class DrawableJoinSplit extends ClientJoinSplit<DrawableProcessNode> implements DrawableProcessNode {

  protected static final boolean CURVED_ARROWS=true;
  private static final boolean TEXT_DESC=true;

  protected static final double STROKEEXTEND = Math.sqrt(2)*STROKEWIDTH;
  private static final double REFERENCE_OFFSET_X = (JOINWIDTH+STROKEEXTEND)/2;
  private static final double REFERENCE_OFFSET_Y = (JOINHEIGHT+STROKEEXTEND)/2;
  protected static final double HORIZONTALDECORATIONLEN = JOINWIDTH*0.4;
  protected static final double CENTERX = (JOINWIDTH+STROKEEXTEND)/2;
  protected static final double CENTERY = (JOINHEIGHT+STROKEEXTEND)/2;
  protected static final double ARROWHEADANGLE = (35*Math.PI)/180;
  protected static final double ARROWLEN = JOINWIDTH*0.15;
  protected static final double ARROWCONTROLRATIO=0.85;
  protected final ItemCache aItems = new ItemCache();
  private int aState = STATE_DEFAULT;

  public DrawableJoinSplit(ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pOwner);
  }

  public DrawableJoinSplit(String pId, ClientProcessModel<DrawableProcessNode> pOwner) {
    super(pId, pOwner);
  }

  public DrawableJoinSplit(DrawableJoinSplit pOrig) {
    super(pOrig);
    aState = pOrig.aState;
  }

  @Override
  public abstract DrawableJoinSplit clone();

  @Override
  public Rectangle getBounds() {
    return new Rectangle(getX()-REFERENCE_OFFSET_X, getY()-REFERENCE_OFFSET_Y, JOINHEIGHT+STROKEEXTEND, JOINWIDTH+STROKEEXTEND);
  }

  @Override
  public void move(double pX, double pY) {
    setX(getX()+pX);
    setY(getY()+pY);
  }

  @Override
  public void setPos(double pLeft, double pTop) {
    setX(pLeft+REFERENCE_OFFSET_X);
    setY(pLeft+REFERENCE_OFFSET_Y);
  }

  @Override
  public Drawable getItemAt(double pX, double pY) {
    final double realradiusX=(JOINWIDTH+STROKEEXTEND)/2;
    final double realradiusY=(JOINHEIGHT+STROKEEXTEND)/2;
    return ((Math.abs(pX-getX())<=realradiusX) && (Math.abs(pY-getY())<=realradiusY)) ? this : null;
  }

  @Override
  public int getState() {
    return aState ;
  }

  @Override
  public void setState(int pState) {
    aState = pState;
  }

  @Override
  public <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void draw(Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds) {
    final S strategy = pCanvas.getStrategy();
    PATH_T path = aItems.getPath(strategy, 0);
    final double dx = JOINWIDTH/2;
    final double hse = STROKEEXTEND/2;
    if (path==null) {
      final double dy = JOINHEIGHT/2;
      path = strategy.newPath();
      path.moveTo(hse,dy+hse)
          .lineTo(dx+hse, hse)
          .lineTo(JOINWIDTH+hse, dy+hse)
          .lineTo(dx+hse, JOINHEIGHT+hse)
          .close();
      aItems.setPath(strategy, 0, path);
    }
    if (hasPos()) {
      PEN_T linePen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, aState & ~STATE_TOUCHED);
      PEN_T bgPen = pCanvas.getTheme().getPen(ProcessThemeItems.BACKGROUND, aState);

      if ((aState&STATE_TOUCHED)!=0) {
        PEN_T touchedPen = pCanvas.getTheme().getPen(ProcessThemeItems.LINE, STATE_TOUCHED);
        pCanvas.drawPath(path, touchedPen, null);
      }
      pCanvas.drawPath(path, linePen, bgPen);

      if (getOwner()!=null || getMin()>=0 || getMax()>=0) {
        PEN_T textPen = pCanvas.getTheme().getPen(ProcessThemeItems.DIAGRAMTEXT, aState);
        String s = getMinMaxText();
  
        pCanvas.drawText(TextPos.DESCENT, hse+dx, -hse, s, Double.MAX_VALUE, textPen);
      }
    }
  }

  public String getMinMaxText() {
    if (TEXT_DESC) {
      if (getMin()==1 && getMax()==1) {
        return "xor";
      } else if (getMin()==1 && getMax()>=getSuccessors().size()) {
        return "or";
      } else if (getMin()==getMax() && getMax()>=getSuccessors().size()) {
        return "and";
      }
    }
    StringBuilder str = new StringBuilder();
    if (getMin()<0) {
      str.append("?...");
    } else if (getMin()==getMax()){
      return Integer.toString(getMin());
    } else {
      str.append(getMin()).append("...");
    }
    if (getMax()<0) {
      str.append("?");
    } else {
      str.append(getMax());
    }
    return str.toString();
  }

}