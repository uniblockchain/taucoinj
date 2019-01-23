package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import net.minidev.json.JSONValue;

import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;
import io.taucoin.core.Transaction;
import io.taucoin.core.PendingState;
import org.spongycastle.util.encoders.Hex;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class tau_getTransactions extends JsonRpcServerMethod {

    protected PendingState pendingState;

    public tau_getTransactions(Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        String rres = "Transacions: [";
		/*
        for(Transaction tx: pendingState.getWireTransactions()){
            rres += Hex.toHexString(tx.getHash())+ ", \n";
        }
		*/
        rres+= "]";

        JSONRPC2Response res = new JSONRPC2Response(JSONValue.parse(rres), req.getID());
        return res;
    }
}
