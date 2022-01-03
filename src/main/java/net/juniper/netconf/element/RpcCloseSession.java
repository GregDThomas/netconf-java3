package net.juniper.netconf.element;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Class to represent a NETCONF RPC close-session element - https://datatracker.ietf.org/doc/html/rfc6241#section-7.8
 */
@Value
@Log4j2
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "There is little alternative")
public class RpcCloseSession extends AbstractNetconfElement {

    private static final String XPATH_RPC
        = getXpathFor("rpc");
    private static final String XPATH_RPC_MESSAGE_ID
        = XPATH_RPC + getXpathFor("message-id");

    String messageId;

    /**
     * Creates an RPC close-session object based on the supplied XML.
     *
     * @param xml The XML of the NETCONF &lt;hello&gt;
     * @return a new, immutable, Hello object.
     * @throws ParserConfigurationException If the XML parser cannot be created
     * @throws IOException                  If the XML cannot be read
     * @throws SAXException                 If the XML cannot be parsed
     * @throws XPathExpressionException     If there is a problem in the parsing expressions
     */
    public static RpcCloseSession from(final String xml)
        throws ParserConfigurationException,
        IOException,
        SAXException,
        XPathExpressionException {

        final Document document = createDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final Element rpcElement = (Element) xPath.evaluate(
            XPATH_RPC,
            document,
            XPathConstants.NODE
        );
        final RpcCloseSession rpcCloseSession = RpcCloseSession.builder()
            .messageId(rpcElement.getAttribute("message-id"))
            .build();
        log.trace("rpcCloseSession is: {}", rpcCloseSession::getXml);
        return rpcCloseSession;
    }

    @Builder
    private RpcCloseSession(
        final Document originalDocument,
        final String namespacePrefix,
        final String messageId
    ) {
        super(getDocument(originalDocument, namespacePrefix, messageId));
        this.messageId = messageId;
    }

    private static Document getDocument(
        final Document originalDocument,
        final String namespacePrefix,
        final String messageId
    ) {
        if (originalDocument != null) {
            return originalDocument;
        } else {
            return createDocument(namespacePrefix, messageId);
        }
    }

    private static Document createDocument(
        final String namespacePrefix,
        final String messageId
    ) {

        final Document createdDocument = createBlankDocument();

        final Element rpcElement
            = createdDocument.createElementNS(URN_XML_NS_NETCONF_BASE_1_0, "rpc");
        rpcElement.setPrefix(namespacePrefix);
        rpcElement.setAttribute("message-id", messageId);
        createdDocument.appendChild(rpcElement);
        final Element closeSessionElement
            = createdDocument.createElementNS(URN_XML_NS_NETCONF_BASE_1_0, "close-session");
        rpcElement.appendChild(closeSessionElement);

        return createdDocument;
    }

}
