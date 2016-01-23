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

package nl.adaptivity.process.tasks;

import net.devrieze.util.StringUtil;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;

import javax.xml.namespace.QName;


/**
 * Created by pdvrieze on 19/01/16.
 */
@XmlDeserializer(PostTask.Factory.class)
public class PostTask implements SimpleXmlDeserializable, XmlSerializable {

  public static final String ELEMENTLOCALNAME = "postTask";
  public static final QName ELEMENTNAME=new QName(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME, Constants.USER_MESSAGE_HANDLER_NS_PREFIX);

  public static class Factory implements XmlDeserializerFactory<PostTask> {

    @Override
    public PostTask deserialize(final XmlReader in) throws XmlException {
      return PostTask.deserialize(in);
    }
  }

  public static final CompactFragment DEFAULT_REPLIES_PARAM = new CompactFragment(new SimpleNamespaceContext("jbi", Constants.MODIFY_NS_STR), "<jbi:element value=\"endpoint\"/>"
          .toCharArray());
  public static final String REPLIESPARAM_LOCALNAME = "repliesParam";
  public static final QName REPLIESPARAM_NAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, REPLIESPARAM_LOCALNAME, Constants.USER_MESSAGE_HANDLER_NS_PREFIX);
  public static final String TASKPARAM_LOCALNAME = "taskParam";
  public static final QName TASKPARAM_NAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, TASKPARAM_LOCALNAME, Constants.USER_MESSAGE_HANDLER_NS_PREFIX);

  public static Factory FACTORY = new Factory();

  private CompactFragment mReplies;
  private UserTask mTask;

  public CompactFragment getReplies() {
    if (mReplies==null) {
      return DEFAULT_REPLIES_PARAM;
    }
    return mReplies;
  }

  public UserTask getTask() {
    return mTask;
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    if (mTask!=null || mReplies!=null) {
      XmlUtil.writeStartElement(out, REPLIESPARAM_NAME);
      getReplies().serialize(out);
      XmlUtil.writeEndElement(out, REPLIESPARAM_NAME);
      XmlUtil.writeStartElement(out, TASKPARAM_NAME);
      mTask.serialize(out);
      XmlUtil.writeEndElement(out, TASKPARAM_NAME);
    }
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

  private static PostTask deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new PostTask(), in);
  }

  @Override
  public boolean deserializeChild(final XmlReader in) throws XmlException {
    if (StringUtil.isEqual(Constants.USER_MESSAGE_HANDLER_NS,in.getNamespaceUri())) {
      switch (in.getLocalName().toString()) {
        case REPLIESPARAM_LOCALNAME:
          in.next();
          mReplies = XmlUtil.readerToFragment(in);
          return true;
        case TASKPARAM_LOCALNAME:
          mTask = UserTask.deserialize(in);
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
  public void onBeforeDeserializeChildren(final XmlReader in) throws XmlException {

  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }
}
