package foundation.omni.proxy.analysis;

import foundation.omni.CurrencyID;
import foundation.omni.OmniValue;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import org.consensusj.analytics.service.RichListService;
import org.consensusj.bitcoin.proxy.jsonrpc.ExtraRpcRegistry;

import javax.inject.Singleton;

/**
 *
 */
@Singleton
@Context
@Requires(property="omniproxyd.enabled", value = "true")
public class OmniAnalysisService {
    private final ExtraRpcRegistry rpcRegistry;
    private final CachedRichListService<OmniValue, CurrencyID> richListService;
    private final OmniPropertyListService propertyListService;

    public OmniAnalysisService(ExtraRpcRegistry extraRpcRegistry,
                               CachedRichListService<OmniValue, CurrencyID> cachedRichListService,
                                OmniPropertyListService propertyListService) {
        rpcRegistry = extraRpcRegistry;
        richListService = cachedRichListService;
        this.propertyListService = propertyListService;
        cachedRichListService.start();
        propertyListService.start();
        rpcRegistry.register("omniproxy.getrichlist", params -> richListService.richList(toCurrencyId(params.get(0)), (int) params.get(1)));
        rpcRegistry.register("omniproxy.listproperties", params -> propertyListService.getProperties());
        rpcRegistry.register("omniproxy.getproperty", params -> propertyListService.getProperty(toCurrencyId(params.get(0))));
    }

    private static CurrencyID toCurrencyId(Object unknownIdType) {
        long id;
        if (unknownIdType instanceof String) {
            String idString = (String) unknownIdType;
            id = Long.parseLong(idString);
        } else if (unknownIdType instanceof Number) {
            Number idNum = (Number) unknownIdType;
            id = idNum.longValue();
        } else {
            throw new IllegalArgumentException("can't covert to CurrencyID");
        }
        return CurrencyID.of(id);
    }
}
