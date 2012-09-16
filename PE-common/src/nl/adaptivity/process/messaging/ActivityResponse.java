//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.09.15 at 03:25:47 PM BST 
//


package nl.adaptivity.process.messaging;

import javax.xml.bind.annotation.*;

import nl.adaptivity.process.ProcessConsts;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.ws.soap.SoapHelper;


/**
 * <p>The ActivityResponse type is a class that helps with process aware methods.
 * When returning this class from a method this signifies to the {@link SoapHelper} that
 * the method is ProcessAware, and wraps an actual return value. The activityResponse is communicated
 * through the header.</p>
 * 
 * <p>When used by JAXB, instances will result in the right header that signifies task
 * awareness to the process engine. This allows responding to a SOAP message to not signify task
 * completion.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ActivityResponseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="taskState">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="Sent"/>
 *             &lt;enumeration value="Acknowledged"/>
 *             &lt;enumeration value="Taken"/>
 *             &lt;enumeration value="Started"/>
 *             &lt;enumeration value="Complete"/>
 *             &lt;enumeration value="Failed"/>
 *             &lt;enumeration value="Cancelled"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 * @param <T> The type of the actual return value returned in the result of the SOAP message.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = ActivityResponse.ELEMENTNAME+"Type")
@XmlRootElement(name=ActivityResponse.ELEMENTNAME, namespace = ActivityResponse.NAMESPACE)
public class ActivityResponse<T>  {

    public static final String NAMESPACE = ProcessConsts.Engine.NAMESPACE;
    public static final String ELEMENTNAME = "ActivityResponse";
    public static final String ATTRTASKSTATE = "taskState";

    private T aReturnValue;
    private Class<T> aReturnType;
    
    
    @XmlTransient
    private TaskState aTaskState;

    /** Default constructor for jaxb use */
    protected ActivityResponse() {}
    
    /**
     * Create a new ActivityResponse.
     * @param pTaskState The state of the task requested.
     * @param pReturnType The actual return type of the method.
     * @param pReturnValue The value to return.
     */
    protected ActivityResponse(TaskState pTaskState, Class<T> pReturnType, T pReturnValue) {
      aTaskState = pTaskState;
      aReturnType = pReturnType;
      aReturnValue = pReturnValue;
    }

    /**
     * Static helper factory for creating a new ActivityResponse.
     * @param pTaskState The state of the task requested.
     * @param pReturnType The actual return type of the method.
     * @param pReturnValue The value to return.
     * @return
     */
    public static <V> ActivityResponse<V> create(TaskState pTaskState, Class<V> pReturnType, V pReturnValue) {
      return new ActivityResponse<V>(pTaskState, pReturnType, pReturnValue);
    }
    
    /**
     * Gets the value of the taskState property.
     * 
     * @return the name of the {@link TaskState}
     *     
     */
    @XmlAttribute(name = ATTRTASKSTATE)
    public String getTaskStateString() {
        return aTaskState.name();
    }
    
    /**
     * Gets the value of the taskState property.
     * 
     * @return the task state.
     *     
     */
    public TaskState getTaskState() {
      return aTaskState;
    }

    /**
     * Sets the value of the taskState property.
     * 
     * @param value the new task state.
     *     
     */
    public void setTaskStateString(String value) {
        aTaskState = TaskState.valueOf(value);
    }

    /**
     * Sets the value of the taskState property.
     * 
     * @param pTaskState the new task state.
     *     
     */
    public void setTaskState(TaskState pTaskState) {
      aTaskState = pTaskState;
    }
    
    /**
     * Get the embedded return type.
     * @return The embedded return type.
     */
    public Class<T> getReturnType() {
      return aReturnType;
    }
    
    /**
     * Get the actual return value.
     * @return The actual return value.
     */
    public T getReturnValue() {
      return aReturnValue;
    }

}
