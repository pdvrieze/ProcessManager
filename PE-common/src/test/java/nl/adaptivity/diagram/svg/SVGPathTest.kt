/*
 * Copyright (c) 2017.
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

package nl.adaptivity.diagram.svg

import nl.adaptivity.diagram.Rectangle
import nl.adaptivity.xml.XmlStreaming
import org.testng.Assert
import org.testng.Assert.fail
import org.testng.annotations.Test
import org.testng.internal.EclipseInterface.*
import java.io.CharArrayWriter


/**
 * Created by pdvrieze on 03/07/17.
 */
class SVGPathTest {

  @Test
  fun testCubicBounds1() {
    val canvas = SVGCanvas(JVMTextMeasurer()).apply {
      val rect = Rectangle(0.0,0.0,0.0,0.0)
      val pen = strategy.newPen().apply {
        strokeWidth = 0.02
        setColor(0,0,0)
      }
      val rectPen = strategy.newPen().apply { strokeWidth=0.01; setColor(0,0,255) }
      val spline = SVGPath().apply {
        moveTo(0.0,0.0)
          .cubicTo(-1.0, -1.0, 6.0,5.0, 4.0, 4.0)
      }.also { drawPath(it, pen, null) }
      spline.getBounds(rect, pen)
      assertEquals(rect, bounds)
      assertEquals(bounds, Rectangle(-0.108, -0.123, 4.480, 4.246))


      drawRect(rect, rectPen)
    }
    XmlStreaming.newWriter(System.out, "UTF-8").use { xmlWriter ->
      canvas.serialize(xmlWriter)
    }
    assertEquals(canvas.bounds, Rectangle(-0.113, -0.128, 4.490, 4.256))
  }

  @Test
  fun testCubicBounds2() {
    val pen = SVGPen(JVMTextMeasurer()).apply { strokeWidth=0.0 }
    val spline = SVGPath().apply {
      moveTo(30.0, 70.0)
      cubicTo(0.0, 270.0,
              290.0, 110.0,
              200.0, 100.0)
    }
    val bounds = Rectangle(0.0,0.0,0.0,0.0)
    spline.getBounds(bounds, pen)
    assertEquals(bounds, Rectangle(27.81, 70.0, 217.396 - 27.81, 170.03 - 70.0),0.05)
  }

  @Test
  fun testSvgComplex() {
    val canvas = SVGCanvas(JVMTextMeasurer()).apply {
      run {
        val splinePen = strategy.newPen().apply {
          setColor(255, 0, 0)
          strokeWidth = 0.5
        }
        val spline = strategy.newPath().apply {
          moveTo(10.0, 5.0)
          lineTo(10.0, 15.0)
          cubicTo(10.0, 45.0, 50.0, -10.0, 60.0, 0.0)
          cubicTo(70.0, 10.0, 60.0, 70.0, 20.0, 70.0)
        }
        val boundRect = Rectangle()
        val boundPen = strategy.newPen().apply {
          setColor(0, 0, 0)
          strokeWidth = 0.3
        }
        spline.getBounds(boundRect, splinePen)
        boundRect.outset(boundPen.strokeWidth / 2)
        drawRect(boundRect, boundPen)
        drawPath(spline, splinePen, null)
      }
      run {
        val circleStroke = strategy.newPen().apply {
          setColor(0,0,255)
          strokeWidth=2.0
        }
        val circleFill = strategy.newPen().apply {
          setColor(0,255,0, 48)
        }
        drawCircle(20.0, 80.0, 15.0, circleStroke, circleFill)
      }
    }
    assertEquals(canvas.bounds, Rectangle(4.0,-1.783,60.157,97.783))
    val writer = CharArrayWriter()
    XmlStreaming.newWriter(writer).use {
      canvas.serialize(it)
    }
    val RealSVG="<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"60.15679774997896\" height=\"97.78326010523611\">" +
                "<g transform=\"translate(-4.0,1.783260105236116)\">" +
                  "<rect x=\"9.6\" y=\"-1.6332601052361162\" width=\"54.40679774997896\" height=\"72.03326010523611\" style=\"stroke: #000000; stroke-width: 0.3; fill:none; \"/>" +
                  "<path style=\"stroke: #ff0000; stroke-width: 0.5; fill:none; \" d=\"M10.0 5.0 L10.0 15.0 C10.0 45.0 50.0 -10.0 60.0 0.0 C70.0 10.0 60.0 70.0 20.0 70.0 \"/>" +
                  "<circle cx=\"20.0\" cy=\"80.0\" r=\"15.0\" style=\"stroke: #0000ff; stroke-width: 2.0; fill: #00ff00; fill-opacity: 0.188235; \"/>" +
                "</g></svg>"
    Assert.assertEquals(writer.toString(), RealSVG)
  }

}



fun assertEquals(actual: Rectangle, expected: Rectangle, delta: Double = 0.0005, message: String? = null) {
  fun isEqual(first: Double, second: Double): Boolean {
    return when {
      first.isInfinite() -> second.isInfinite()
      first.isNaN() -> second.isNaN()
      else -> Math.abs(first - second) <=delta
    }
  }
  if (!(isEqual(actual.left, expected.left) &&
        isEqual(actual.top, expected.top) &&
        isEqual(actual.width, expected.width) &&
        isEqual(actual.height, expected.height)) ) {
    val msg = if (message==null) {
      ASSERT_LEFT + expected + ASSERT_MIDDLE + actual + ASSERT_RIGHT
    } else {
      message+" "+ASSERT_LEFT + expected + ASSERT_MIDDLE + actual + ASSERT_RIGHT
    }
    fail(msg)
  }
}