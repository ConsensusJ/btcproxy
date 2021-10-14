package org.consensusj.bitcoin.proxy.jsonrpc;

import io.reactivex.rxjava3.core.Single;
import org.consensusj.jsonrpc.JsonRpcRequest;

import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A place to register extra RPC methods that are (typically) implemented
 * on the proxy server itself.
 */
@Singleton
public class ExtraRpcRegistry {
    private final ConcurrentHashMap<String, RPCMethodInvoke> extraMethods = new ConcurrentHashMap<>();

    public ExtraRpcRegistry() {
    }

    public void register(String methodName, RPCMethodInvoke method) {
        extraMethods.put(methodName, method);
    }

    public boolean isExtraRpcMethod(String methodName) {
        return extraMethods.containsKey(methodName);
    }

    public Single<?> invoke(JsonRpcRequest request) {
        RPCMethodInvoke handler = extraMethods.get(request.getMethod());
        return handler.call(request.getParams());
    }

    @FunctionalInterface
    public interface RPCMethodInvoke {
        Single<?> call(List<Object> params);

        default Single<?> call(Object... params) {
            return this.call(Arrays.asList(params));
        }
    }
}
