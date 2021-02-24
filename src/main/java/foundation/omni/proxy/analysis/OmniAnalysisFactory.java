package foundation.omni.proxy.analysis;

import com.fasterxml.jackson.databind.Module;
import foundation.omni.CurrencyID;
import foundation.omni.OmniValue;
import foundation.omni.analytics.OmniLayerRichListService;
import foundation.omni.json.conversion.OmniServerModule;
import foundation.omni.netapi.ConsensusService;
import foundation.omni.netapi.omnicore.OmniCoreClient;
import io.micronaut.context.annotation.Factory;
import org.bitcoinj.core.NetworkParameters;
import org.consensusj.analytics.service.RichListService;
import org.consensusj.bitcoin.proxy.jsonrpc.JsonRpcProxyConfiguration;
import org.consensusj.bitcoin.proxy.core.ProxyChainTipService;

import javax.inject.Singleton;

/**
 * Factory that reads {@link JsonRpcProxyConfiguration} and creates necessary objects
 * for {@link OmniAnalysisController}.
 */
@Factory
public class OmniAnalysisFactory {
    @Singleton
    public Module jacksonModule() {
        return new OmniServerModule();
    }

    @Singleton
    public ConsensusService newOmniCoreClient(JsonRpcProxyConfiguration conf, NetworkParameters netParams) {
        return new OmniCoreClient(netParams, conf.getUri(), conf.getUsername(), conf.getPassword());
    }

    @Singleton
    public RichListService<OmniValue, CurrencyID> omniRichListService(ConsensusService client, ProxyChainTipService chainTipService) {
        return new OmniLayerRichListService<>(client, chainTipService);
    }
}
