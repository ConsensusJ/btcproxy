package foundation.omni.proxy.analysis;

import com.msgilligan.bitcoinj.json.pojo.ChainTip;
import foundation.omni.CurrencyID;
import foundation.omni.OmniDivisibleValue;
import foundation.omni.json.pojo.OmniPropertyInfo;
import foundation.omni.rpc.SmartPropertyListInfo;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.operators.observable.ObservableInterval;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.params.MainNetParams;
import org.consensusj.bitcoin.proxy.core.RxBitcoinClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 */
@Singleton
public class OmniPropertyListService {
    private static final Logger log = LoggerFactory.getLogger(OmniPropertyListService.class);
    private final List<CurrencyID> activeProperties;
    RxBitcoinClient rxOmniClient;
    private List<SmartPropertyListInfo> cachedPropertyList = new ArrayList<>();
    private Disposable chainTipSubscription;
    private Disposable intervalSubscription;

    private ConcurrentHashMap<CurrencyID, OmniPropertyInfo> cachedPropertyInfo = new ConcurrentHashMap<>(2000);

    private final Observable<Long> loadPollingInterval;

    OmniPropertyListService(NetworkParameters netParams, RxBitcoinClient rxBitcoinClient) {
        // RxBitcoinClient is currently an Omni client too!
        rxOmniClient = rxBitcoinClient;
        loadPollingInterval = ObservableInterval.interval(5,5, TimeUnit.SECONDS);
        if (netParams.getId().equals(MainNetParams.ID_MAINNET)) {
            activeProperties = List.of(CurrencyID.OMNI, CurrencyID.USDT);
        } else {
            activeProperties = List.of(CurrencyID.OMNI, CurrencyID.TOMNI);
        }
    }

    public synchronized void start() {
        if (chainTipSubscription == null) {
            log.info("starting");
            chainTipSubscription = rxOmniClient.chainTipService().subscribe(this::onNewBlock, this::onError);
        }
        if (intervalSubscription == null) {
            intervalSubscription = loadPollingInterval.subscribe(i -> loadProperties(), this::onError);
        }
    }

    public Single<List<OmniPropertyInfo>> getProperties() {
        // Return everything in the cache
        List<OmniPropertyInfo> resultList = cachedPropertyInfo.values().stream().collect(Collectors.toUnmodifiableList());
        return Single.just(resultList);
    }

    public Single<OmniPropertyInfo> getProperty(CurrencyID id) {
        // Return a single entry from the cache
        return Single.just(cachedPropertyInfo.get(id));
    }

    /**
     * On a new block, update the cache and initiate fetch requests for the Currency IDs on
     * the "eager" list.
     *
     * @param tip the new ChainTip (currently unused)
     */
    private void onNewBlock(ChainTip tip) {
        log.info("New Block: {}/{}", tip.getHeight(), tip.getHash());
        updateActiveProperties();
        // TODO: Find changed properties in the block
        handleNewProperties();
        //loadProperties();
    }

    private void updatePropertyAsync(CurrencyID id) {
        log.debug("Fetching {}", id);
        var disposable = rxOmniClient.pollOnce(() -> rxOmniClient.omniGetProperty(id))
                .subscribe((info) -> update(id, info), this::onError);
    }

    private void update(CurrencyID id, OmniPropertyInfo info) {
        log.debug("Updating {}/{} TxID {} Amount {}", info.getPropertyid(), info.getName(), info.getCreationtxid(), info.getTotaltokens());
        cachedPropertyInfo.put(id, info);
    }

    private void updateActiveProperties() {
        activeProperties.forEach(this::updatePropertyAsync);
    }

    private void handleNewProperties() {
        final int perBlockNewFetch = 5;
        rxOmniClient.pollOnce(() -> rxOmniClient.omniListProperties())
                .subscribe(this::onListProperties, this::onError);
    }

    private void onListProperties(List<SmartPropertyListInfo> list) {
        final int perBlockNewFetch = 5;
        List<CurrencyID> newProperties = new ArrayList<>();
        list.forEach(info -> {
            if (!cachedPropertyInfo.containsKey(info.getPropertyid())) {
                newProperties.add(info.getPropertyid());
                cachedPropertyInfo.put(info.getPropertyid(), new OmniPropertyInfo(info));
            }
        });
        // Fetch up to perBlockNewFetch new properties per block
        int subsize = Math.min(perBlockNewFetch, newProperties.size());
        List<CurrencyID> newUpdateList = newProperties.subList(0, subsize);
        newUpdateList.forEach(this::updatePropertyAsync);
    }

    private void onError(Throwable t){
        log.error("meh", t);
    }

    private void loadProperties() {
        final int maxPerTickLoadFetch = 10;
        List<CurrencyID> unloadedProperties = new ArrayList<>();
        cachedPropertyInfo.forEach((id, info) -> {
            if (!isLoaded(info)) {
                unloadedProperties.add(id);
            }
        });
        unloadedProperties.sort(CurrencyID::compareTo);
        int subsize = Math.min(maxPerTickLoadFetch, unloadedProperties.size());
        List<CurrencyID> loadList = unloadedProperties.subList(0, subsize);
        log.info("Found {} unloaded properties, loading {} of them asynchronously", unloadedProperties.size(), loadList.size());
        loadList.forEach(this::updatePropertyAsync);
    }

    public static boolean isLoaded(OmniPropertyInfo info) {
        // All tokens besides OMNI and TOMNI must have a creationTxId
        if (info.getPropertyid().equals(CurrencyID.OMNI)) {
            return !info.getTotaltokens().equals(OmniDivisibleValue.ZERO);
        } else if (info.getPropertyid().equals(CurrencyID.TOMNI)) {
            return !info.getTotaltokens().equals(OmniDivisibleValue.ZERO);
        } else {
            return !info.getCreationtxid().equals(Sha256Hash.ZERO_HASH);
        }
    }
}
