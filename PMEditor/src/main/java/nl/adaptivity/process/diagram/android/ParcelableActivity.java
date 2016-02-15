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
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.diagram.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import nl.adaptivity.process.ProcessConsts.Endpoints.UserTaskServiceDescriptor;
import nl.adaptivity.process.clientProcessModel.ClientActivityNode;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.clientProcessModel.ClientProcessNode;
import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.tasks.EditableUserTask;
import nl.adaptivity.process.tasks.PostTask;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import org.w3.soapEnvelope.Envelope;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Activity implementation that is parcelable (can be passed along to other activities.
 * Created by pdvrieze on 15/01/16.
 */
public class ParcelableActivity<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends ClientActivityNode<T,M> implements Parcelable {

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

  private ParcelableActivity(final Parcel source) {
    super((M) null, source.readByte()!=0);
    setId(source.readString());
    setLabel(source.readString());
    setName(source.readString());
    setX(source.readDouble());
    setY(source.readDouble());

    setCondition(source.readString());
    setPredecessors(fromIdStrings(source.createStringArray()));
    setSuccessors(fromIdStrings(source.createStringArray()));

    final String strMessage = source.readString();
    Log.d(TAG, "deserializing message:\n"+strMessage);
    if (strMessage!=null && strMessage.length()>0) {
      try {
        setMessage(XmlUtil.deSerialize(new StringReader(strMessage), XmlMessage.class));
      } catch (XmlException e) {
        throw new RuntimeException(e);
      }
    }

    setDefines(readDefines(source));
    setResults(readResults(source));
  }

  public ParcelableActivity(final Activity<?, ?> orig, final boolean compat) {
    super(orig, compat);
  }

  public EditableUserTask getUserTask() {
    final XmlMessage message = getMessage();
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

  public static <T extends ClientProcessNode<T,M>, M extends ClientProcessModel<T,M>> ParcelableActivity<T,M> newInstance(final Activity<T,M> orig, final boolean compat) {
    return new ParcelableActivity<>(orig, compat);
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
      dest.writeString(XmlUtil.toString(getMessage()));
    }

    writeDefines(dest);
    writeResults(dest);
  }

  private void writeDefines(final Parcel dest) {
    final List<XmlDefineType> defines = getDefines();
    dest.writeInt(defines.size());
    for(final XmlDefineType define:defines) {
      dest.writeString(XmlUtil.toString(define));
    }
  }

  private static List<XmlDefineType> readDefines(final Parcel source) {
    final int count = source.readInt();
    final List<XmlDefineType> result = new ArrayList<>();
    try {
      for (int i = 0; i < count; i++) {
        result.add(XmlUtil.deSerialize(new StringReader(source.readString()),XmlDefineType.class));
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
      dest.writeString(XmlUtil.toString(result));
    }
  }

  private static List<XmlResultType> readResults(final Parcel source) {
    final int count = source.readInt();
    final List<XmlResultType> retValue = new ArrayList<>();
    try {
      for (int i = 0; i < count; i++) {
        retValue.add(XmlUtil.deSerialize(new StringReader(source.readString()),XmlResultType.class));
      }
    } catch (XmlException e) {
      throw new RuntimeException(e);
    }
    return retValue;
  }


  private static String[] toIdStrings(final ProcessNodeSet<? extends Identifiable> set) {
    final String[] result = new String[set.size()];
    int i=0;
    for(final Identifiable elem:set) {
      result[i++]=elem.getId();
    }
    return result;
  }


  private static Collection<? extends Identifiable> fromIdStrings(final String[] stringArray) {
    final List<Identifiable> result = new ArrayList<>();
    for(final String s:stringArray) {
      result.add(new Identifier(s));
    }
    return result;
  }

}
