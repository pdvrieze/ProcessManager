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
import org.testng.Assert.fail
import org.testng.annotations.Test
import org.testng.internal.EclipseInterface.*


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