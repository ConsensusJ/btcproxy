package foundation.omni.proxy.analysis;

import foundation.omni.netapi.ConsensusService;
import foundation.omni.json.pojo.OmniJBalances;
import foundation.omni.json.pojo.WalletAddressBalance;
import io.micronaut.context.annotation.Requires;
import io.reactivex.rxjava3.core.Single;
import jakarta.inject.Singleton;
import org.bitcoinj.base.Address;

import java.util.List;

/**
 *
 */
@Singleton
@Requires(property="omniproxyd.enabled", value = "true")
public class OmniBalanceService {
    private final ConsensusService consensusService;

    public OmniBalanceService(ConsensusService consensusService) {
        this.consensusService = consensusService;
    }

    public Single<WalletAddressBalance> getBalance(Address address) {
        return Single.defer(() -> Single.fromCompletionStage(consensusService.balancesForAddressAsync(address)));
    }

    public Single<OmniJBalances> getBalances(List<Address> addresses) {
        return Single.defer(() -> Single.fromCompletionStage(consensusService.balancesForAddressesAsync(addresses)));
    }
}
