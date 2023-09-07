package org.consensusj.bitcoin.proxyd

import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.consensusj.jsonrpc.JsonRpcStatusException
import org.consensusj.jsonrpc.groovy.DynamicRpcClient
import spock.lang.Shared
import spock.lang.Specification
import jakarta.inject.Inject

@MicronautTest
class ApplicationSpec extends Specification {

    @Inject
    EmbeddedServer server

    @Shared URI endpoint
    @Shared DynamicRpcClient client


    void setup() {
        endpoint = URI.create(server.URI.toString()+"/")
        client = new DynamicRpcClient(endpoint, "", "")
    }

    void 'server.running is true'() {
        expect:
        server.running
    }

    void 'method btcproxy.help is present'() {
        when:
        String result = client."btcproxy.help"()

        then:
        result
        result.length() > 0
    }

    void 'method btcproxy.proxyinfo returns correct info'() {
        when:
        var result = client."btcproxy.proxyinfo"()

        then:
        result
        result.name == server.applicationConfiguration.getName().orElseThrow()
        result.version == "unknown"
    }

    void 'unknown method returns error'() {
        when:
        client.unknownmethod()

        then:
        var e = thrown(JsonRpcStatusException)
        e.message == "Method not found"
        e.jsonRpcCode == -32601
        // TODO e.httpCode == ??
    }
}
