package foundation.omni.proxy.analysis;

import com.msgilligan.bitcoinj.json.pojo.ChainTip;
import foundation.omni.CurrencyID;
import foundation.omni.OmniValue;
import io.micronaut.context.annotation.Requires;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import org.consensusj.analytics.service.RichListService;
import org.consensusj.analytics.service.TokenRichList;
import org.consensusj.bitcoin.proxy.core.RxBitcoinClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caching wrapper for RichListService
 */
@Requires(property="omniproxyd.enabled", value = "true")
public class CachedRichListService<N extends Number & Comparable<? super N>, ID> implements RichListService<N, ID> {
    private static final Logger log = LoggerFactory.getLogger(CachedRichListService.class);
    private final RichListService<N, ID> uncachedService;
    private final RxBitcoinClient jsonRpc;
    private final List<ID> eager;
    private final ConcurrentHashMap<ID, Single<TokenRichList<N, ID>>> cache = new ConcurrentHashMap<>();
    private final int cacheSize = 12;
    private Disposable chainTipSubscription;

    public CachedRichListService(RichListService<N, ID> uncachedService, RxBitcoinClient rxBitcoinClient, List<ID> eager) {
        this.uncachedService = uncachedService;
        this.jsonRpc = rxBitcoinClient;
        this.eager = eager;
    }

    @PostConstruct
    public synchronized void start() {
        if (chainTipSubscription == null) {
            log.info("starting");
            chainTipSubscription = Flowable.fromPublisher(jsonRpc.chainTipService()).subscribe(this::onNewBlock, this::onError);
        }
    }

    @Override
    public Single<TokenRichList<N, ID>> richList(ID id, int i) {
        if (i > cacheSize) throw new IllegalArgumentException("too big");
        return fetch(id);
    }

    @Override
    public Observable<TokenRichList<N, ID>> richListUpdates(ID id, int i) {
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
        eager.forEach(id -> {
            this.fetch(id).subscribe((r) -> {} , this::onError);
        });
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
