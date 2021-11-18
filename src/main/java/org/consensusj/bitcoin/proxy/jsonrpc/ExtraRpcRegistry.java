package org.consensusj.bitcoin.proxy.jsonrpc;

import io.reactivex.rxjava3.core.Single;
import org.consensusj.jsonrpc.JsonRpcError;
import org.consensusj.jsonrpc.JsonRpcErrorException;
import org.consensusj.jsonrpc.JsonRpcRequest;

import jakarta.inject.Singleton;
import org.consensusj.jsonrpc.JsonRpcStatusException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A place to register extra RPC methods that are (typically) implemented
 * on the proxy server itself.
 */
@Singleton
public class ExtraRpcRegistry {
    Map<String, String> helpInfo = new HashMap<>();
    private final ConcurrentHashMap<String, RPCMethodInvoke> extraMethods = new ConcurrentHashMap<>();

    public ExtraRpcRegistry() {
    }

    public void register(String methodName, String help, RPCMethodInvoke method) {
        extraMethods.put(methodName, method);
        helpInfo.put(methodName, help);
    }

    public Single<String> help() {
        String allMethodHelp = helpInfo.entrySet().stream()
                .map(this::formatMethodHelp)
                .sorted()
                .collect(Collectors.joining("\n"));
        return Single.just(allMethodHelp);
    }

    public Single<String> help(String method) {
        if (isExtraRpcMethod(method)) {
            String oneMethodHelp = formatMethodHelp(method, helpInfo.get(method));
            return Single.just(oneMethodHelp);
        } else {
            return Single.error(JsonRpcErrorException.of(JsonRpcError.Error.METHOD_NOT_FOUND));
        }
    }

    public String formatMethodHelp(Map.Entry<String, String> entry) {
        return formatMethodHelp(entry.getKey(), entry.getValue());
    }

    public String formatMethodHelp(String method, String methodParamHelp) {
        return method + " " + methodParamHelp;
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
