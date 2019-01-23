package io.taucoin.rpc.server.light.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import net.minidev.json.JSONObject;
import io.taucoin.rpc.server.light.JsonRpcServerMethod;
import io.taucoin.core.Account;
import io.taucoin.core.Transaction;
import io.taucoin.facade.Taucoin;
import org.spongycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static io.taucoin.core.Denomination.SZABO;
import static io.taucoin.config.SystemProperties.CONFIG;


public class tau_sendTransaction extends JsonRpcServerMethod {

    public tau_sendTransaction (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            JSONObject obj = (JSONObject)params.get(0);
            Transaction tx;
            try {
                tx = jsToTransaction(obj);
            } catch (Exception e) {
                return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
            }

            ArrayList<Object> rparams = new ArrayList<Object>();
            rparams.add("0x" + Hex.toHexString(tx.getEncoded()));
            JSONRPC2Request rreq = new JSONRPC2Request("tau_sendRawTransaction", rparams, req.getID());
            JSONRPC2Response rres = getRemoteData(rreq);
            if (rres == null) {
                return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, req.getID());
            }
            rres.setID(req.getID());

            return rres;
        }

    }
}