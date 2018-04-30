/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.diagram;

import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.svg.JVMTextMeasurer;
import nl.adaptivity.diagram.svg.JVMTextMeasurer.JvmMeasureInfo;
import nl.adaptivity.diagram.svg.SVGCanvas;
import nl.adaptivity.xml.XmlStreaming;
import nl.adaptivity.xml.XmlWriter;

import java.io.FileWriter;
import java.io.IOException;


/**
 * Created by pdvrieze on 07/06/16.
 */
public class ImageGen {


  public static void main(String[] args) throws IOException {
    drawNode(new DrawableStartNode.Builder(), "startNode.svg");
    drawNode(new DrawableSplit.Builder(), "split.svg");
    drawNode(new DrawableJoin.Builder(), "join.svg");
    drawNode(new DrawableActivity.Builder(), "activity.svg");
    drawNode(new DrawableEndNode.Builder(), "endNode.svg");
  }

  private static void drawNode(final DrawableProcessNode.Builder drawable, final String fileName) throws IOException {
    try (final FileWriter fileWriter = new FileWriter(fileName);
         final XmlWriter xmlWriter = XmlStreaming.newWriter(fileWriter)) {
      SVGCanvas<JvmMeasureInfo> canvas = new SVGCanvas<>(new JVMTextMeasurer());
      Rectangle                 bounds = drawable.getBounds();
      drawable.setX(bounds.width/2d);
      drawable.setY(bounds.height/2d);
      drawable.draw(canvas, null);
      canvas.serialize(xmlWriter);
    }
  }
}
