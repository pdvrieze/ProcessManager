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

package nl.adaptivity.process.diagram.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import nl.adaptivity.process.ProcessConsts.Endpoints.UserTaskServiceDescriptor;
import nl.adaptivity.process.diagram.DrawableActivity;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.STUB_DRAWABLE_BUILD_HELPER;
import nl.adaptivity.process.processModel.Activity;
import nl.adaptivity.process.processModel.Activity.Builder;
import nl.adaptivity.process.processModel.ProcessModel.BuildHelper;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.processModel.XmlMessage;
import nl.adaptivity.process.processModel.XmlResultType;
import nl.adaptivity.process.tasks.EditableUserTask;
import nl.adaptivity.process.tasks.PostTask;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identified;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlStreaming;
import org.jetbrains.annotations.NotNull;
import org.w3.soapEnvelope.Envelope;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * Activity implementation that is parcelable (can be passed along to other activities.
 * Created by pdvrieze on 15/01/16.
 */
public class ParcelableActivity extends DrawableActivity
  implements Parcelable {

  private static final String TAG = "ParcelableActivity";

  public static final Creator<ParcelableActivity> CREATOR = new Creator<ParcelableActivity>() {
    @Override
    public ParcelableActivity createFromParcel(final Parcel in) {
      return new ParcelableActivity(in);
    }

    @Override
    public ParcelableActivity[] newArray(final int size) {
      return new ParcelableActivity[size];
    }
  };

  private static DrawableActivity.Builder fromParcel(final Parcel source) {
    DrawableActivity.Builder builder = new DrawableActivity.Builder();
    builder.setId(source.readString());
    builder.setLabel(source.readString());
    builder.setName(source.readString());
    builder.setX(source.readDouble());
    builder.setY(source.readDouble());

    builder.setCondition(source.readString());
    builder.setPredecessors(fromIdStrings(source.createStringArray()));
    builder.setSuccessors(fromIdStrings(source.createStringArray()));

    final String strMessage = source.readString();
    Log.d(TAG, "deserializing message:\n"+strMessage);
    if (strMessage!=null && strMessage.length()>0) {
      try {
        builder.setMessage(XmlStreaming.deSerialize(new StringReader(strMessage), XmlMessage.class));
      } catch (XmlException e) {
        throw new RuntimeException(e);
      }
    }

    builder.setDefines(readDefines(source));
    builder.setResults(readResults(source));


    return builder;
  }

  private ParcelableActivity(final Parcel source) {
    this(fromParcel(source));
  }

  private static DrawableActivity.Builder builder(Activity<?,?> orig, boolean compat) {
    final DrawableActivity.Builder builder = new DrawableActivity.Builder(orig);
    builder.setCompat(compat);
    return builder;
  }

  public ParcelableActivity(final Activity<?, ?> orig, final boolean compat) {
    super(builder(orig, compat), STUB_DRAWABLE_BUILD_HELPER.INSTANCE);
  }

  public ParcelableActivity(@NotNull final Activity.Builder<?, ?> builder,
                            @NotNull final BuildHelper<DrawableProcessNode, DrawableProcessModel> buildHelper) {
    super(builder, buildHelper);
  }

  public ParcelableActivity(@NotNull final Activity.Builder<?, ?> builder) {
    this(builder, STUB_DRAWABLE_BUILD_HELPER.INSTANCE);
  }

  public EditableUserTask getUserTask() {
    final XmlMessage message = XmlMessage.get(getMessage());
    if (message!=null && UserTaskServiceDescriptor.SERVICENAME.equals(message.getService()) &&
            UserTaskServiceDescriptor.ENDPOINT.equals(message.getEndpoint())) {
      try {
        final Envelope<PostTask> envelope = Envelope.deserialize(message.getBodyStreamReader(), PostTask.FACTORY);
        return envelope.getBody().getBodyContent().getTask();
      } catch (XmlException e) {
        Log.e(TAG, "getUserTask", e);
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  public static ParcelableActivity newInstance(final Activity<?,?> orig, final boolean compat) {
    return new ParcelableActivity(orig, compat);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(final Parcel dest, final int flags) {
    dest.writeByte((byte) (isCompat()? 1 : 0));
    dest.writeString(getId());
    dest.writeString(getLabel());
    dest.writeString(getName());
    dest.writeDouble(getX());
    dest.writeDouble(getY());

    dest.writeString(getCondition());
    dest.writeStringArray(toIdStrings(getPredecessors()));
    dest.writeStringArray(toIdStrings(getSuccessors()));

    if (getMessage()==null) {
      dest.writeString("");
    } else {
      dest.writeString(nl.adaptivity.xml.XmlUtil.toString(XmlMessage.get(getMessage())));
    }

    writeDefines(dest);
    writeResults(dest);
  }

  private void writeDefines(final Parcel dest) {
    final List<? extends XmlDefineType> defines = getDefines();
    dest.writeInt(defines.size());
    for(final XmlDefineType define:defines) {
      dest.writeString(nl.adaptivity.xml.XmlUtil.toString(define));
    }
  }

  private static List<XmlDefineType> readDefines(final Parcel source) {
    final int count = source.readInt();
    final List<XmlDefineType> result = new ArrayList<>();
    try {
      for (int i = 0; i < count; i++) {
        result.add(XmlStreaming.deSerialize(new StringReader(source.readString()), XmlDefineType.class));
      }
    } catch (XmlException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private void writeResults(final Parcel dest) {
    final List<XmlResultType> results = getResults();
    dest.writeInt(results.size());
    for(final XmlResultType result:results) {
      dest.writeString(nl.adaptivity.xml.XmlUtil.toString(result));
    }
  }

  private static List<XmlResultType> readResults(final Parcel source) {
    final int count = source.readInt();
    final List<XmlResultType> retValue = new ArrayList<>();
    try {
      for (int i = 0; i < count; i++) {
        retValue.add(XmlStreaming.deSerialize(new StringReader(source.readString()), XmlResultType.class));
      }
    } catch (XmlException e) {
      throw new RuntimeException(e);
    }
    return retValue;
  }


  private static String[] toIdStrings(final Set<? extends Identifiable> set) {
    final String[] result = new String[set.size()];
    int i=0;
    for(final Identifiable elem:set) {
      result[i++]=elem.getId();
    }
    return result;
  }


  private static Collection<? extends Identified> fromIdStrings(final String[] stringArray) {
    final List<Identified> result = new ArrayList<>();
    for(final String s:stringArray) {
      result.add(new Identifier(s));
    }
    return result;
  }

}
