package net.juniper.netconf.element;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.xmlunit.assertj.XmlAssert;

class RpcCloseSessionTest {

    // Example from https://datatracker.ietf.org/doc/html/rfc6241#section-7.8
    private static final String RPC_CLOSE_SESSION_WITHOUT_NAMESPACE = ""
        + "<rpc message-id=\"101\"\n"
        + "     xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
        + "  <close-session/>\n"
        + "</rpc>";

    private static final String RPC_CLOSE_SESSION_WITH_NAMESPACE = ""
        + "<nc:rpc message-id=\"102\"\n"
        + "     xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
        + "  <nc:close-session/>\n"
        + "</nc:rpc>";

    @Test
    public void willParseRpcCloseSessionWithoutNamespace() throws Exception {
        final RpcCloseSession rpcReply = RpcCloseSession.from(RPC_CLOSE_SESSION_WITHOUT_NAMESPACE);

        assertThat(rpcReply.getMessageId())
            .isEqualTo("101");
    }

    @Test
    public void willParseRpcCloseSessionWithNamespace() throws Exception {
        final RpcCloseSession rpcReply = RpcCloseSession.from(RPC_CLOSE_SESSION_WITH_NAMESPACE);

        assertThat(rpcReply.getMessageId())
            .isEqualTo("102");
    }

    @Test
    public void willCreateXmlFromAnObject() {

        final RpcCloseSession rpcReply = RpcCloseSession.builder()
            .messageId("101")
            .build();

        XmlAssert.assertThat(rpcReply.getXml())
            .and(RPC_CLOSE_SESSION_WITHOUT_NAMESPACE)
            .ignoreWhitespace()
            .areIdentical();
    }


}