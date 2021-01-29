package org.consensusj.bitcoin.proxyd;

import com.fasterxml.jackson.databind.Module;
import com.msgilligan.bitcoinj.json.conversion.RpcClientModule;
import com.msgilligan.bitcoinj.json.conversion.RpcServerModule;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;

import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;

@Factory
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

    @Singleton
    @Named("JSON_RPC_URI")
    public URI jsonRpcUri() {
        return URI.create("http://127.0.0.1");
    }

    @Singleton
    @Named("JSON_RPC_USER")
    public String jsonRpcUser() {
        return "rpc-username";
    }

    @Singleton
    @Named("JSON_RPC_PASSWORD")
    public String jsonRpcPassword() {
        return "rpc-password";
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
