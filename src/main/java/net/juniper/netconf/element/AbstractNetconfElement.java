package net.juniper.netconf.element;

import static java.lang.String.format;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The base class from which all other NETCONF elements extend.
 */
@Value
@NonFinal
public abstract class AbstractNetconfElement {

    public static final String MESSAGE_SEPARATOR = "]]>]]>";
    public static final String URN_XML_NS_NETCONF_BASE_1_0 =
        "urn:ietf:params:xml:ns:netconf:base:1.0";
    public static final String URN_IETF_PARAMS_NETCONF_BASE_1_0 =
        "urn:ietf:params:netconf:base:1.0";

    /**
     * The W3C document representing this element. Note that although ths field is mutable, it
     * should not be changed. Altering this field will not change any properties of this class,
     * not even it's XML representation. It can however be used to extract additional information
     * received in the message that is not directly exposed by the class.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "There is little alternative")
    Document document;

    /**
     * The XML representation of this element.
     */
    @ToString.Exclude
    String xml;

    protected AbstractNetconfElement(final Document document) {
        this.document = document;
        this.xml = createXml(document);
    }

    protected static Document createBlankDocument() {
        try {
            return createDocumentBuilderFactory().newDocumentBuilder().newDocument();
        } catch (final ParserConfigurationException e) {
            throw new IllegalStateException("Unable to create document builder", e);
        }
    }

    protected static DocumentBuilderFactory createDocumentBuilderFactory() {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        return documentBuilderFactory;
    }

    protected static String createXml(final Document document) {
        try {
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            final StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (final TransformerException e) {
            throw new IllegalStateException("Unable to transform document to XML", e);
        }
    }

    protected static String getXpathFor(final String elementName) {
        return format(
            "/*[namespace-uri()='urn:ietf:params:xml:ns:netconf:base:1.0' and local-name()='%s']",
            elementName
        );
    }

    protected static Element appendElementWithText(
        final Document document,
        final Element parentElement,
        final String namespacePrefix,
        final String elementName,
        final String text
    ) {

        if (text != null) {
            final Element childElement =
                document.createElementNS(URN_XML_NS_NETCONF_BASE_1_0, elementName);
            childElement.setPrefix(namespacePrefix);
            childElement.setTextContent(text);
            parentElement.appendChild(childElement);
            return childElement;
        } else {
            return null;
        }
    }

    protected static String getAttribute(final Element element, final String attributeName) {
        if (element != null && element.hasAttribute(attributeName)) {
            return element.getAttribute(attributeName);
        } else {
            return null;
        }
    }

    protected static String getTextContent(final Element element) {
        if (element == null) {
            return null;
        } else {
            return trim(element.getTextContent());
        }
    }

    protected static String trim(final String string) {
        return string == null ? null : string.trim();
    }
}
