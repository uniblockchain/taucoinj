package org.ethereum.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import org.ethereum.rpc.server.full.JsonRpcServerMethod;
import org.ethereum.rpc.server.full.filter.FilterBlock;
import org.ethereum.rpc.server.full.filter.FilterManager;
import org.ethereum.rpc.server.full.filter.FilterTransaction;
import org.ethereum.facade.Ethereum;

public class eth_newPendingTransactionFilter extends JsonRpcServerMethod {

    public eth_newPendingTransactionFilter (Ethereum ethereum) {
        super(ethereum);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        int id = FilterManager.getInstance().addFilter(new FilterTransaction());
        String tmp = "0x" + Integer.toHexString(id);
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}