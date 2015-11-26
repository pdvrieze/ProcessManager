package nl.adaptivity.process.diagram;


import nl.adaptivity.process.clientProcessModel.ClientJoinSplit;

import static nl.adaptivity.process.diagram.DrawableProcessModel.*;


public interface DrawableJoinSplit extends ClientJoinSplit<DrawableProcessNode, DrawableProcessModel>, DrawableProcessNode{

  boolean CURVED_ARROWS=true;
  boolean TEXT_DESC=true;

  double STROKEEXTEND = Math.sqrt(2)*STROKEWIDTH;
  double REFERENCE_OFFSET_X = (JOINWIDTH + STROKEEXTEND) / 2;
  double REFERENCE_OFFSET_Y = (JOINHEIGHT + STROKEEXTEND) / 2;
  double HORIZONTALDECORATIONLEN = JOINWIDTH*0.4;
  double CENTERX = (JOINWIDTH+STROKEEXTEND)/2;
  double CENTERY = (JOINHEIGHT+STROKEEXTEND)/2;
  double ARROWHEADANGLE = (35*Math.PI)/180;
  double ARROWLEN = JOINWIDTH*0.15;
  double ARROWCONTROLRATIO=0.85;

}