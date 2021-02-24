package org.consensusj.bitcoin.proxyd;

import com.fasterxml.jackson.databind.Module;
import com.msgilligan.bitcoinj.json.conversion.RpcServerModule;
import com.msgilligan.bitcoinj.rpc.BitcoinClient;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.consensusj.bitcoin.proxyd.service.JsonRpcProxyConfiguration;

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
    public NetworkParameters networkParameters() {
        return MainNetParams.get();
    }

    @Singleton
    public BitcoinClient bitcoinClient(JsonRpcProxyConfiguration configuration, NetworkParameters networkParameters) {
        return new BitcoinClient(networkParameters, configuration.getUri(), configuration.getUsername(), configuration.getPassword());
    }
}
