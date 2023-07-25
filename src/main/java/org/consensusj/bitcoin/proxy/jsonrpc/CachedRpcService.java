package org.consensusj.bitcoin.proxy.jsonrpc;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import org.consensusj.bitcoin.json.pojo.ChainTip;
import org.consensusj.bitcoin.rx.jsonrpc.RxBitcoinClient;
import org.consensusj.jsonrpc.JsonRpcRequest;
import org.consensusj.jsonrpc.JsonRpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  This prototype caching service implementation works best for RPC methods that:
 *  <ul>
 *      <li>take no parameters</li>
 *      <li>are updated with every new block</li>
 *      <li>have no data/fields that change between blocks</li>
 *  </ul>
 *
 *  TODO: Add eager caching for a Set of methods
 */
@Singleton
public class CachedRpcService {
    private static final Logger log = LoggerFactory.getLogger(CachedRpcService.class);
    private final Set<String> cached = Set.of("getchaintips", "getblockcount", "getblockchaininfo", "getbestblockhash", "gettxoutsetinfo");
    private final ConcurrentHashMap<String, Single<Object>> cache = new ConcurrentHashMap<>();
    private final RxBitcoinClient rxBitcoinClient;
    private Disposable chainTipSubscription;

    public CachedRpcService( RxBitcoinClient rxBitcoinClient) {
        this.rxBitcoinClient = rxBitcoinClient;
    }

    @PostConstruct
    public synchronized void start() {
        if (chainTipSubscription == null) {
            log.info("starting");
            chainTipSubscription = Flowable.fromPublisher(rxBitcoinClient.chainTipPublisher())
                    .subscribe(this::onNewBlock, this::onError);
        }
    }

    public boolean isCached(JsonRpcRequest request) {
        return cached.contains(request.getMethod());
    }

    public Single<JsonRpcResponse<?>> callCached(JsonRpcRequest request) {
        return fetch(request.getMethod())
            .map(result -> responseFromResult(request, result));
    }

    private void onError(Throwable t) {
        log.error("CachedRichListService onError: ", t);
    }

    /**
     * On a new block, invalidate the cache.
     *
     * @param tip the new ChainTip (currently unused)
     */
    private void onNewBlock(ChainTip tip) {
        log.info("new block -- clearing cache");
        cache.clear();
    }

    private Single<Object> fetch(String method) {
        return cache.computeIfAbsent(method, key -> {
            log.info("Fetching {}", method);
            return rxBitcoinClient.call(() -> rxBitcoinClient.send(method))
                    .doOnError(t -> log.error("Got an error from upstream", t))
                    .doOnSuccess(result -> log.info("got result for cache {}", result))
                    .cache();
        })
        .doOnError(t -> log.error("Error reading from RPC cache", t))
        .doOnSuccess(r -> log.debug("pulled from cache {}", r));
    }

    private static <RSLT> JsonRpcResponse<RSLT> responseFromResult(JsonRpcRequest request, RSLT result) {
        return new JsonRpcResponse<>(request, result);
    }
}
