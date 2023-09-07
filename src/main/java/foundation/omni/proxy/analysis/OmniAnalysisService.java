package foundation.omni.proxy.analysis;

import foundation.omni.CurrencyID;
import foundation.omni.OmniValue;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import org.consensusj.bitcoin.proxy.jsonrpc.ExtraRpcRegistry;

import jakarta.inject.Singleton;
import static org.consensusj.bitcoin.proxy.jsonrpc.RpcParmParser.*;

/**
 * Service that adds "extra" RPCs, such as {@code omniproxy.help}, {@code omniproxy.getrichlist}
 */
@Singleton
@Context
@Requires(property="omniproxyd.enabled", value = "true")
public class OmniAnalysisService {
    private final ExtraRpcRegistry rpcRegistry;
    private final CachedRichListService<OmniValue, CurrencyID> richListService;
    private final OmniPropertyListService propertyListService;
    private final OmniBalanceService omniBalanceService;

    public OmniAnalysisService(ExtraRpcRegistry extraRpcRegistry,
                               CachedRichListService<OmniValue, CurrencyID> cachedRichListService,
                               OmniPropertyListService propertyListService,
                               OmniBalanceService omniBalanceService) {
        rpcRegistry = extraRpcRegistry;
        richListService = cachedRichListService;
        this.propertyListService = propertyListService;
        this.omniBalanceService = omniBalanceService;
        cachedRichListService.start();
        propertyListService.start();
        rpcRegistry.register("omniproxy.help",
                "",
                params -> params.size() >= 1 ? this.rpcRegistry.help(parmToString(params.get(0))) : this.rpcRegistry.help());
        rpcRegistry.register("omniproxy.getrichlist",
                "currency-id list-size(12)",
                params -> richListService.richList(toCurrencyId(params.get(0)), parmToInt(params.get(1))));
        rpcRegistry.register("omniproxy.listproperties",
                "",
                params -> propertyListService.getProperties());
        rpcRegistry.register("omniproxy.getproperty",
                "currency-id",
                params -> propertyListService.getProperty(toCurrencyId(params.get(0))));
        rpcRegistry.register("omniproxy.getbalance",
                "address",
                params -> omniBalanceService.getBalance(parmToAddress(params.get(0))));
        rpcRegistry.register("omniproxy.getbalances",
                "address1 address2 ...",
                params -> omniBalanceService.getBalances(parmsToAddressList(params)));
    }

    private static CurrencyID toCurrencyId(Object unknownIdType) {
        long id;
        if (unknownIdType instanceof String idString) {
            id = Long.parseLong(idString);
        } else if (unknownIdType instanceof Number idNum) {
            id = idNum.longValue();
        } else {
            throw new IllegalArgumentException("can't covert to CurrencyID");
        }
        return CurrencyID.of(id);
    }
}
