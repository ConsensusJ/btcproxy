package org.consensusj.bitcoin.proxy.jsonrpc;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.net.URI;
import java.util.List;

/**
 * Proxy Server Configuration Bean
 */
@ConfigurationProperties("btcproxyd.rpcproxy")
public interface JsonRpcProxyConfiguration {
    URI getUri();
    String getUsername();
    String getPassword();
    List<String> getAllowList();
}
