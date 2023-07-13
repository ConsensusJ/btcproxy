package org.consensusj.bitcoin.proxy.jsonrpc;

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;
import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.Network;

import java.net.URI;
import java.util.List;

/**
 * Proxy Server Configuration Bean
 */
@ConfigurationProperties("btcproxyd.rpcproxy")
public class JsonRpcProxyConfiguration {

    private final Network network;
    private final URI uri;
    private final String username;
    private final String password;
    private final boolean useZmq;
    private final List<String> allowList;

    /**
     * Injectable constructor
     * 
     * @param networkId A valid bitcoinj network id, see {@link org.bitcoinj.base.Network#id()}
     * @param uri URI to server including port number
     * @param username JSON-RPC username
     * @param password JSON-RPC password
     * @param allowList A list of allowed method names
     */
    @ConfigurationInject
    public JsonRpcProxyConfiguration(@Bindable(defaultValue = "org.bitcoin.production") String networkId,
                                     URI uri,
                                     String username,
                                     String password,
                                     boolean useZmq,
                                     List<String> allowList) {
        network = BitcoinNetwork.fromIdString(networkId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Bitcoin network-id string: " + networkId));
        this.uri = uri;
        this.username = username;
        this.password = password;
        this.useZmq = useZmq;
        this.allowList = allowList;
    }

    public Network network() {
        return network;
    }

    public URI getUri() {
        return uri;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean getUseZmq() {
        return useZmq;
    }

    public List<String> getAllowList() {
        return allowList;
    }
}
