package org.consensusj.bitcoin.proxy.jsonrpc;

import org.bitcoinj.base.Address;
import org.bitcoinj.base.AddressParser;

import java.util.List;

/**
 * Primitive (too primitive) JSON-RPC parameter parser.
 */
public class RpcParmParser {
    private static final AddressParser addressParser = AddressParser.getDefault();

    public static int parmToInt(Object param) {
        int result;
        if (param instanceof Number num) {
            result = num.intValue();
        } else if (param instanceof String string) {
            try {
                result = Integer.parseInt(string);
            } catch (NumberFormatException e) {
                throw e;
            }
        } else {
            throw new IllegalArgumentException("can't covert to integer");
        }
        return result;
    }

    public static String parmToString(Object param) {
        if (param instanceof String string) {
            return string;
        } else {
            throw new IllegalArgumentException("can't covert to integer");
        }
    }

    public static Address parmToAddress(Object param) {
        if (param instanceof String string) {
            return addressParser.parseAddress(string);
        } else {
            throw new IllegalArgumentException("can't covert to address");
        }
    }

    public static List<Address> parmsToAddressList(List<Object> params) {
        return params.stream().map(RpcParmParser::parmToAddress).toList();
    }
}
