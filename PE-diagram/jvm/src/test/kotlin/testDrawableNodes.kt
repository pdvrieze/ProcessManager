import nl.adaptivity.diagram.svg.JVMTextMeasurer
import nl.adaptivity.diagram.svg.SVGCanvas
import nl.adaptivity.xml.XmlStreaming
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringWriter
import nl.adaptivity.process.diagram.*
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


class NodeTest {
  @Test
  fun testStartNode() {
    val EXPECTED_SVG= ("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"21.0\" height=\"21.0\">" +
                       "<circle cx=\"10.5\" cy=\"10.5\" r=\"10.5\" style=\"stroke:none; fill: #000000; \"/>" +
                       "</svg>").trimIndent()
      assertEquals(EXPECTED_SVG, testDrawNode(DrawableStartNode.Builder()))
  }

  @Test
  fun testActivityNode() {
    val EXPECTED_SVG= "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"33.0\" height=\"33.0\">" +
                      "<rect x=\"0.5\" y=\"0.5\" width=\"32.0\" height=\"32.0\" rx=\"8.0\" ry=\"8.0\" style=\"stroke:none; fill: #ffffff; \"/>" +
                      "<rect x=\"0.5\" y=\"0.5\" width=\"32.0\" height=\"32.0\" rx=\"8.0\" ry=\"8.0\" style=\"stroke: #000000; stroke-width: 1.0; fill:none; \"/>" +
                      "</svg>"
      assertEquals(testDrawNode(DrawableActivity.Builder()), EXPECTED_SVG)
  }

  @Test
  fun testSplitNode() {
    val EXPECTED_SVG= "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"24.70710678118655\" height=\"24.70710678118655\">" +
                      "<g transform=\"translate(-0.35355339059327384,-0.3535533905932738)\">" +
                      "<path style=\"stroke: #000000; stroke-width: 1.0; fill: #ffffff; \" d=\"M0.7071067811865476 12.707106781186548 L12.707106781186548 0.7071067811865476 L24.707106781186546 12.707106781186548 L12.707106781186548 24.707106781186546 Z \"/>" +
                      "<path style=\"stroke: #000020; stroke-opacity: 0.690196; stroke-width: 0.85; fill:none; \" d=\"M3.1071067811865465 12.707106781186548 L5.918881681795691 12.707106781186548 C11.688873016277919 12.707106781186548 12.810705255275732 12.603508307097364 16.890705255275734 8.523508307097364 L17.50710678118655 7.907106781186547 M5.918881681795691 12.707106781186548 C11.688873016277919 12.707106781186548 12.810705255275732 12.810705255275732 16.890705255275734 16.890705255275734 L17.50710678118655 17.50710678118655 M13.961798870342601 8.532240220787497 L17.50710678118655 7.907106781186547 L16.881973341585603 11.452414692030496 M16.881973341585603 13.961798870342601 L17.50710678118655 17.50710678118655 L13.961798870342601 16.881973341585603 \"/>" +
                      "</g>" +
                      "</svg>"
      assertEquals(EXPECTED_SVG, testDrawNode(DrawableSplit.Builder().apply { min=-1; max=-1 }))
  }

  @Test
  fun testJoinNode() {
    val EXPECTED_SVG= "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"24.70710678118655\" height=\"24.70710678118655\">" +
                      "<g transform=\"translate(-0.35355339059327384,-0.3535533905932738)\">" +
                      "<path style=\"stroke: #000000; stroke-width: 1.0; fill: #ffffff; \" d=\"M0.7071067811865476 12.707106781186548 L12.707106781186548 0.7071067811865476 L24.707106781186546 12.707106781186548 L12.707106781186548 24.707106781186546 Z \"/>" +
                      "<path style=\"stroke: #000020; stroke-opacity: 0.690196; stroke-width: 0.85; fill:none; \" d=\"M19.495331880577403 12.707106781186548 L20.835383383375998 12.707106781186548 M18.758159421746175 10.642231610322781 L21.707106781186546 12.707106781186548 L18.758159421746175 14.771981952050314 M7.907106781186547 7.907106781186547 C11.987106781186547 11.987106781186547 13.725340546095177 12.707106781186548 19.495331880577403 12.707106781186548 C13.725340546095177 12.707106781186548 11.987106781186547 13.427106781186549 7.907106781186547 17.50710678118655 M19.495331880577403 12.707106781186548 L21.707106781186546 12.707106781186548 \"/>" +
                      "</g>" +
                      "</svg>"
      assertEquals(EXPECTED_SVG, testDrawNode(DrawableJoin.Builder().apply { min=-1; max=-1 }))
  }

  @Test
  fun testEndNode() {
    val EXPECTED_SVG= "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"25.7\" height=\"25.7\">" +
                      "<circle cx=\"12.85\" cy=\"12.85\" r=\"12.0\" style=\"stroke: #000000; stroke-width: 1.7; fill:none; \"/>" +
                      "<circle cx=\"12.85\" cy=\"12.85\" r=\"7.0\" style=\"stroke:none; fill: #000000; \"/>" +
                      "</svg>"
      assertEquals(EXPECTED_SVG, testDrawNode(DrawableEndNode.Builder()))
  }

  fun testDrawNode(node: DrawableProcessNode.Builder<*>): String {
    return StringWriter().also {
      XmlStreaming.newWriter(it).use { xmlWriter ->
        val canvas = SVGCanvas(JVMTextMeasurer())
        val bounds = node.bounds
        node.x = bounds.width / 2.0
        node.y = bounds.height / 2.0
        node.draw(canvas, null)
        canvas.serialize(xmlWriter)
      }
    }.toString()

  }
}
