package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;
import org.spongycastle.util.encoders.Hex;
import java.util.List;

public class tau_getBlockTransactionCountByHash extends JsonRpcServerMethod {

    public tau_getBlockTransactionCountByHash (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            byte[] hash = jsToAddress((String)params.get(0));
            String tmp = "0x" + Integer.toHexString(taucoin.getBlockchain().getBlockByHash(hash).getTransactionsList().size());
            JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
            return res;
        }

    }
}