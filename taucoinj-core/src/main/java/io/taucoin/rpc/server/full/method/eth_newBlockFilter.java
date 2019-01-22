package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.rpc.server.full.filter.FilterBlock;
import io.taucoin.rpc.server.full.filter.FilterManager;
import io.taucoin.facade.Taucoin;

public class eth_newBlockFilter extends JsonRpcServerMethod {

    public eth_newBlockFilter (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        int id = FilterManager.getInstance().addFilter(new FilterBlock());
        String tmp = "0x" + Integer.toHexString(id);
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}