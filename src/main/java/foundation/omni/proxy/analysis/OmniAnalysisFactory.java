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
import org.bitcoinj.core.NetworkParameters;
import org.consensusj.analytics.service.RichListService;
import org.consensusj.bitcoin.proxy.core.RxBitcoinClient;
import org.consensusj.bitcoin.proxy.jsonrpc.JsonRpcProxyConfiguration;

import javax.inject.Singleton;
import java.util.List;

/**
 * Factory that reads {@link JsonRpcProxyConfiguration} and creates necessary objects
 * for {@link OmniAnalysisController}.
 */
@Factory
public class OmniAnalysisFactory {
    private final List<CurrencyID> richListEagerFetch = List.of(CurrencyID.USDT);

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
    public CachedRichListService<OmniValue, CurrencyID> omniRichListService(ConsensusService client, RxBitcoinClient rxBitcoinClient) {
        var uncached = new OmniLayerRichListService<OmniValue, CurrencyID>(client, rxBitcoinClient.chainTipService());
        var cached = new CachedRichListService<>(uncached, rxBitcoinClient, richListEagerFetch);
        cached.start();
        return cached;
    }
}
