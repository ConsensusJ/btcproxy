package org.consensusj.bitcoin.proxy.jsonrpc;

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.bind.annotation.Bindable;
import org.bitcoinj.core.NetworkParameters;

import java.net.URI;
import java.util.List;

/**
 * Proxy Server Configuration Bean
 */
@ConfigurationProperties("btcproxyd.rpcproxy")
public class JsonRpcProxyConfiguration {

    private final NetworkParameters networkParameters;
    private final URI uri;
    private final String username;
    private final String password;
    private final List<String> allowList;

    /**
     * Injectable constructor
     * 
     * @param networkId A valid bitcoinj network id, see {@link org.bitcoinj.core.NetworkParameters#getId()}
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
                                     List<String> allowList) {
        networkParameters = NetworkParameters.fromID(networkId);
        if (networkParameters == null) {
            throw new IllegalArgumentException("Invalid Bitcoin network-id string");
        }
        this.uri = uri;
        this.username = username;
        this.password = password;
        this.allowList = allowList;
    }

    public NetworkParameters getNetworkParameters() {
        return networkParameters;
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

    public List<String> getAllowList() {
        return allowList;
    }
}
