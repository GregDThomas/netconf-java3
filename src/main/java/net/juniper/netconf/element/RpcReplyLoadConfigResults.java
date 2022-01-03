package net.juniper.netconf.element;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
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
import lombok.experimental.NonFinal;
import lombok.extern.log4j.Log4j2;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Class to represent a NETCONF load-configuration-results rpc-reply element -
 * https://www.juniper.net/documentation/us/en/software/junos/netconf/junos-xml-protocol/topics/ref/tag/junos-xml-protocol-load-configuration-results.html
 */
@Value
@Log4j2
@NonFinal
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "There is little alternative")
public class RpcReplyLoadConfigResults extends RpcReply {

    static final String XPATH_RPC_REPLY_LOAD_CONFIG_RESULT
        = RpcReply.XPATH_RPC_REPLY + "/*[local-name()='load-configuration-results']";
    private static final String XPATH_RPC_REPLY_LOAD_CONFIG_RESULT_OK
        = XPATH_RPC_REPLY_LOAD_CONFIG_RESULT + getXpathFor("ok");
    private static final String XPATH_RPC_REPLY_LOAD_CONFIG_RESULT_ERROR
        = XPATH_RPC_REPLY_LOAD_CONFIG_RESULT + getXpathFor("rpc-error");

    String action;

    /**
     * Generates an LoadConfigResults RpcReply object from XML.
     *
     * @param xml The XML representing the reply.
     * @return an RpcReplyLoadConfigResults object.
     * @throws ParserConfigurationException If the XML parser cannot be created
     * @throws IOException                  If the XML cannot be read
     * @throws SAXException                 If the XML cannot be parsed
     * @throws XPathExpressionException     If there is a problem in the parsing expressions
     */
    public static RpcReplyLoadConfigResults from(final String xml)
        throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        final Document document = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(new InputSource(new StringReader(xml)));
        final XPath xPath = XPathFactory.newInstance().newXPath();

        final Element rpcReplyElement = (Element) xPath.evaluate(
            XPATH_RPC_REPLY,
            document,
            XPathConstants.NODE
        );
        final Element loadConfigResultsElement = (Element) xPath.evaluate(
            RpcReplyLoadConfigResults.XPATH_RPC_REPLY_LOAD_CONFIG_RESULT,
            document,
            XPathConstants.NODE
        );
        final Element rpcReplyOkElement = (Element) xPath.evaluate(
            XPATH_RPC_REPLY_LOAD_CONFIG_RESULT_OK,
            document,
            XPathConstants.NODE
        );
        final List<RpcError> errorList = getRpcErrors(
            document,
            xPath,
            XPATH_RPC_REPLY_LOAD_CONFIG_RESULT_ERROR
        );

        return RpcReplyLoadConfigResults.loadConfigResultsBuilder()
            .messageId(getAttribute(rpcReplyElement, "message-id"))
            .action(getAttribute(loadConfigResultsElement, "action"))
            .ok(rpcReplyOkElement != null)
            .errors(errorList)
            .originalDocument(document)
            .build();
    }

    @Builder(builderMethodName = "loadConfigResultsBuilder")
    private RpcReplyLoadConfigResults(
        final Document originalDocument,
        final String namespacePrefix,
        final String messageId,
        final String action,
        final boolean ok,
        @Singular("error") final List<RpcError> errors
    ) {
        super(getDocument(originalDocument, namespacePrefix, messageId, action, ok, errors),
            namespacePrefix, messageId, ok, errors);
        this.action = action;
    }

    private static Document getDocument(
        final Document originalDocument,
        final String namespacePrefix,
        final String messageId,
        final String action,
        final boolean ok,
        final List<RpcError> errors
    ) {
        if (originalDocument != null) {
            return originalDocument;
        } else {
            return createDocument(namespacePrefix, messageId, action, ok, errors);
        }
    }

    private static Document createDocument(
        final String namespacePrefix,
        final String messageId,
        final String action,
        final boolean ok,
        final List<RpcError> errors
    ) {
        final Document createdDocument = createBlankDocument();
        final Element rpcReplyElement = createdDocument.createElementNS(
            URN_XML_NS_NETCONF_BASE_1_0,
            "rpc-reply"
        );
        rpcReplyElement.setPrefix(namespacePrefix);
        rpcReplyElement.setAttribute("message-id", messageId);
        createdDocument.appendChild(rpcReplyElement);
        final Element loadConfigResultsElement
            = createdDocument.createElement("load-configuration-results");
        loadConfigResultsElement.setAttribute("action", action);
        rpcReplyElement.appendChild(loadConfigResultsElement);
        appendErrors(namespacePrefix, errors, createdDocument, loadConfigResultsElement);
        if (ok) {
            final Element okElement = createdDocument.createElementNS(
                URN_XML_NS_NETCONF_BASE_1_0,
                "ok"
            );
            okElement.setPrefix(namespacePrefix);
            loadConfigResultsElement.appendChild(okElement);
        }

        return createdDocument;
    }
}