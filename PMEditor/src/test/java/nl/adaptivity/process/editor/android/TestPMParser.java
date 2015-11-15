package nl.adaptivity.process.editor.android;

import nl.adaptivity.process.diagram.*;
import nl.adaptivity.xml.AndroidXmlReader;
import nl.adaptivity.xml.XmlReader;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Created by pdvrieze on 13/11/15.
 */
public class TestPMParser {

  @Test
  public void testParseSimple() throws XmlPullParserException {
    InputStream inputStream = getClass().getResourceAsStream("/processmodel.xml");
    XmlReader parser = new AndroidXmlReader(inputStream, "UTF-8");
    DrawableProcessModel model = PMParser.parseProcessModel(parser, LayoutAlgorithm.<DrawableProcessNode>nullalgorithm(), LayoutAlgorithm.<DrawableProcessNode>nullalgorithm());
    assertNotNull(model);

    assertEquals("There should be 9 effective elements in the process model (including an introduced split)", 9, model.getChildElements().size());
    DrawableStartNode start = (DrawableStartNode) model.getNode("start");
    DrawableActivity ac1 = (DrawableActivity) model.getNode("ac1");
    DrawableActivity ac2 = (DrawableActivity) model.getNode("ac2");
    DrawableActivity ac3 = (DrawableActivity) model.getNode("ac3");
    DrawableActivity ac4 = (DrawableActivity) model.getNode("ac4");
    DrawableActivity ac5 = (DrawableActivity) model.getNode("ac5");
    DrawableSplit split = (DrawableSplit) model.getNode("split1");
    DrawableJoin j1 = (DrawableJoin) model.getNode("j1");
    DrawableEndNode end = (DrawableEndNode) model.getNode("end");
    assertArrayEquals(new Object[] {start, ac1, ac2, split, ac3, ac5, j1, ac4, end}, model.getChildElements().toArray());

    assertArrayEquals(toArray(), start.getPredecessors().toArray());
    assertArrayEquals(toArray(ac1), start.getSuccessors().toArray());

    assertArrayEquals(toArray(start), ac1.getPredecessors().toArray());
    assertArrayEquals(toArray(split), ac1.getSuccessors().toArray());

    assertArrayEquals(toArray(ac1), split.getPredecessors().toArray());
    assertArrayEquals(toArray(ac2, ac3), split.getSuccessors().toArray());

    assertArrayEquals(toArray(split), ac2.getPredecessors().toArray());
    assertArrayEquals(toArray(j1), ac2.getSuccessors().toArray());

    assertArrayEquals(toArray(split), ac3.getPredecessors().toArray());
    assertArrayEquals(toArray(ac5), ac3.getSuccessors().toArray());

    assertArrayEquals(toArray(j1), ac4.getPredecessors().toArray());
    assertArrayEquals(toArray(end), ac4.getSuccessors().toArray());

    assertArrayEquals(toArray(ac3), ac5.getPredecessors().toArray());
    assertArrayEquals(toArray(j1), ac5.getSuccessors().toArray());

    assertArrayEquals(toArray(ac4), end.getPredecessors().toArray());
    assertArrayEquals(toArray(), end.getSuccessors().toArray());

  }

  private static Object[] toArray(Object... val) {
    return val;
  }

  private static void assertNoneNull(final Object... values) {
    assertNotNull(values);
    int counter = 0;
    for(Object value: values) {
      assertNotNull("Value #"+counter+" should not be null", value);
      ++counter;
    }
  }
}
