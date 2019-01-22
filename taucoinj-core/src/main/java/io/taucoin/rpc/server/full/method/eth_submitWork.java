package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.rpc.server.full.filter.FilterManager;
import io.taucoin.facade.Taucoin;

import java.util.List;

/*
TODO: must be changed in app that implement mining
*/
public class eth_submitWork extends JsonRpcServerMethod {

    public eth_submitWork (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 3) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            byte[] nonce = jsToAddress((String) params.get(0));
            byte[] powHash = jsToAddress((String) params.get(1));
            byte[] mixDigest = jsToAddress((String) params.get(2));

            JSONRPC2Response res = new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
            return res;
        }

    }
}