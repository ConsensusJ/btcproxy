package foundation.omni.proxy.analysis;

import foundation.omni.CurrencyID;
import foundation.omni.json.pojo.OmniPropertyInfo;
import foundation.omni.json.pojo.SmartPropertyListInfo;
import foundation.omni.rpc.OmniClient;
import io.micronaut.context.annotation.Requires;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import org.bitcoinj.base.BitcoinNetwork;
import org.consensusj.bitcoin.json.pojo.ChainTip;
import org.consensusj.bitcoin.rx.ChainTipPublisher;
import org.consensusj.bitcoin.rx.jsonrpc.service.TxOutSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static foundation.omni.CurrencyID.OMNI;
import static foundation.omni.CurrencyID.TOMNI;
import static foundation.omni.CurrencyID.USDT;

/**
 * Maintain a cache of {@link OmniPropertyInfo} for serving requests. The cache is loaded
 * using data received from {@link OmniClient}.
 */
@Singleton
@Requires(property="omniproxyd.enabled", value = "true")
public class OmniPropertyListService implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(OmniPropertyListService.class);

    private final OmniClient rxJsonClient;
    private final OmniPropertyListCache cache;
    private final List<CurrencyID> activeProperties;
    private final TxOutSetService txOutSetService;
    private final Flowable<Long> timerInterval;

    private Disposable chainTipSubscription;
    private Disposable intervalSubscription;
    private Disposable outSetSubscription;

    OmniPropertyListService(OmniClient omniClient, ChainTipPublisher chainTipPublisher) {
        rxJsonClient = omniClient;
        cache = new OmniPropertyListCache((BitcoinNetwork) rxJsonClient.getNetwork());
        activeProperties = rxJsonClient.getNetwork().equals(BitcoinNetwork.MAINNET) ? List.of(OMNI, TOMNI, USDT) : List.of(OMNI, TOMNI);
        txOutSetService = new TxOutSetService(omniClient, chainTipPublisher);
        timerInterval = Flowable.interval(3,1, TimeUnit.SECONDS);
    }

    /**
     * Subscribe to publishers
     */
    public synchronized void start() {
        // Subscribe to a new block (ChainTip) stream
        if (chainTipSubscription == null) {
            log.info("starting");
            chainTipSubscription = Flowable.fromPublisher(rxJsonClient.chainTipPublisher()).subscribe(this::onNewBlock, this::onError);
        }
        // Subscribe to a TxOutSetInfo stream (happens once per block, but delayed because the calculation takes some time)
        if (outSetSubscription == null) {
            outSetSubscription = Flowable.fromPublisher(txOutSetService.getTxOutSetPublisher())
                    .subscribe(cache::cachePutBitcoin,
                            t -> log.error("TxOutSetService", t),
                            () -> log.error("TxOutSetService completed"));
        }
        // Timer ticks used for resolving placeholders
        if (intervalSubscription == null) {
            intervalSubscription = timerInterval.subscribe(i -> loadPlaceholders(), this::onError);
        }
    }

    @Override
    public void close() {
        chainTipSubscription.dispose();
        intervalSubscription.dispose();
        outSetSubscription.dispose();
    }

    /**
     * Return the entire cache
     * @return entire contents of the cache
     */
    public Single<List<OmniPropertyInfo>> getProperties() {
        return Single.just(cache.getAll().toList());
    }

    /**
     * Return a single entry from the cache
     * @param id property to request
     * @return a single entry from the cache
     */
    public Single<OmniPropertyInfo> getProperty(CurrencyID id) {
        // Return a single entry from the cache
        return Single.just(cache.get(id));
    }

    /**
     * On a new block, update the cache by:
     * <ol>
     *      <li>initiate get-property requests for the Currency IDs on the "active" list</li>
     *      <li>Poll the entire smart property list to look for additions</li>
     * </ol>
     * TODO: Find changed properties by looking at transactions in the block
     *
     * @param tip the new ChainTip (currently unused)
     */
    private void onNewBlock(ChainTip tip) {
        log.info("New Block -- updating CurrencyIDs on the eager list and fetching any new properties created");
        activeProperties.forEach(this::getPropertyAsync);
        omniListPropertiesMaybe().subscribe(list -> list.forEach(cache::cachePutIfNew), this::onError);
    }


    private void onError(Throwable t){
        log.error("Fatal stream error", t);
    }

    /**
     * Load a batch of placeholder properties
     */
    private void loadPlaceholders() {
        final int maxFetchPerTimerTick = 10;
        // Find all properties needing loading
        List<CurrencyID> placeholders = cache.getPlaceholderIds();
        // Create a prioritized (ascending numeric order by ID) list to load on this tick
        List<CurrencyID> loadList = placeholders.stream()
                .sorted()
                .limit(maxFetchPerTimerTick)
                .toList();
        if (loadList.size() > 0) {
            log.info("Found {} placeholder properties, loading {}, starting with {}", placeholders.size(), loadList.size(), loadList.get(0));
        }
        loadList.forEach(this::getPropertyAsync);
    }

    /**
     * Try to get a property and if successful, put it in the cache
     * @param id property to fetch
     */
    private void getPropertyAsync(CurrencyID id) {
        log.debug("Fetching {}", id);
        var disposable = omniGetPropertyMaybe(id).subscribe(cache::cachePut, this::onError);
    }

    private Maybe<OmniPropertyInfo> omniGetPropertyMaybe(CurrencyID id) {
        return rxJsonClient.pollOnce(() -> rxJsonClient.omniGetProperty(id));
    }

    private Maybe<List<SmartPropertyListInfo>> omniListPropertiesMaybe() {
        return rxJsonClient.pollOnce(rxJsonClient::omniListProperties);
    }
}
