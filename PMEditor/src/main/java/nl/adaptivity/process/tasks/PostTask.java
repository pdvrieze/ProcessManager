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

package nl.adaptivity.process.tasks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import net.devrieze.util.StringUtil;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.ICompactFragment;
import nl.adaptivity.util.xml.SimpleXmlDeserializable;
import nl.adaptivity.xml.*;

import javax.xml.namespace.QName;


/**
 * Created by pdvrieze on 19/01/16.
 */
@XmlDeserializer(PostTask.Factory.class)
public class PostTask implements SimpleXmlDeserializable, XmlSerializable {

  public static final String ELEMENTLOCALNAME = "postTask";
  public static final QName ELEMENTNAME=new QName(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME,
                                                  Constants.USER_MESSAGE_HANDLER_NS_PREFIX);

  public static class Factory implements XmlDeserializerFactory<PostTask> {

    @Override
    public PostTask deserialize(final XmlReader reader) {
      return PostTask.deserialize(reader);
    }
  }

  public static final CompactFragment DEFAULT_REPLIES_PARAM  =
      new CompactFragment(new SimpleNamespaceContext("jbi", Constants.MODIFY_NS_STR),
                                     "<jbi:element value=\"endpoint\"/>");
  public static final String REPLIESPARAM_LOCALNAME = "repliesParam";
  public static final QName  REPLIESPARAM_NAME      = new QName(Constants.USER_MESSAGE_HANDLER_NS, REPLIESPARAM_LOCALNAME,
                                                          Constants.USER_MESSAGE_HANDLER_NS_PREFIX);
  public static final String TASKPARAM_LOCALNAME    = "taskParam";
  public static final QName  TASKPARAM_NAME         = new QName(Constants.USER_MESSAGE_HANDLER_NS, TASKPARAM_LOCALNAME,
                                                       Constants.USER_MESSAGE_HANDLER_NS_PREFIX);

  public static final Factory FACTORY = new Factory();

  private ICompactFragment mReplies;
  private EditableUserTask mTask;

  public PostTask(final EditableUserTask task) {
    this();
    mTask = task;
  }

  public PostTask() {
  }

  @NonNull
  public ICompactFragment getReplies() {
    if (mReplies==null) {
      return DEFAULT_REPLIES_PARAM;
    }
    return mReplies;
  }

  /**
   * Set the replies parameter. If not set, a default parameter will be passed.
   * @param replies The content of the replies parameter.
   */
  public void setReplies(@Nullable final ICompactFragment replies) {
    mReplies = replies;
  }

  public EditableUserTask getTask() {
    return mTask;
  }

  public void setTask(final EditableUserTask userTask) {
    mTask = userTask;
  }

  @Override
  public void serialize(final XmlWriter out) {
    XmlWriterUtil.smartStartTag(out, ELEMENTNAME);
    if (mTask!=null || mReplies!=null) {
      XmlWriterUtil.smartStartTag(out, REPLIESPARAM_NAME);
      getReplies().serialize(out);
      XmlWriterUtil.endTag(out, REPLIESPARAM_NAME);
      XmlWriterUtil.smartStartTag(out, TASKPARAM_NAME);
      mTask.serialize(out);
      XmlWriterUtil.endTag(out, TASKPARAM_NAME);
    }
    XmlWriterUtil.endTag(out, ELEMENTNAME);
  }

  private static PostTask deserialize(final XmlReader in) {
    return XmlUtilDeserializable.deserializeHelper(new PostTask(), in);
  }

  @Override
  public boolean deserializeChild(@NonNull final XmlReader reader) {
    if (StringUtil.isEqual(Constants.USER_MESSAGE_HANDLER_NS, reader.getNamespaceUri())) {
      switch (reader.getLocalName().toString()) {
        case REPLIESPARAM_LOCALNAME:
          mReplies = XmlReaderUtil.elementContentToFragment(reader);
          return true;
        case TASKPARAM_LOCALNAME:
          reader.next();//The param tag has been handled.
          mTask = EditableUserTask.deserialize(reader);
          reader.nextTag();
          reader.require(EventType.END_ELEMENT, Constants.USER_MESSAGE_HANDLER_NS, TASKPARAM_LOCALNAME);
          return true;
      }
    }
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(final XmlReader reader) {

  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }
}
