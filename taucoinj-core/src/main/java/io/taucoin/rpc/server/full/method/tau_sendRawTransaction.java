package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import io.taucoin.core.Transaction;
import io.taucoin.facade.Taucoin;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class tau_sendRawTransaction extends JsonRpcServerMethod {
    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public tau_sendRawTransaction (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        List<Object> params = req.getPositionalParams();
        if (params.size() < 1 ) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        }

        logger.info("tx is: "+ Hex.toHexString(jsToByteArray((String)params.get(0))));

        ArrayList<String> result = new ArrayList<String>();
        Transaction tx = new Transaction(jsToByteArray((String)params.get(0)));

        try {
            // parse transaction
            tx.rlpParse();
            // verify transaction
            if (!tx.validate()) {
                throw new Exception("Invalid params");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        }

        try {
            taucoin.submitTransaction(tx);//.get(CONFIG.transactionApproveTimeout(), TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, req.getID());
        }

        result.add("txid: "+Hex.toHexString(tx.getHash()));

        JSONRPC2Response res = new JSONRPC2Response(result, req.getID());
        return res;

    }

}
