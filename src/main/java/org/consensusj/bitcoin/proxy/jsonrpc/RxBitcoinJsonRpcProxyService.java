package org.consensusj.bitcoin.proxy.jsonrpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.consensusj.jsonrpc.JsonRpcError;
import org.consensusj.jsonrpc.JsonRpcRequest;
import org.consensusj.jsonrpc.JsonRpcResponse;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link RxJsonRpcProxyService} for Bitcoin.
 * Uses RxJava 3 internally.
 */
@Singleton
public class RxBitcoinJsonRpcProxyService implements RxJsonRpcProxyService {
    private static final Logger log = LoggerFactory.getLogger(RxBitcoinJsonRpcProxyService.class);
    // TODO: Create default allowList that includes all "safe" operations
    private final Optional<List<String>> optionalAllowList;
    // TODO: Create default denyList that includes all "dangerous" operations, this is currently a partial list
    private final List<String> denyList = List.of("stop", "logging", "backupwallet", "encryptwallet", "getwalletinfo", "dumpwallet", "rescanblockchain");
    private final Optional<List<String>> optionalDenyList = Optional.of(denyList);
    private final ObjectMapper mapper;
    private final JsonRpcError notFoundError = JsonRpcError.of(JsonRpcError.Error.METHOD_NOT_FOUND);

    private final HttpClient client;
    private final ExtraRpcRegistry extraRpcRegistry;
    private final CachedRpcService cachedRpcService;
    private final URI remoteRpcUri;
    private final String remoteRpcUser;
    private final String remoteRpcPass;

    public RxBitcoinJsonRpcProxyService(HttpClient httpClient,
                                        ExtraRpcRegistry extraRpcRegistry,
                                        CachedRpcService cachedRpcService,
                                        ObjectMapper jsonMapper,
                                        JsonRpcProxyConfiguration config) {
        client = httpClient;
        this.extraRpcRegistry = extraRpcRegistry;
        this.cachedRpcService = cachedRpcService;
        mapper = jsonMapper;
        remoteRpcUri = config.getUri();
        remoteRpcUser = config.getUsername();
        remoteRpcPass = config.getPassword();
        optionalAllowList = Optional.of(config.getAllowList());
        log.info("remoteRpcUri: {}", remoteRpcUri);
    }


    /**
     * Check permissions and if approved forward an RPC request to the remote server
     * returning the serialized response or an error response if not approved.
     *
     * @param request A deserialized JSON-RPC request
     * @return  A "promise" for the appropriate HttpResponse (JSON already serialized in a string)
     */
    @Override
    public Publisher<HttpResponse<String>> rpcProxy(JsonRpcRequest request) {
        if (methodPermitted(request)) {
            log.trace("method permitted: {}", request.getMethod());
            if (extraRpcRegistry.isExtraRpcMethod(request.getMethod())) {
                return renderResult(request, extraRpcRegistry.invoke(request));
            } else if (isCached(request)) {
                return renderResponse(callCached(request));
            } else {
                // Make a remote call and return unprocessed result
                log.info("Request {}", request.getMethod());
                return client.exchange( HttpRequest.POST(remoteRpcUri, request).basicAuth(remoteRpcUser, remoteRpcPass), String.class);
            }
        } else {
            log.error("method denied: {}", request.getMethod());
            return Flowable.just(makeErrorResponse(request));
        }
    }

    private Publisher<HttpResponse<String>> renderResult(JsonRpcRequest request, Single<?> result) {
        return renderResponse(result.map(r -> responseFromResult(request, r)));
    }

    private Publisher<HttpResponse<String>> renderResponse(Single<JsonRpcResponse<?>> response) {
        return Flowable.fromSingle(response
                .map(mapper::writeValueAsString)
                .map(string -> HttpResponse.ok().body(string)));
    }

    @Override
    public Publisher<HttpResponse<String>> rpcProxy(String method) {
        JsonRpcRequest request = new JsonRpcRequest(method);
        // TODO: Filter methods and only allow read-only methods here. (or will read-only be the same as "allowed"?)
        return rpcProxy(request);
    }

    @Override
    public Publisher<HttpResponse<String>> rpcProxy(String method, String... args) {
        List<Object> convertedArgs = convertParameters(method, List.of(args));
        JsonRpcRequest request = new JsonRpcRequest(method, convertedArgs);
        return rpcProxy(request);
    }

    private boolean isCached(JsonRpcRequest request) {
        return cachedRpcService.isCached(request);
    }

    private Single<JsonRpcResponse<?>> callCached(JsonRpcRequest request) {
        return cachedRpcService.callCached(request);
    }

    private static <RSLT> JsonRpcResponse<RSLT> responseFromResult(JsonRpcRequest request, RSLT result) {
        return new JsonRpcResponse<>(request, result);
    }

    /**
     * Check that the JSON-RPC method is permitted. This means it is on the allow list (if an
     * allow list is present) and not on the deny list.
     *
     * @param request The incoming request
     * @return true if permitted, false if denied
     */
    protected boolean methodPermitted(JsonRpcRequest request) {
        return methodAllowed(request.getMethod()) && methodNotDenied(request.getMethod());
    }

    private boolean methodAllowed(String method) {
        return optionalAllowList.map(l -> l.contains(method)).orElse(true);
    }

    private boolean methodNotDenied(String method) {
        return optionalDenyList.map(l -> !l.contains(method)).orElse(true);
    }

    /**
     * Convert params from strings to Java types that will map to correct JSON types
     * <p>
     * TODO: Make this better and complete
     *
     * @param method the JSON-RPC method
     * @param params Params with String type
     * @return Params with correct Java types for JSON
     */
    protected List<Object> convertParameters(String method, List<String> params) {
        List<Object> converted = new ArrayList<>();
        for (String param : params) {
            converted.add(convertParam(param));
        }
        return converted;
    }

    /**
     * Convert a single param from a command-line option {@code String} to a type more appropriate
     * for Jackson/JSON-RPC.
     *
     * @param param A string parameter to convert
     * @return The input parameter, possibly converted to a different type
     */
    private Object convertParam(String param) {
        Object result;
        Optional<Long> l = toLong(param);
        if (l.isPresent()) {
            // If the param was a valid Long, return a Long
            result = l.get();
        } else {
            // Else, return a Boolean or String
            switch (param) {
                case "false":
                    result = Boolean.FALSE;
                    break;
                case "true":
                    result = Boolean.TRUE;
                    break;
                default:
                    result = param;
            }
        }
        return result;
    }

    // Convert to Long (if possible)
    protected static Optional<Long> toLong(String strNum) {
        try {
            return Optional.of(Long.parseLong(strNum));
        } catch (NumberFormatException nfe) {
            return Optional.empty();
        }
    }


    private HttpResponse<String> makeErrorResponse(JsonRpcRequest request) {
        JsonRpcResponse<Void> jsonResponse = new JsonRpcResponse<>(request, notFoundError);
        String body;
        try {
            body = mapper.writeValueAsString(jsonResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return HttpResponse.ok().body(body);
    }

}
