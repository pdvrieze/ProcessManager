package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.*;

/**
 * Created by pdvrieze on 17/05/15.
 */
public class DrawableUtil {
    private DrawableUtil(){}

    public static <S extends DrawingStrategy<S, PEN_T, PATH_T>, PEN_T extends Pen<PEN_T>, PATH_T extends DiagramPath<PATH_T>> void defaultDrawLabel(DrawableProcessNode drawable, Canvas<S, PEN_T, PATH_T> pCanvas, Rectangle pClipBounds, double left, double top) {
        // Not yet
    }
}
