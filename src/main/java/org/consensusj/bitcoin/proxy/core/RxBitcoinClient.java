package org.consensusj.bitcoin.proxy.core;

import com.msgilligan.bitcoinj.json.pojo.ChainTip;
import foundation.omni.rpc.OmniClient;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.operators.observable.ObservableInterval;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.processors.FlowableProcessor;
import org.consensusj.bitcoin.proxy.jsonrpc.JsonRpcProxyConfiguration;
import org.consensusj.bitcoin.zeromq.RxBitcoinJsonRpcClient;
import org.consensusj.bitcoin.zeromq.RxBitcoinZmqService;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * RxJava subclass of BitcoinClient, that internally polls for new blocks
 */
public class RxBitcoinClient extends OmniClient implements RxBitcoinJsonRpcClient {
    private static final Logger log = LoggerFactory.getLogger(RxBitcoinClient.class);
    private final Observable<Long> interval;
    private final Flowable<ChainTip> chainTipSource;
    private Disposable chainTipSubscription;
    // How will we properly use backpressure here?
    private final FlowableProcessor<ChainTip> chainTipProcessor = BehaviorProcessor.create();

    public RxBitcoinClient(JsonRpcProxyConfiguration config) {
        super(config.getNetworkParameters(), config.getUri(), config.getUsername(), config.getPassword());
        RxBitcoinZmqService zmqService;
        if (config.getUseZmq()) {
            log.info("Constructing ZMQ version: {}, {}", config.getNetworkParameters().getId(), config.getUri());
            zmqService = new RxBitcoinZmqService(this);
            interval = null;
            chainTipSource = zmqService.chainTipPublisher();
        } else {
            log.info("Constructing polling version: {}, {}", config.getNetworkParameters().getId(), config.getUri());
            interval = ObservableInterval.interval(2,10, TimeUnit.SECONDS);
            chainTipSource = pollForDistinctChainTip();
        }
    }

    @PostConstruct
    public synchronized void start() {
        if (chainTipSubscription == null) {
            chainTipSubscription = chainTipSource.subscribe(chainTipProcessor::onNext, chainTipProcessor::onError, chainTipProcessor::onComplete);
        }
    }

    /**
     * Return an observable for ChainTip. 
     *
     * @return An Observable for the sequence
     */
    @Override
    public Publisher<ChainTip> chainTipService() {
        return chainTipProcessor;
    }
    
    private Flowable<ChainTip> pollForDistinctChainTip() {
        return interval
                .doOnNext(t -> log.debug("got interval"))
                .flatMapMaybe(t -> this.currentChainTipMaybe())
                .doOnNext(tip -> log.debug("blockheight, blockhash = {}, {}", tip.getHeight(), tip.getHash()))
                .distinctUntilChanged(ChainTip::getHash)
                .doOnNext(tip -> log.info("** NEW ** blockheight, blockhash = {}, {}", tip.getHeight(), tip.getHash()))
                // ERROR backpressure strategy is compatible with BehaviorProcessor since it subscribes to MAX items
                .toFlowable(BackpressureStrategy.ERROR);
    }

    private Maybe<ChainTip> currentChainTipMaybe() {
        return pollOnce(this::getChainTips)
                .map(l -> l.get(0));
    }
}
