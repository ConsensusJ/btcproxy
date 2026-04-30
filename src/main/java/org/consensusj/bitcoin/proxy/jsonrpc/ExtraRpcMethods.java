package org.consensusj.bitcoin.proxy.jsonrpc;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.reactivex.rxjava3.core.Single;
import jakarta.inject.Singleton;

import static org.consensusj.bitcoin.proxy.jsonrpc.RpcParmParser.*;

/**
 * Add some extra btcproxy-specific methods to the server.
 */
@Singleton
@Context
public class ExtraRpcMethods {
    private final ExtraRpcRegistry rpcRegistry;

    private final String appName;

    public ExtraRpcMethods(ExtraRpcRegistry rpcRegistry, @Property(name = "micronaut.application.name") String appName) {
        this.rpcRegistry = rpcRegistry;
        this.appName = appName;
        rpcRegistry.register("btcproxy.help",
                "",
                params -> params.size() >= 1 ? this.rpcRegistry.help(parmToString(params.get(0))) : this.rpcRegistry.help());
        rpcRegistry.register("btcproxy.proxyinfo",
                "",
                params -> this.proxyInfo());
    }

    public Single<BtcProxyInfo> proxyInfo() {
        return Single.just(new BtcProxyInfo(appName, "unknown"));
    }

    @ReflectiveAccess
    public record BtcProxyInfo(String name, String version){}
}
