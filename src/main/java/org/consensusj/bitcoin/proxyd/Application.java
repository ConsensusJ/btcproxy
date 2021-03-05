package org.consensusj.bitcoin.proxyd;

import com.fasterxml.jackson.databind.Module;
import com.msgilligan.bitcoinj.json.conversion.RpcServerModule;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;
import org.bitcoinj.core.NetworkParameters;
import org.consensusj.bitcoin.proxy.core.RxBitcoinClient;
import org.consensusj.bitcoin.proxy.jsonrpc.JsonRpcProxyConfiguration;

import javax.inject.Singleton;

@Factory
public class Application {
    public static void main(String[] args) {
        Micronaut.build(args)
                .banner(false)
                .mainClass(Application.class)
                .start();
    }

    @Singleton
    public Module jacksonModule(NetworkParameters networkParameters) {
        return new RpcServerModule(networkParameters);
    }
    
    @Singleton
    public NetworkParameters networkParameters(JsonRpcProxyConfiguration config) {
        return NetworkParameters.fromID(config.getNetworkId());
    }

    @Singleton
    public RxBitcoinClient bitcoinClient(JsonRpcProxyConfiguration configuration, NetworkParameters networkParameters) {
        var client = new RxBitcoinClient(networkParameters, configuration.getUri(), configuration.getUsername(), configuration.getPassword());
        client.start();
        return client;
    }
}
