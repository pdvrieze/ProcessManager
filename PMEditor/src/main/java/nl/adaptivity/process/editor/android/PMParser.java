/*
 * Copyright (c) 2016.
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

package nl.adaptivity.process.editor.android;

import android.util.Log;
import nl.adaptivity.process.diagram.AbstractLayoutStepper;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.diagram.RootDrawableProcessModel;
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Builder;
import nl.adaptivity.process.processModel.RootProcessModel;
import nl.adaptivity.xml.*;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.util.UUID;

public final class PMParser {

  public static final String MIME_TYPE="application/x-processmodel";

  public static final String NS_PROCESSMODEL="http://adaptivity.nl/ProcessEngine/";

  public static void serializeProcessModel(final OutputStream out, final RootDrawableProcessModel processModel) throws XmlPullParserException, IOException, XmlException {
    final XmlSerializer serializer = getSerializer(out);
    final AndroidXmlWriter writer = new AndroidXmlWriter(serializer);
    processModel.serialize(writer);
    writer.close();
  }

  public static void serializeProcessModel(final Writer out, final RootDrawableProcessModel processModel) throws XmlPullParserException, IOException, XmlException {
    final XmlSerializer serializer = getSerializer(out);
    final AndroidXmlWriter writer = new AndroidXmlWriter(serializer);
    processModel.serialize(writer);
    writer.close();
  }

  public static void exportProcessModel(final OutputStream out, final RootProcessModel<?,?> processModel) throws XmlPullParserException, IOException, XmlException {
    RootDrawableProcessModel sanitizedModel = sanitizeForExport(processModel);
    serializeProcessModel(out, sanitizedModel);
  }

  public static void exportProcessModel(final Writer out, final RootProcessModel<?,?> processModel) throws XmlPullParserException, IOException, XmlException {
    RootDrawableProcessModel sanitizedModel = sanitizeForExport(processModel);
    serializeProcessModel(out, sanitizedModel);
  }

  private static RootDrawableProcessModel sanitizeForExport(final RootProcessModel<?,?> processModel) {
    RootDrawableProcessModel result = RootDrawableProcessModel.get(processModel);
    if (result.getUuid()==null) { result.setUuid(UUID.randomUUID()); }
    result.setHandleValue(-1);
    return result;
  }

  private static BetterXmlSerializer getSerializer() throws XmlPullParserException {
    final XmlSerializer serializer = new BetterXmlSerializer();
    serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", false);
    return (BetterXmlSerializer) serializer;
  }

  public static XmlSerializer getSerializer(final OutputStream out) throws XmlPullParserException, IOException {
    final XmlSerializer serializer = getSerializer();
    try {
      serializer.setOutput(out, "UTF-8");
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new IOException(e);
    }
    return serializer;
  }

  public static XmlSerializer getSerializer(final Writer out) throws XmlPullParserException, IOException {
    final XmlSerializer serializer = getSerializer();
    try {
      serializer.setOutput(out);
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new IOException(e);
    }
    return serializer;
  }

  public static RootDrawableProcessModel.Builder parseProcessModel(final Reader in, final LayoutAlgorithm simpleLayoutAlgorithm, final LayoutAlgorithm advancedAlgorithm) {
    try {
      return parseProcessModel(new AndroidXmlReader(in), simpleLayoutAlgorithm, advancedAlgorithm);
    } catch (Exception e){
      Log.e(PMEditor.class.getSimpleName(), e.getMessage(), e);
      return null;
    }
  }

  public static RootDrawableProcessModel.Builder parseProcessModel(final InputStream in, final LayoutAlgorithm simpleLayoutAlgorithm, final LayoutAlgorithm advancedAlgorithm) {
    try {
      return parseProcessModel(new AndroidXmlReader(in, "UTF-8"), simpleLayoutAlgorithm, advancedAlgorithm);
    } catch (Exception e){
      Log.e(PMEditor.class.getSimpleName(), e.getMessage(), e);
      return null;
    }
  }

  public static RootDrawableProcessModel.Builder parseProcessModel(final XmlReader in, final LayoutAlgorithm simpleLayoutAlgorithm, final LayoutAlgorithm advancedAlgorithm) throws XmlException {
    final Builder result = Builder.deserialize(in);
    if (result.hasUnpositioned()) {
      result.setLayoutAlgorithm(advancedAlgorithm);
      result.layout(new AbstractLayoutStepper());
    } else {
      result.setLayoutAlgorithm(simpleLayoutAlgorithm);
    }
    return result;
  }

}
