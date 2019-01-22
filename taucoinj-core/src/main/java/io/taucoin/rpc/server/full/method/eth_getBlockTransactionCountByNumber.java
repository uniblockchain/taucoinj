package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.core.Blockchain;
import io.taucoin.core.BlockchainImpl;
import io.taucoin.facade.Taucoin;
import java.util.List;

public class eth_getBlockTransactionCountByNumber extends JsonRpcServerMethod {

    public eth_getBlockTransactionCountByNumber (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            String height = (String)params.get(0);

            long blockNumber = getBlockNumber(height);
            if (blockNumber == -1)
                blockNumber = taucoin.getBlockchain().getBestBlock().getNumber();

            int count = 0;
            if (blockNumber == -2) {
                count = ((BlockchainImpl)taucoin.getBlockchain()).getPendingState().getPendingTransactions().size();
            } else {
                count = taucoin.getBlockchain().getBlockByNumber(blockNumber).getTransactionsList().size();
            }

            String tmp = "0x" + Integer.toHexString(count);
            JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
            return res;
        }

    }
}