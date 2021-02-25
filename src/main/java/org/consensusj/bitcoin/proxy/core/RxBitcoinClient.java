package org.consensusj.bitcoin.proxy.core;

import com.msgilligan.bitcoinj.json.pojo.ChainTip;
import com.msgilligan.bitcoinj.rpc.BitcoinClient;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.operators.observable.ObservableInterval;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import org.bitcoinj.core.NetworkParameters;
import org.consensusj.jsonrpc.AsyncSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.io.IOError;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * RxJava subclass of BitcoinClient, that internally polls for new blocks
 */
public class RxBitcoinClient extends BitcoinClient {
    private static final Logger log = LoggerFactory.getLogger(RxBitcoinClient.class);
    private final Observable<Long> interval;
    private Disposable intervalSubscription;
    // BehaviorProcessor will remember the last block received and pass it to new subscribers.
    private final Subject<ChainTip> chainTipSubject = BehaviorSubject.create();

    public RxBitcoinClient(NetworkParameters netParams, URI server, String rpcuser, String rpcpassword) {
        super(netParams, server, rpcuser, rpcpassword);
        this.interval = ObservableInterval.interval(2,10, TimeUnit.SECONDS);
    }

    @PostConstruct
    public synchronized void start() {
        if (intervalSubscription == null) {
            log.info("Starting...");
            intervalSubscription = pollForDistinctChainTip()
                    .subscribe(chainTipSubject::onNext, chainTipSubject::onError, chainTipSubject::onComplete);
        }
    }

    /**
     * This method will give you a stream of ChainTips
     *
     * @return An Observable for the sequence
     */
    public Observable<ChainTip> chainTipService() {
        return chainTipSubject;
    }
    
    /**
     * Poll a method, ignoring {@link IOError}.
     * The returned {@link Maybe} will:
     * <ol>
     *     <li>Emit a value if successful</li>
     *     <li>Empty Complete on IOError</li>
     *     <li>Error out if any other Exception occurs</li>
     * </ol>
     *
     * @param method A supplier (should be an RPC Method) that can throw {@link Exception}.
     * @param <RSLT> The type of the expected result
     * @return A Maybe for the expected result type
     */
    public <RSLT> Maybe<RSLT> pollOnce(AsyncSupport.ThrowingSupplier<RSLT> method) {
        return Single.defer(() -> Single.fromCompletionStage(this.supplyAsync(method)))
                .doOnSuccess(r -> log.debug("RPC call returned: {}", r))
                .doOnError(t -> log.error("Exception in RPCCall", t))
                .toMaybe()
                .onErrorComplete(t -> t instanceof IOError);    // Empty completion if IOError
    }

    /**
     * Poll a method, repeatedly once-per-new-block
     *
     * @param method A supplier (should be an RPC Method) that can throw {@link Exception}.
     * @param <RSLT> The type of the expected result
     * @return An Observable for the expected result type, so we can expect one call to {@code onNext} per block.
     */
    public <RSLT> Observable<RSLT> pollOnNewBlock(AsyncSupport.ThrowingSupplier<RSLT> method) {
        return chainTipSubject.flatMapMaybe(tip -> pollOnce(method));
    }

    private Observable<ChainTip> pollForDistinctChainTip() {
        return interval
                .doOnNext(t -> log.debug("got interval"))
                .flatMapMaybe(t -> this.currentChainTipMaybe())
                .doOnNext(tip -> log.debug("blockheight, blockhash = {}, {}", tip.getHeight(), tip.getHash()))
                .distinctUntilChanged(ChainTip::getHash)
                .doOnNext(tip -> log.info("** NEW ** blockheight, blockhash = {}, {}", tip.getHeight(), tip.getHash()));
    }

    private Maybe<ChainTip> currentChainTipMaybe() {
        return pollOnce(this::getChainTips)
                .map(l -> l.get(0));
    }
}
