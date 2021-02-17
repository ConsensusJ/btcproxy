package org.consensusj.bitcoin.proxyd;

import com.fasterxml.jackson.databind.Module;
import com.msgilligan.bitcoinj.json.conversion.RpcClientModule;
import com.msgilligan.bitcoinj.json.conversion.RpcServerModule;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.Micronaut;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;

import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;

@Factory
public class Application {

    public static void main(String[] args) {
        Micronaut.build(args)
                .banner(false)
                .mainClass(Application.class)
                .start();
    }

    @Value("${btcproxyd.rpc.uri}")
    protected String rpcUriString;

    @Value("${btcproxyd.rpc.username}")
    protected String rpcUsername;

    @Value("${btcproxyd.rpc.password}")
    protected String rpcPassword;

    @Singleton
    @Named("JSON_RPC_URI")
    public URI jsonRpcUri() {
        return URI.create(rpcUriString);
    }

    @Singleton
    @Named("JSON_RPC_USER")
    public String jsonRpcUser() {
        return rpcUsername;
    }

    @Singleton
    @Named("JSON_RPC_PASSWORD")
    public String jsonRpcPassword() {
        return rpcPassword;
    }

    @Singleton
    public NetworkParameters networkParameters() {
        return MainNetParams.get();
    }

    @Singleton
    public Module jacksonModule(NetworkParameters networkParameters) {
        return new RpcServerModule(null);
    }

    @Singleton
    public Module jacksonModuleClient(NetworkParameters networkParameters) {
        return new RpcClientModule(null);
    }
}
