package io.taucoin.rpc.server.light.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.light.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;
import io.taucoin.core.*;
import org.spongycastle.util.encoders.Hex;

public class eth_coinbase extends JsonRpcServerMethod {

    public eth_coinbase (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        String tmp = "0x" + Hex.toHexString(getCoinBase());
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}