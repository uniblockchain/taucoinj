package org.ethereum.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import org.ethereum.rpc.server.full.JsonRpcServerMethod;
import org.ethereum.core.Blockchain;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.facade.Ethereum;
import java.util.List;

public class eth_getTransactionByHash extends JsonRpcServerMethod {

    public eth_getTransactionByHash (Ethereum ethereum) {
        super(ethereum);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            byte[] address = jsToAddress((String) params.get(0));

            Blockchain blockchain = (Blockchain)ethereum.getBlockchain();
            TransactionReceipt transaction = blockchain.getTransactionReceiptByHash(address);

            if (transaction == null)
                return new JSONRPC2Response(null, req.getID());

            JSONRPC2Response res = new JSONRPC2Response(transactionToJS(null, transaction.getTransaction()), req.getID());
            return res;
        }

    }
}