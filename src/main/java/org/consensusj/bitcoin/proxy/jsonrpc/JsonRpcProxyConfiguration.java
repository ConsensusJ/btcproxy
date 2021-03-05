package org.consensusj.bitcoin.proxy.jsonrpc;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;

import java.net.URI;
import java.util.List;

/**
 * Proxy Server Configuration Bean
 */
@ConfigurationProperties("btcproxyd.rpcproxy")
public interface JsonRpcProxyConfiguration {
    /**
     * A valid bitcoinj network id, see {@link org.bitcoinj.core.NetworkParameters#getId()}
     * TODO: Consider returning an actual NetworkParameters
     *
     * @return A valid bitcoinj network id string
     */
    @Bindable(defaultValue = "org.bitcoin.production")
    String          getNetworkId();
    
    URI             getUri();
    String          getUsername();
    String          getPassword();
    List<String>    getAllowList();
}
