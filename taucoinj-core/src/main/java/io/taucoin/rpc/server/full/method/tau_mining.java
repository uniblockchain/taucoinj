package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;

/*
TODO: must be changed in app that implement mining
*/
public class tau_mining extends JsonRpcServerMethod {

    public tau_mining (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        Boolean tmp = false;
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}