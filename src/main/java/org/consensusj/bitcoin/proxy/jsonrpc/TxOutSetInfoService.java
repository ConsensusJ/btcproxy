package org.consensusj.bitcoin.proxy.jsonrpc;

import com.msgilligan.bitcoinj.json.pojo.TxOutSetInfo;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import org.consensusj.bitcoin.proxy.core.RxBitcoinClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

/**
 * This service (as currently written) will poll the upstream JSON-RPC every block
 * and ask for `txoutsetinfo` -- so it should not be used as is -- and is disabled
 * in RxBitcoinJsonRpcProxyService.  `gettxoutsetinfo` is probably not a good
 * service for a public API haha.
 * TODO: Adapt into a more generic caching service for any no-parameter RPC
 * and make configurable as to which RPC methods are used/allowed.
 */
//@Singleton
public class TxOutSetInfoService extends Observable<TxOutSetInfo> {
    private static final Logger log = LoggerFactory.getLogger(TxOutSetInfoService.class);
    private final Subject<TxOutSetInfo> txOutSetInfoSubject = BehaviorSubject.create();
    private Disposable subscription;
    private final RxBitcoinClient jsonRpc;

    public TxOutSetInfoService (RxBitcoinClient client) {
        jsonRpc = client;
    }

    public Single<TxOutSetInfo> latest() {
        return Single.fromObservable(txOutSetInfoSubject.take(1));
    }

    @PostConstruct
    private synchronized void start() {
        if (subscription == null) {
            log.info("subscribing to chainTipService");
            subscription = jsonRpc.pollOnNewBlock(jsonRpc::getTxOutSetInfo)
                    .subscribe(txOutSetInfoSubject::onNext, txOutSetInfoSubject::onError, txOutSetInfoSubject::onComplete);
        }
    }
    
    @Override
    protected void subscribeActual(@NonNull Observer<? super TxOutSetInfo> observer) {
        start();
        txOutSetInfoSubject.subscribe(observer);
    }
}
