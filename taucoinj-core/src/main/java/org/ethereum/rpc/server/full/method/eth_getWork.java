package org.ethereum.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import org.ethereum.rpc.server.full.JsonRpcServerMethod;
import org.ethereum.facade.Ethereum;

/*
TODO: must be changed in app that implement mining
*/
public class eth_getWork extends JsonRpcServerMethod {

    public eth_getWork (Ethereum ethereum) {
        super(ethereum);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        JSONRPC2Response res = new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
        return res;

    }
}