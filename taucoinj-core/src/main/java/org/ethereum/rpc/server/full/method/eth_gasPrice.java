package org.ethereum.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import org.ethereum.rpc.server.full.JsonRpcServerMethod;
import org.ethereum.facade.Ethereum;
import static org.ethereum.core.Denomination.SZABO;

public class eth_gasPrice extends JsonRpcServerMethod {

    public eth_gasPrice (Ethereum ethereum) {
        super(ethereum);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        String tmp = "0x" + Long.toHexString(10 * SZABO.longValue());
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}