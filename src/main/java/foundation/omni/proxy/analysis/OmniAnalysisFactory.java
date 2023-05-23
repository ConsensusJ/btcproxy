package foundation.omni.proxy.analysis;

import com.fasterxml.jackson.databind.Module;
import foundation.omni.CurrencyID;
import foundation.omni.OmniValue;
import foundation.omni.json.conversion.OmniServerModule;
import foundation.omni.netapi.ConsensusService;
import foundation.omni.netapi.analytics.OmniLayerRichListService;
import foundation.omni.netapi.omnicore.OmniCoreClient;
import foundation.omni.rpc.OmniClient;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.consensusj.bitcoin.proxy.jsonrpc.JsonRpcProxyConfiguration;

import jakarta.inject.Singleton;
import java.util.List;

/**
 * Factory that reads {@link JsonRpcProxyConfiguration} and creates necessary objects
 * for {@link OmniAnalysisController}.
 */
@Factory
@Requires(property="omniproxyd.enabled", value = "true")
public class OmniAnalysisFactory {
    
    @Singleton
    public Module jacksonModule() {
        return new OmniServerModule();
    }

    @Singleton
    public ConsensusService newOmniCoreClient(OmniClient omniClient) {
        return new OmniCoreClient(omniClient);
    }

    @Singleton
    @Context
    public CachedRichListService<OmniValue, CurrencyID> omniRichListService(OmniClient omniClient, ConsensusService client) {
        List<CurrencyID> richListEagerFetch  = eagerFetchList(omniClient.getNetParams());
        var uncached = new OmniLayerRichListService<OmniValue, CurrencyID>(client);
        var cached = new CachedRichListService<>(uncached, omniClient, richListEagerFetch);
        cached.start();
        return cached;
    }

    /**
     * Return the CurrencyIDs that should have their rich lists fetched on each block
     * @param params The network we are running on
     * @return List of CurrencyIDs to eager fetch
     */
    private static List<CurrencyID> eagerFetchList(NetworkParameters params) {
        return params.getId().equals(MainNetParams.ID_MAINNET)
                ? List.of(CurrencyID.OMNI, CurrencyID.USDT)  // On MainNet eagerly fetch OMNI & USDT
                : List.of(CurrencyID.OMNI);                  // On other nets, just prefetch OMNI
    }
}
