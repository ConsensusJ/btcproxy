package org.consensusj.bitcoin.proxyd.service;

import io.micronaut.http.HttpResponse;
import org.consensusj.jsonrpc.JsonRpcRequest;
import org.reactivestreams.Publisher;

/**
 * Interface for proxying JSON-RPC requests using Reactive Streams {@link Publisher}
 * and Micronaut {link @HttpResponse} types.
 */
public interface RxJsonRpcProxyService {
    /**
     *
     * @param request A deserialized (for filtering) request
     * @return A promise of a serialized response
     */
    Publisher<HttpResponse<String>> rpcProxy(JsonRpcRequest request);


    Publisher<HttpResponse<String>> rpcProxy(String method);

    Publisher<HttpResponse<String>> rpcProxy(String method, String... args);
}
