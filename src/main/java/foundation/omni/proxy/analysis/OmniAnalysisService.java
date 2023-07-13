package foundation.omni.proxy.analysis;

import foundation.omni.CurrencyID;
import foundation.omni.OmniValue;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import org.bitcoinj.base.Address;
import org.bitcoinj.base.AddressParser;
import org.bitcoinj.base.DefaultAddressParser;
import org.consensusj.bitcoin.proxy.jsonrpc.ExtraRpcRegistry;

import jakarta.inject.Singleton;

import java.util.List;

/**
 * Service that combines Omni rich list and property list and makes available as "extra" RPCs
 */
@Singleton
@Context
@Requires(property="omniproxyd.enabled", value = "true")
public class OmniAnalysisService {
    private static final AddressParser addressParser = new DefaultAddressParser();
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

    private static int parmToInt(Object param) {
        int result;
        if (param instanceof Number) {
            result = ((Number) param).intValue();
        } else if (param instanceof String) {
            try {
                result = Integer.parseInt((String) param);
            } catch (NumberFormatException e) {
                throw e;
            }
        } else {
            throw new IllegalArgumentException("can't covert to integer");
        }
        return result;
    }

    private static String parmToString(Object param) {
        if (param instanceof String) {
            return (String) param;
        } else {
            throw new IllegalArgumentException("can't covert to integer");
        }
    }

    private static Address parmToAddress(Object param) {
        if (param instanceof String) {
            return addressParser.parseAddressAnyNetwork((String) param);
        } else {
            throw new IllegalArgumentException("can't covert to address");
        }
    }

    private static List<Address> parmsToAddressList(List<Object> params) {
        return params.stream().map(OmniAnalysisService::parmToAddress).toList();
    }

}
