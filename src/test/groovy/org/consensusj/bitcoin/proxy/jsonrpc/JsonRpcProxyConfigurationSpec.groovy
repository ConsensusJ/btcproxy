package org.consensusj.bitcoin.proxy.jsonrpc

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

/**
 *
 */
class JsonRpcProxyConfigurationSpec extends Specification {

    void "test default json-rpc proxy configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext)

        when:
        JsonRpcProxyConfiguration jsonRpcProxyConfiguration = ctx.getBean(JsonRpcProxyConfiguration)

        then:
        jsonRpcProxyConfiguration.networkId         == 'org.bitcoin.production'
        jsonRpcProxyConfiguration.uri.toString()    == 'http://localhost:8332'
        jsonRpcProxyConfiguration.username          == 'rpcusername'
        jsonRpcProxyConfiguration.password          == 'rpcpassword'
        jsonRpcProxyConfiguration.allowList[0]      == 'getblockcount'
        jsonRpcProxyConfiguration.allowList.size()  == 20

        cleanup:
        ctx.close()
    }

    void "test json-rpc proxy configuration"() {
        given:
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext, [
            'btcproxyd.rpcproxy.network-id': 'org.bitcoin.regtest',
            'btcproxyd.rpcproxy.uri': 'http://localhost:9999',
            'btcproxyd.rpcproxy.username': 'Satoshi',
            'btcproxyd.rpcproxy.password': 'Nakamoto',
            'btcproxyd.rpcproxy.allow-list': ['I', 'Shall', 'Not', 'Fear']
        ])

        when:
        JsonRpcProxyConfiguration jsonRpcProxyConfiguration = ctx.getBean(JsonRpcProxyConfiguration)

        then:
        jsonRpcProxyConfiguration.networkId       == 'org.bitcoin.regtest'
        jsonRpcProxyConfiguration.uri.toString()    == 'http://localhost:9999'
        jsonRpcProxyConfiguration.username          == 'Satoshi'
        jsonRpcProxyConfiguration.password          == 'Nakamoto'
        jsonRpcProxyConfiguration.allowList         == ['I', 'Shall', 'Not', 'Fear']

        cleanup:
        ctx.close()
    }
}
