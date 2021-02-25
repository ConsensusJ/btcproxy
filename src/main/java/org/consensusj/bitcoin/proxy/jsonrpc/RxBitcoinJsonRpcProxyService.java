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

import javax.inject.Singleton;
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
    private final TxOutSetInfoService txOutSetInfoService;
    private final URI remoteRpcUri;
    private final String remoteRpcUser;
    private final String remoteRpcPass;

    public RxBitcoinJsonRpcProxyService(HttpClient httpClient,
                                        TxOutSetInfoService txOutSetInfoService,
                                        ObjectMapper jsonMapper,
                                        JsonRpcProxyConfiguration config) {
        client = httpClient;
        mapper = jsonMapper;
        remoteRpcUri = config.getUri();
        remoteRpcUser = config.getUsername();
        remoteRpcPass = config.getPassword();
        optionalAllowList = Optional.of(config.getAllowList());
        this.txOutSetInfoService = txOutSetInfoService;
        log.info("btcproxyd.rpc.uri: {}", remoteRpcUri);
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
            if (isCached(request)) {
                // If a cached method, for now this is turned off
                return Flowable.fromSingle(callCached(request)
                        .map(mapper::writeValueAsString)
                        .map(string -> HttpResponse.ok().body(string))
                );
            } else {
                // Make a remote call and return unprocessed result
                return client.exchange( HttpRequest.POST(remoteRpcUri, request).basicAuth(remoteRpcUser, remoteRpcPass), String.class);
            }
        } else {
            return Flowable.just(makeErrorResponse(request));
        }
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
        // Currently only one method is cached
        //return request.getMethod().equals("gettxoutsetinfo");
        // Currently nothing is cached.
        return false;
    }

    private Single<JsonRpcResponse<?>> callCached(JsonRpcRequest request) {
        // Currently only  "gettxoutsetinfo" is cached
        return txOutSetInfoService.latest()
                .map(result -> new JsonRpcResponse<>(result, null, request.getJsonrpc(), request.getId()));
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
     *
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
        JsonRpcResponse<Void> jsonResponse = new JsonRpcResponse<>(null, notFoundError, request.getJsonrpc(), request.getId());
        String body;
        try {
            body = mapper.writeValueAsString(jsonResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return HttpResponse.ok().body(body);
    }

}