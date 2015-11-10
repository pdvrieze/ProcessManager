//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.08.06 at 08:14:28 PM BST
//


package nl.adaptivity.process.exec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the nl.adaptivity.process.exec package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ProcessNodeInstance_QNAME = new QName("http://adaptivity.nl/ProcessEngine/", "processNodeInstance");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: nl.adaptivity.process.exec
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link XmlProcessNodeInstance }
     *
     */
    @NotNull
    public XmlProcessNodeInstance createXmlProcessNodeInstance() {
        return new XmlProcessNodeInstance();
    }

    /**
     * Create an instance of {@link XmlProcessNodeInstance.Body }
     *
     */
    @NotNull
    public XmlProcessNodeInstance.Body createXmlProcessNodeInstanceBody() {
        return new XmlProcessNodeInstance.Body();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XmlProcessNodeInstance }{@code >}}
     *
     */
    @Nullable
    @XmlElementDecl(namespace = "http://adaptivity.nl/ProcessEngine/", name = "processNodeInstance")
    public JAXBElement<XmlProcessNodeInstance> createProcessNodeInstance(final XmlProcessNodeInstance value) {
        return new JAXBElement<>(_ProcessNodeInstance_QNAME, XmlProcessNodeInstance.class, null, value);
    }

}
