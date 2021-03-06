package net.juniper.netconf.element;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.ToString;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Class to represent a NETCONF hello element - https://datatracker.ietf.org/doc/html/rfc6241#section-8.1
 */
@Value
@Log4j2
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "There is little alternative")
public class Hello extends AbstractNetconfElement {

    private static final String XPATH_HELLO = getXpathFor("hello");
    private static final String XPATH_HELLO_SESSION_ID
        = XPATH_HELLO + getXpathFor("session-id");
    private static final String XPATH_HELLO_CAPABILITIES
        = XPATH_HELLO + getXpathFor("capabilities");
    private static final String XPATH_HELLO_CAPABILITIES_CAPABILITY
        = XPATH_HELLO_CAPABILITIES + getXpathFor("capability");

    String sessionId;

    @Singular("capability")
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "False positive - list is immutable"
    )
    List<String> capabilities;

    public boolean hasCapability(final String capability) {
        return capabilities.contains(capability);
    }

    /**
     * Creates a Hello object based on the supplied XML.
     *
     * @param xml The XML of the NETCONF &lt;hello&gt;
     * @return a new, immutable, Hello object.
     * @throws ParserConfigurationException If the XML parser cannot be created
     * @throws IOException                  If the XML cannot be read
     * @throws SAXException                 If the XML cannot be parsed
     * @throws XPathExpressionException     If there is a problem in the parsing expressions
     */
    public static Hello from(final String xml)
        throws ParserConfigurationException,
        IOException,
        SAXException,
        XPathExpressionException {

        final Document document = createDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final String sessionId = xPath.evaluate(XPATH_HELLO_SESSION_ID, document);
        final HelloBuilder builder = Hello.builder()
            .originalDocument(document)
            .sessionId(sessionId);
        final NodeList capabilities = (NodeList) xPath.evaluate(
            XPATH_HELLO_CAPABILITIES_CAPABILITY,
            document,
            XPathConstants.NODESET);
        for (int i = 0; i < capabilities.getLength(); i++) {
            final Node node = capabilities.item(i);
            builder.capability(node.getTextContent());
        }
        final Hello hello = builder.build();
        log.trace("hello is: {}", hello::getXml);
        return hello;
    }

    @Builder
    private Hello(
        final Document originalDocument,
        final String namespacePrefix,
        final String sessionId,
        @Singular("capability") final List<String> capabilities
    ) {
        super(getDocument(originalDocument, namespacePrefix, sessionId, capabilities));
        this.sessionId = sessionId;
        this.capabilities = Collections.unmodifiableList(capabilities);
    }

    private static Document getDocument(
        final Document originalDocument,
        final String namespacePrefix,
        final String sessionId,
        final List<String> capabilities
    ) {
        if (originalDocument != null) {
            return originalDocument;
        } else {
            return createDocument(namespacePrefix, sessionId, capabilities);
        }
    }

    private static Document createDocument(
        final String namespacePrefix,
        final String sessionId,
        final List<String> capabilities
    ) {

        final Document createdDocument = createBlankDocument();

        final Element helloElement
            = createdDocument.createElementNS(URN_XML_NS_NETCONF_BASE_1_0, "hello");
        helloElement.setPrefix(namespacePrefix);
        createdDocument.appendChild(helloElement);

        final Element capabilitiesElement
            = createdDocument.createElementNS(URN_XML_NS_NETCONF_BASE_1_0, "capabilities");
        capabilitiesElement.setPrefix(namespacePrefix);
        capabilities.forEach(capability -> {
            final Element capabilityElement =
                createdDocument.createElementNS(URN_XML_NS_NETCONF_BASE_1_0, "capability");
            capabilityElement.setTextContent(capability);
            capabilityElement.setPrefix(namespacePrefix);
            capabilitiesElement.appendChild(capabilityElement);
        });
        helloElement.appendChild(capabilitiesElement);

        if (sessionId != null) {
            final Element sessionIdElement
                = createdDocument.createElementNS(URN_XML_NS_NETCONF_BASE_1_0, "session-id");
            sessionIdElement.setPrefix(namespacePrefix);
            sessionIdElement.setTextContent(sessionId);
            helloElement.appendChild(sessionIdElement);
        }
        return createdDocument;
    }

}
