package org.consensusj.bitcoin.proxy.jsonrpc;

import org.bitcoinj.base.Address;
import org.bitcoinj.base.AddressParser;
import org.bitcoinj.base.DefaultAddressParser;

import java.util.List;

/**
 * Primitive (too primitive) JSON-RPC parameter parser.
 */
public class RpcParmParser {
    private static final AddressParser addressParser = new DefaultAddressParser();

    public static int parmToInt(Object param) {
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

    public static String parmToString(Object param) {
        if (param instanceof String) {
            return (String) param;
        } else {
            throw new IllegalArgumentException("can't covert to integer");
        }
    }

    public static Address parmToAddress(Object param) {
        if (param instanceof String) {
            return addressParser.parseAddressAnyNetwork((String) param);
        } else {
            throw new IllegalArgumentException("can't covert to address");
        }
    }

    public static List<Address> parmsToAddressList(List<Object> params) {
        return params.stream().map(RpcParmParser::parmToAddress).toList();
    }
}