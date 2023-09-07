package foundation.omni.proxy.analysis;

import foundation.omni.rpc.OmniClient;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import org.consensusj.analytics.service.RichListService;
import org.consensusj.analytics.service.TokenRichList;
import org.consensusj.bitcoin.json.pojo.ChainTip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caching wrapper for RichListService.
 * This should not be a Singleton, (for now) it must be constructed in {@link OmniAnalysisFactory}
 */
public class CachedRichListService<N extends Number & Comparable<? super N>, ID> implements RichListService<N, ID> {
    private static final Logger log = LoggerFactory.getLogger(CachedRichListService.class);
    private final RichListService<N, ID> uncachedService;
    private final OmniClient jsonRpc;
    private final List<ID> eager;
    private final ConcurrentHashMap<ID, Single<TokenRichList<N, ID>>> cache = new ConcurrentHashMap<>();
    private final int cacheSize = 12;
    private Disposable chainTipSubscription;

    public CachedRichListService(RichListService<N, ID> uncachedService, OmniClient omniClient, List<ID> eager) {
        this.uncachedService = uncachedService;
        this.jsonRpc = omniClient;
        this.eager = eager;
    }

    public synchronized void start() {
        if (chainTipSubscription == null) {
            log.info("starting");
            chainTipSubscription = Flowable.fromPublisher(jsonRpc.chainTipPublisher()).subscribe(this::onNewBlock, this::onError);
        }
    }

    @Override
    public Single<TokenRichList<N, ID>> richList(ID id, int i) {
        if (i > cacheSize) throw new IllegalArgumentException("too big");
        return fetch(id);
    }

    @Override
    public Flowable<TokenRichList<N, ID>> richListUpdates(ID id, int i) {
        throw new UnsupportedOperationException("this service is incubating and this method isn't available yet.");
    }

    private void onError(Throwable t) {
        log.error("CachedRichListService onError: ", t);
    }

    /**
     * On a new block, invalidate the cache and initiate fetch requests for the Currency IDs on
     * the "eager" list.
     * 
     * @param tip the new ChainTip (currently unused)
     */
    private void onNewBlock(ChainTip tip) {
        log.info("New Block: clearing cache & starting eager fetch");
        cache.clear();
        eager.forEach(id ->
            this.fetch(id).subscribe((r) -> {} , this::onError)
        );
    }

    private Single<TokenRichList<N, ID>> fetch(ID id) {
        return cache.computeIfAbsent(id, key -> {
            log.info("Fetching {}", id);
            return uncachedService.richList(id, cacheSize)
                    .doOnError(t -> log.error("Got an error from upstream", t))
                    .doOnSuccess(r -> log.info("got rich list from upstream {}", r.getCurrencyID()))
                    .cache();
        })
        .doOnError(t -> log.error("Error reading from richList cache", t))
        .doOnSuccess(r -> log.info("pulled from cache {}", r.getCurrencyID()));
    }

}
