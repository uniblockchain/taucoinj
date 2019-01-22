package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServer;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.core.AccountState;
import io.taucoin.core.Repository;
import io.taucoin.core.Transaction;
import io.taucoin.facade.Taucoin;
import org.spongycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class eth_getTransactionCount extends JsonRpcServerMethod {

    public eth_getTransactionCount(Taucoin taucoin) { super(taucoin); }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 2) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            Repository repository;
            byte[] address = jsToAddress((String) params.get(0));
            String height = (String)params.get(1);

            long blockNumber = getBlockNumber(height);

            /*
            byte[] root = taucoin.getBlockchain().getBestBlock().getStateRoot();

            if (blockNumber >= 0) {
                repository = (Repository)taucoin.getRepository();
                repository.syncToRoot(taucoin.getBlockchain().getBlockByNumber(blockNumber).getStateRoot());
            }
             */

            BigInteger nonce = BigInteger.ZERO;
            repository = (Repository)taucoin.getRepository();
            AccountState accountState = repository.getAccountState(address);
            /*
            if (accountState != null)
                nonce = accountState.getNonce();
             */

            if (blockNumber == -1) {
                synchronized (taucoin.getPendingStateTransactions()) {
                    for (Transaction tx : taucoin.getPendingStateTransactions()) {
                        if (Arrays.equals(address, tx.getSender())) {
                            nonce.add(BigInteger.ONE);
                        }
                    }
                }
            }

            if (blockNumber >= 0) {
                repository = (Repository)taucoin.getRepository();
                //repository.syncToRoot(root);
            }

            String tmp = "0x" + nonce.toString(16);
            JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
            return res;
        }

    }
}
