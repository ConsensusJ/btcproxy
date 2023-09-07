package org.consensusj.bitcoin.proxy.jsonrpc;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import org.consensusj.jsonrpc.JsonRpcRequest;
import org.consensusj.jsonrpc.JsonRpcResponse;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Micronaut controller for proxying JSON-RPC requests to a remote JSON-RPC server.
 */
@Controller
public class JsonRpcProxyController {
    private static final Logger log = LoggerFactory.getLogger(JsonRpcProxyController.class);
    private final RxJsonRpcProxyService jsonRpcProxyService;

    /**
     *
     * @param jsonRpcProxyService proxy service
     */
    JsonRpcProxyController(RxJsonRpcProxyService jsonRpcProxyService) {
        this.jsonRpcProxyService = jsonRpcProxyService;
    }

    /**
     * Proxy a JSON-RPC request by forwarding to the remote JSON-RPC server if the
     * {@code method} is permitted. Permitted means on the allowed list (if the allow list is present)
     * and not on the deny list.
     * NOTE: We deserialize the {@link JsonRpcRequest} but for performance reasons we do not deserialize
     * the {@link JsonRpcResponse} objects and just treat them as type {@link String}.
     *
     * @param request The incoming JSON-RPC request
     * @return A Publisher that will resolve to the response from the remote server.
     */
    @Post(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public Publisher<HttpResponse<String>> rpcProxy(@Body JsonRpcRequest request) {
        return jsonRpcProxyService.rpcProxy(request);
    }

    @Get(uri="/get/{method}", produces = MediaType.APPLICATION_JSON)
    public Publisher<HttpResponse<String>> rpcGet(String method) {
        return jsonRpcProxyService.rpcProxy(method);
    }

    @Get(uri="/get/{method}/{args:.*}", produces = MediaType.APPLICATION_JSON)
    public Publisher<HttpResponse<String>> rpcGet(@PathVariable("method") String method, @PathVariable("args") String argString) {
        log.info("method: {}, args: {}", method, argString);
        String[] args = argString.split("/");
        return jsonRpcProxyService.rpcProxy(method, args);
    }
}
