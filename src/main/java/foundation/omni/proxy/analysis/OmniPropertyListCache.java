package foundation.omni.proxy.analysis;

import foundation.omni.CurrencyID;
import foundation.omni.OmniDivisibleValue;
import foundation.omni.json.pojo.OmniPropertyInfo;
import foundation.omni.json.pojo.SmartPropertyListInfo;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.consensusj.bitcoin.json.pojo.TxOutSetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static foundation.omni.CurrencyID.BTC;
import static foundation.omni.CurrencyID.OMNI;
import static foundation.omni.CurrencyID.TOMNI;

/**
 * An in-memory cache of {@link OmniPropertyInfo}. Some entries may be "placeholders" generated
 * from {@link SmartPropertyListInfo} which contains a subset of {@code OmniPropertyInfo}.
 */
public class OmniPropertyListCache {
    private static final Logger log = LoggerFactory.getLogger(OmniPropertyListService.class);

    private final NetworkParameters netParams;
    private final ConcurrentHashMap<CurrencyID, OmniPropertyInfo> map = new ConcurrentHashMap<>(2000);

    public OmniPropertyListCache(NetworkParameters networkParameters) {
        netParams = networkParameters;
        // Add Bitcoin placeholder entry with estimated totalSupply
        map.put(BTC, OmniPropertyInfo.mockBitcoinPropertyInfo(netParams));
    }

    public OmniPropertyInfo get(CurrencyID id) {
        return map.get(id);
    }

    public Stream<OmniPropertyInfo> getAll() {
        return map.values().stream();
    }

    public List<CurrencyID> getPlaceholderIds() {
        return map.values().stream()
                .filter(this::isPlaceholder)
                .map(SmartPropertyListInfo::getPropertyid)
                .toList();
    }

    public void cachePut(OmniPropertyInfo info) {
        log.debug("Updating {}/{} TxID {} Amount {}", info.getPropertyid(), info.getName(), info.getCreationtxid(), info.getTotaltokens());
        map.put(info.getPropertyid(), info);
    }

    public void cachePutBitcoin(TxOutSetInfo outSetInfo) {
        log.info("Block height is {}, Bitcoin Supply is now: {}", outSetInfo.getHeight(), outSetInfo.getTotalAmount().toFriendlyString());
        cachePut(OmniPropertyInfo.bitcoinPropertyInfo(netParams, outSetInfo.getTotalAmount()));
    }

    public void cachePutIfNew(SmartPropertyListInfo splInfo) {
        map.computeIfAbsent(splInfo.getPropertyid(), id -> new OmniPropertyInfo(netParams, splInfo));
    }

    /**
     * Determine if an {@link OmniPropertyInfo} instance is "complete" or a "placeholder" entry
     * generated from {@link SmartPropertyListInfo}.
     * <p>
     * How this is determined depends upon the property:
     * <dl>
     *     <dt>BTC</dt><dd> Always {@code false} because not loaded via {@code omni_getproperty}</dd>
     *     <dt>OMNI</dt><dd>{@code true} if {@code totaltokens} is zero</dd>
     *     <dt>TOMNI</dt><dd>{@code true} if {@code totaltokens} is zero</dd>
     *     <dt>Other</dt><dd>{@code true} if {@code creationtxid} hash is zero</dd>
     * </dl>
     * Note: On a syncing MainNet, TOMNI totalTokens stays zero until the exodus block it seems?
     * @param info property info
     * @return {@code true} if info is loaded/"complete", {@code false} otherwise
     */
    private boolean isPlaceholder(OmniPropertyInfo info) {
        CurrencyID id = info.getPropertyid();
        if (id.equals(BTC)) {
            return false;
        } else if (id.equals(OMNI)) {
            return info.getTotaltokens().equals(OmniDivisibleValue.ZERO);
        } else if (id.equals(TOMNI)) {
            return info.getTotaltokens().equals(OmniDivisibleValue.ZERO);
        } else {
            return info.getCreationtxid().equals(Sha256Hash.ZERO_HASH);
        }
    }
}
