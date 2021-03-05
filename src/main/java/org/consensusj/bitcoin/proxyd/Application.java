package org.consensusj.bitcoin.proxyd;

import com.fasterxml.jackson.databind.Module;
import com.msgilligan.bitcoinj.json.conversion.RpcServerModule;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;
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
    public Module jacksonModule(JsonRpcProxyConfiguration config) {
        return new RpcServerModule(config.getNetworkParameters());
    }
    
    @Singleton
    public RxBitcoinClient bitcoinClient(JsonRpcProxyConfiguration configuration) {
        var client = new RxBitcoinClient(configuration);
        client.start();
        return client;
    }
}
