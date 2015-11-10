package nl.adaptivity.process.userMessageHandler.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import nl.adaptivity.process.userMessageHandler.server.UserTask.TaskItem;
import nl.adaptivity.process.util.Constants;

@XmlRootElement(name="item")
@XmlAccessorType(XmlAccessType.NONE)
public class XmlItem implements TaskItem{
  private String aName;
  private String aLabel;
  private String aType;
  private String aValue;
  private String aParams;
  private List<String> aOptions;

  @Override
  @XmlAttribute(name="name")
  public String getName() {
    return aName;
  }

  public void setName(String name) {
    aName = name;
  }

  @Override
  @XmlAttribute(name="label")
  public String getLabel() {
    return aLabel;
  }

  public void setLabel(String label) {
    aLabel = label;
  }

  @Override
  @XmlAttribute(name="params")
  public String getParams() {
    return aParams;
  }

  public void setParams(String params) {
    aParams = params;
  }

  @Override
  @XmlAttribute(name="type")
  public String getType() {
    return aType;
  }

  public void setType(String type) {
    aType = type;
  }

  @Override
  @XmlAttribute(name="value")
  public String getValue() {
    return aValue;
  }

  public void setValue(String value) {
    aValue = value;
  }

  @Override
  public List<String> getOptions() {
    if (aOptions==null) { aOptions = new ArrayList<>(); }
    return aOptions;
  }

  @XmlElement(name="option", namespace=Constants.USER_MESSAGE_HANDLER_NS)
  public void setOptions(List<String> options) {
    aOptions = options;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((aName == null) ? 0 : aName.hashCode());
    result = prime * result + ((aOptions == null || aOptions.isEmpty()) ? 0 : aOptions.hashCode());
    result = prime * result + ((aType == null) ? 0 : aType.hashCode());
    result = prime * result + ((aValue == null) ? 0 : aValue.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    XmlItem other = (XmlItem) obj;
    if (aName == null) {
      if (other.aName != null)
        return false;
    } else if (!aName.equals(other.aName))
      return false;
    if (aOptions==null || aOptions.isEmpty()) {
      if (other.aOptions!=null && ! aOptions.isEmpty())
        return false;
    } else if (!aOptions.equals(other.aOptions))
      return false;
    if (aType == null) {
      if (other.aType != null)
        return false;
    } else if (!aType.equals(other.aType))
      return false;
    if (aValue == null) {
      if (other.aValue != null)
        return false;
    } else if (!aValue.equals(other.aValue))
      return false;
    return true;
  }

  public static Collection<XmlItem> get(Collection<? extends TaskItem> source) {
    if (source.isEmpty()) {
      return Collections.emptyList();
    }
    if (source.size()==1) {
      return Collections.singleton(get(source.iterator().next()));
    }
    ArrayList<XmlItem> result = new ArrayList<>(source.size());
    for(TaskItem item: source) {
      result.add(get(item));
    }
    return result;
  }

  public static XmlItem get(TaskItem orig) {
    if (orig instanceof XmlItem) { return (XmlItem) orig; }
    if (orig == null) { return null; }
    XmlItem result = new XmlItem();
    result.aName = orig.getName();
    result.aLabel = orig.getLabel();
    result.aType = orig.getType();
    result.aValue = orig.getValue();
    result.aParams = orig.getParams();
    result.aOptions = new ArrayList<>(orig.getOptions());
    return result;
  }
}