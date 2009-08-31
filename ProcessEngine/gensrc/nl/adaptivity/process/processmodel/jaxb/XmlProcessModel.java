//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.08.27 at 08:15:55 PM CEST 
//


package nl.adaptivity.process.processmodel.jaxb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.*;

import nl.adaptivity.process.engine.ProcessModel;
import nl.adaptivity.process.engine.processModel.*;


/**
 * <p>
 * Java class for TProcessModel complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name=&quot;TProcessModel&quot;&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base=&quot;{http://www.w3.org/2001/XMLSchema}anyType&quot;&gt;
 *       &lt;choice maxOccurs=&quot;unbounded&quot; minOccurs=&quot;0&quot;&gt;
 *         &lt;element ref=&quot;{http://adaptivity.nl/ProcessEngine/}start&quot;/&gt;
 *         &lt;element ref=&quot;{http://adaptivity.nl/ProcessEngine/}activity&quot;/&gt;
 *         &lt;element ref=&quot;{http://adaptivity.nl/ProcessEngine/}join&quot;/&gt;
 *         &lt;element ref=&quot;{http://adaptivity.nl/ProcessEngine/}end&quot;/&gt;
 *       &lt;/choice&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "ProcessModel", propOrder = { "nodes" })
@XmlRootElement(name = "processModel")
public class XmlProcessModel {
  
  public XmlProcessModel() {
    
  }
  
  public XmlProcessModel(ProcessModel m) {
    nodes = Arrays.asList(m.getModelNodes());
  }

  @XmlElementRefs( { @XmlElementRef(name = "end", type = EndNode.class), @XmlElementRef(name = "activity", type = Activity.class),
                    @XmlElementRef(name = "start", type = StartNode.class), @XmlElementRef(name = "join", type = Join.class) })
  protected List<ProcessNode> nodes;

  /**
   * Gets the value of the startOrActivityOrJoin property.
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot.
   * Therefore any modification you make to the returned list will be present
   * inside the JAXB object. This is why there is not a <CODE>set</CODE> method
   * for the startOrActivityOrJoin property.
   * <p>
   * For example, to add a new item, do as follows:
   * 
   * <pre>
   * getStartOrActivityOrJoin().add(newItem);
   * </pre>
   * <p>
   * Objects of the following type(s) are allowed in the list {@link EndNode }
   * {@link Activity } {@link StartNode } {@link Join }
   */
  public List<ProcessNode> getNodes() {
    if (nodes == null) {
      nodes = new ArrayList<ProcessNode>();
    }
    return this.nodes;
  }

  public ProcessModel toProcessModel() {
    return new ProcessModel(this);
  }
  
}
