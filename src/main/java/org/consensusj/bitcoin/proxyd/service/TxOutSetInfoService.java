package org.consensusj.bitcoin.proxyd.service;

import com.msgilligan.bitcoinj.json.pojo.ChainTip;
import com.msgilligan.bitcoinj.json.pojo.TxOutSetInfo;
import com.msgilligan.bitcoinj.rpc.BitcoinClient;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOError;
import java.util.concurrent.CompletableFuture;

/**
 *
 */
@Singleton
public class TxOutSetInfoService extends Observable<TxOutSetInfo> {
    private static final Logger log = LoggerFactory.getLogger(TxOutSetInfoService.class);
    private final ProxyChainTipService proxyChainTipService;
    private final Subject<TxOutSetInfo> txOutSetInfoSubject = BehaviorSubject.create();
    private Disposable chainTipSubscription;
    private final BitcoinClient jsonRpc;

    public TxOutSetInfoService (BitcoinClient client, ProxyChainTipService proxyChainTipService) {
        jsonRpc = client;
        this.proxyChainTipService = proxyChainTipService;
    }

    public Single<TxOutSetInfo> latest() {
        start();
        return Single.fromObservable(txOutSetInfoSubject.take(1));
    }

    private synchronized void start() {
        if (chainTipSubscription == null) {
            log.info("subscribing to chainTipService");
            chainTipSubscription = proxyChainTipService
                    .doOnNext(tip -> log.info("got a new tip {}", tip))
                    .flatMapMaybe(tip -> this.currentTxOutSetInfoMaybe())
                    .doOnNext(outSetInfo -> log.info("got one {}", outSetInfo))
                    .subscribe(txOutSetInfoSubject::onNext, txOutSetInfoSubject::onError, txOutSetInfoSubject::onComplete);
        }
    }

    /**
     * Wrap a block chainTip in a Single.
     * @return A Single for the block height
     */
    private Single<TxOutSetInfo> currentTxOutSetInfo() {
        return Single.defer(() -> Single.fromCompletionStage(txOutSetInfo()));
    }

    /**
     * Retrieve the first ChainTip from the list returned by the
     * standard RPC `gettxoutinfo`
     *
     * @return The current chain tip
     */
    private CompletableFuture<TxOutSetInfo> txOutSetInfo() {
        log.info("Requesting TxOutSetInfo");
        return jsonRpc.supplyAsync(jsonRpc::getTxOutSetInfo).whenComplete((r, e) -> {
            if (r != null) {
                log.info("txoutsetinfo returned: {}", r);
            } else {
                log.error("Error: ", e);
            }
        });
    }

    /**
     * Get ChainTip, but swallowing IOExceptions
     *
     * @return A Maybe that is empty if an {@link IOError} occurred
     */
    private Maybe<TxOutSetInfo> currentTxOutSetInfoMaybe() {
        return currentTxOutSetInfo()
                .toMaybe()
                .doOnError(t -> log.error("Exception in currentTxOutSetInfoMaybe", t))
                .onErrorComplete(t -> t instanceof IOError);    // Empty completion if IOError
    }

    @Override
    protected void subscribeActual(@NonNull Observer<? super TxOutSetInfo> observer) {
        start();
        txOutSetInfoSubject.subscribe(observer);
    }
}
