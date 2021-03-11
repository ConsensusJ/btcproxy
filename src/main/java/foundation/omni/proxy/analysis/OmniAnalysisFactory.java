package foundation.omni.proxy.analysis;

import com.fasterxml.jackson.databind.Module;
import foundation.omni.CurrencyID;
import foundation.omni.OmniValue;
import foundation.omni.analytics.OmniLayerRichListService;
import foundation.omni.json.conversion.OmniServerModule;
import foundation.omni.netapi.ConsensusService;
import foundation.omni.netapi.omnicore.OmniCoreClient;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.reactivex.rxjava3.core.Observable;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.consensusj.bitcoin.proxy.core.RxBitcoinClient;
import org.consensusj.bitcoin.proxy.jsonrpc.JsonRpcProxyConfiguration;

import javax.inject.Singleton;
import java.util.List;

/**
 * Factory that reads {@link JsonRpcProxyConfiguration} and creates necessary objects
 * for {@link OmniAnalysisController}.
 */
@Factory
@Requires(property="omniproxyd.enabled", value = "true")
public class OmniAnalysisFactory {

    @Singleton
    public NetworkParameters networkParameters(JsonRpcProxyConfiguration config) {
        return config.getNetworkParameters();
    }

    @Singleton
    public Module jacksonModule() {
        return new OmniServerModule();
    }

    @Singleton
    public ConsensusService newOmniCoreClient(JsonRpcProxyConfiguration conf, NetworkParameters netParams) {
        return new OmniCoreClient(netParams, conf.getUri(), conf.getUsername(), conf.getPassword());
    }

    @Singleton
    @Context
    public CachedRichListService<OmniValue, CurrencyID> omniRichListService(NetworkParameters netParams, ConsensusService client, RxBitcoinClient rxBitcoinClient) {
        List<CurrencyID> richListEagerFetch  = netParams.getId().equals(MainNetParams.ID_MAINNET)
                ? List.of(CurrencyID.USDT) : List.of(CurrencyID.OMNI);
        var uncached = new OmniLayerRichListService<OmniValue, CurrencyID>(client, Observable.fromPublisher(rxBitcoinClient.chainTipService()));
        var cached = new CachedRichListService<>(uncached, rxBitcoinClient, richListEagerFetch);
        cached.start();
        return cached;
    }
}
